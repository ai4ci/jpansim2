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
import io.github.ai4ci.flow.BatchRunner;
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
					"behaviour", 
					PartialExecutionConfiguration.builder()
						.setName("ignore")
						.setDefaultBehaviourModelName(BehaviourModel.Test.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("smart-agent")
						.setDefaultBehaviourModelName(BehaviourModel.SmartAgentTesting.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("reactive-test")
						.setDefaultBehaviourModelName(BehaviourModel.ReactiveTestAndIsolate.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(BehaviourModel.NonCompliant.class.getSimpleName())
						.build()
					);
	
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
				"R",
				PartialExecutionConfiguration.builder()
					.setName("1.0")
					.setRO(1D)
					.build(),
				PartialExecutionConfiguration.builder()
					.setName("2.0")
					.setRO(2D)
					.build(),
				PartialExecutionConfiguration.builder()
					.setName("3.0")
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
						.withDefaultBehaviourModelName(BehaviourModel.Test.class.getSimpleName())
						.withImportationProbability(0D)
						// .withInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withExecutionReplications(1);
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
		
		ExperimentConfiguration tmp = ageStrat; // testR0;
		
		Updater updater = new Updater();
		
		StateExporter exporter = StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve("tmp"),
				ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState())),
				ExportSelector.ofMany(ImmutablePersonStateCSV.class, o -> o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV)),
				ExportSelector.ofMany(ImmutableOutbreakHistoryCSV.class, o -> o.getHistory().stream().map(CSVMapper.INSTANCE::toCSV)),
				ExportSelector.ofMany(ImmutableContactCSV.class, o -> o.getPeople().stream().flatMap(CSVMapper.INSTANCE::toContacts)),
				// ExportSelector.ofMany(ImmutableOutbreakFinalStateCSV.class, o -> CSVMapper.INSTANCE.finalCSV(o.getCurrentState())),
				ExportSelector.ofMany(ImmutableInfectivityProfileCSV.class, o -> CSVMapper.INSTANCE.infectivityProfile(o)),
				ExportSelector.ofMany(ImmutablePersonDemographicsCSV.class, o -> o.getPeople().stream().map(CSVMapper.INSTANCE::toDemog))
		);
		
		exporter.writeInputConfiguration(tmp);
		
		BatchRunner.runExperiments(tmp, "test", updater, exporter, 100);
		
		exporter.close();
		
		
		System.out.println("finished");
	} 

}
