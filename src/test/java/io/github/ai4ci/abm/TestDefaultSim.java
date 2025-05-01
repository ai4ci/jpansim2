package io.github.ai4ci.abm;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.ImmutableExperimentConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;
import io.github.ai4ci.flow.SimulationFactory;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutableContactCSV;
import io.github.ai4ci.output.ImmutableInfectivityProfileCSV;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonDemographicsCSV;
import io.github.ai4ci.output.ImmutablePersonStateCSV;
import io.github.ai4ci.output.StateExporter;
import io.github.ai4ci.output.StateExporter.ExportSelector;
import io.github.ai4ci.util.SimpleDistribution;

class TestDefaultSim {

	static ExperimentConfiguration config1 =  
			ImmutableExperimentConfiguration.copyOf(	
				ExperimentConfiguration.DEFAULT
			)
			.withExecutionConfig(
				ExecutionConfiguration.DEFAULT
					.withDefaultPolicyModelName(PolicyModel.NoControl.class.getSimpleName())
					.withImportationProbability(0D)
					// .setInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withFacet(
				"smart-agent",
				PartialExecutionConfiguration.builder()
					.setDefaultBehaviourModelName(BehaviourModel.SmartAgentTesting.class.getSimpleName())
					.build()
				)
			.withFacet(
				"reactive-test",
				PartialExecutionConfiguration.builder()
					.setDefaultBehaviourModelName(BehaviourModel.ReactiveTestAndIsolate.class.getSimpleName())
					.build()
				)
			.withFacet(
				"nothing",
				PartialExecutionConfiguration.builder()
					.setDefaultBehaviourModelName(BehaviourModel.NonCompliant.class.getSimpleName())
					.build()
				)
			.withFacet(
				"non-responder",
					PartialExecutionConfiguration.builder()
						.setDefaultBehaviourModelName(BehaviourModel.Test.class.getSimpleName())
						.build()
					)
			;
	
	static ExperimentConfiguration testR0 =  
			ImmutableExperimentConfiguration.copyOf(	
					ExperimentConfiguration.DEFAULT
			)
			.withSetupConfig(
				WattsStrogatzConfiguration.DEFAULT
			)
			.withSetupReplications(1)
			.withExecutionConfig(
				ExecutionConfiguration.DEFAULT
					.withDefaultPolicyModelName(PolicyModel.NoControl.class.getSimpleName())
					.withDefaultBehaviourModelName(BehaviourModel.Test.class.getSimpleName())
					.withImportationProbability(0D) //.001D)
					.withContactProbability( SimpleDistribution.unimodalBeta(0.1, 0.1) )
					// .withInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withExecutionReplications(1)
			.withFacet(
				"r0_1", 
				PartialExecutionConfiguration.builder()
					.setRO(1D)
					.build()
			)
			.withFacet(
				"r0_2", 
				PartialExecutionConfiguration.builder()
					.setRO(2D)
					.build()
			)
			.withFacet(
				"r0_3", 
				PartialExecutionConfiguration.builder()
					.setRO(3D)
					.build()
			);
	
	
	static ExperimentConfiguration ageStrat =  
			ImmutableExperimentConfiguration.copyOf(	
					ExperimentConfiguration.DEFAULT
			)
			.withSetupConfig(
					AgeStratifiedNetworkConfiguration.DEFAULT
			)
			.withSetupReplications(1)
			.withExecutionConfig(
					ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(PolicyModel.NoControl.class.getSimpleName())
						.withImportationProbability(0D)
						// .withInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withExecutionReplications(1)
			.withFacet(
				"ignore", 
				PartialExecutionConfiguration.builder()
						.setDefaultBehaviourModelName(BehaviourModel.Test.class.getSimpleName())
						.build()
			)
			.withFacet(
				"smart-agent",
				PartialExecutionConfiguration.builder()
					.setDefaultBehaviourModelName(BehaviourModel.SmartAgentTesting.class.getSimpleName())
					.build()
			)
			.withFacet(
				"reactive-test",
				PartialExecutionConfiguration.builder()
					.setDefaultBehaviourModelName(BehaviourModel.ReactiveTestAndIsolate.class.getSimpleName())
					.build()
			)
			.withFacet(
				"symptom-management",
				PartialExecutionConfiguration.builder()
					.setDefaultBehaviourModelName(BehaviourModel.NonCompliant.class.getSimpleName())
					.build()
				);
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
		
		ExperimentConfiguration tmp = config1;
		
		tmp.writeConfig(SystemUtils.getUserHome().toPath().resolve("tmp"));
		
		Updater updater = new Updater();
		
		StateExporter exporter = StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve("tmp"),
				ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState()), "summary.csv"),
				ExportSelector.ofVeryMany(ImmutablePersonStateCSV.class, o -> o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV), "linelist.csv"),
				ExportSelector.ofMany(ImmutableOutbreakHistoryCSV.class, o -> o.getHistory().stream().map(CSVMapper.INSTANCE::toCSV), "test-positivity.csv"),
				ExportSelector.ofVeryMany(ImmutableContactCSV.class, o -> o.getPeople().stream().flatMap(CSVMapper.INSTANCE::toContacts), "contact.csv")
		);
		
		StateExporter finalState = StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve("tmp"),
				// ExportSelector.ofMany(ImmutableOutbreakFinalStateCSV.class, o -> CSVMapper.INSTANCE.finalCSV(o.getCurrentState()), "rt-final.csv"),
				ExportSelector.ofMany(ImmutableInfectivityProfileCSV.class, o -> CSVMapper.INSTANCE.infectivityProfile(o.getExecutionConfiguration()), "ip.csv"),
				ExportSelector.ofMany(ImmutablePersonDemographicsCSV.class, o -> o.getPeople().stream().map(CSVMapper.INSTANCE::toDemog), "demog.csv")
		);
		
		SimulationFactory.runExperiments(tmp, "test", updater, exporter, 100, finalState);
		
		exporter.close();
		finalState.close();
		
		System.out.println("finished");
	} 

}
