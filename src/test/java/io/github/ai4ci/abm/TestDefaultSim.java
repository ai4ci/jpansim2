package io.github.ai4ci.abm;

import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.flow.ExperimentBuilder;
import io.github.ai4ci.flow.ExperimentConfiguration;
import io.github.ai4ci.flow.ExperimentConfiguration.BasicExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableBasicExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableExperimentFacet;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutableInfectivityProfileCSV;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonCSV;
import io.github.ai4ci.output.StateExporter;
import io.github.ai4ci.output.StateExporter.ExportSelector;
import io.github.ai4ci.util.SimpleDistribution;

class TestDefaultSim {

	static BasicExperimentConfiguration config1 =  
			ImmutableBasicExperimentConfiguration.copyOf(	
					BasicExperimentConfiguration.DEFAULT
				)
			.adjustSetup(b -> b
					.setNetworkSize(10000)
					.setInitialImports(30)
			)
			.adjustExecution(e -> e
					.setDefaultPolicyModelName(PolicyModel.NoControl.class.getSimpleName())
					.setImportationProbability(0D)
					// .setInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withFacets(
						
						ImmutableExperimentFacet.builder()
							.putModification(
								"smart-agent",
								PartialExecutionConfiguration.builder()
									.setDefaultBehaviourModelName(BehaviourModel.SmartAgentTesting.class.getSimpleName())
									.build()
								)
							.putModification(
									"reactive-test",
									PartialExecutionConfiguration.builder()
										.setDefaultBehaviourModelName(BehaviourModel.ReactiveTestAndIsolate.class.getSimpleName())
										.build()
									)
							.putModification(
									"nothing",
									PartialExecutionConfiguration.builder()
										.setDefaultBehaviourModelName(BehaviourModel.NonCompliant.class.getSimpleName())
										.build()
									)
						.setName("strategy")
						.build()
				);
	
	static BasicExperimentConfiguration testR0 =  
			ImmutableBasicExperimentConfiguration.copyOf(	
					BasicExperimentConfiguration.DEFAULT
				)
			.adjustSetup(b -> b
					.setNetworkSize(10000)
					.setNetworkConnectedness(100)
					.setInitialImports(30)
					.setNetworkRandomness(1D)
			)
			.withSetupReplications(2)
			.adjustExecution(e -> e
					.setDefaultPolicyModelName(PolicyModel.NoControl.class.getSimpleName())
					.setDefaultBehaviourModelName(BehaviourModel.Test.class.getSimpleName())
					.setImportationProbability(0D) //.001D)
					.setContactProbability( SimpleDistribution.unimodalBeta(0.1, 0.1) )
					// .setInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withExecutionReplications(5)
			.withFacets(
						
						ImmutableExperimentFacet.builder()
							.putModification(
								"r0_1",
								PartialExecutionConfiguration.builder()
									.setRO(1D)
									.build()
								)
							.putModification(
									"r0_2",
									PartialExecutionConfiguration.builder()
										.setRO(2D)
										.build()
									)
							.putModification(
									"r0_3",
									PartialExecutionConfiguration.builder()
										.setRO(3D)
										.build()
									)
						.setName("repro")
						.build()
				);
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
		
		ExperimentConfiguration<?> tmp = testR0;
		
		tmp.writeToYaml(SystemUtils.getUserHome().toPath().resolve("tmp"));
		
		Updater updater = new Updater();
		
		StateExporter exporter = StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve("tmp"),
				ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState()), "summary.csv"),
				ExportSelector.ofMany(ImmutablePersonCSV.class, o -> o.getPeople().stream().map(CSVMapper.INSTANCE::toCSV), "linelist.csv"),
				ExportSelector.ofMany(ImmutableOutbreakHistoryCSV.class, o -> o.getHistory().stream().map(CSVMapper.INSTANCE::toCSV), "test-positivity.csv")
		);
		
		StateExporter finalState = StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve("tmp"),
				// ExportSelector.ofMany(ImmutableOutbreakFinalStateCSV.class, o -> CSVMapper.INSTANCE.finalCSV(o.getCurrentState()), "rt-final.csv"),
				ExportSelector.ofMany(ImmutableInfectivityProfileCSV.class, o -> CSVMapper.INSTANCE.infectivityProfile(o.getExecutionConfiguration()), "ip.csv")
		);
		
		ExperimentBuilder.runExperiments(tmp, "test", updater, exporter, 100, finalState);
		
		exporter.close();
		finalState.close();
		
		System.out.println("finished");
	} 

}
