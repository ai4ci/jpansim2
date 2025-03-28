package io.github.ai4ci.abm;

import java.io.IOException;
import java.util.stream.IntStream;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import io.github.ai4ci.abm.BehaviourModel.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.BehaviourModel.SmartAgentTesting;
import io.github.ai4ci.abm.PolicyModel.NoControl;
import io.github.ai4ci.config.ConfigMerger;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.flow.ExperimentBuilder;
import io.github.ai4ci.flow.ExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableExperimentFacet;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakFinalStateCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonCSV;
import io.github.ai4ci.output.StateExporter;
import io.github.ai4ci.output.StateExporter.ExportSelector;

class TestDefaultSim {

		
	
	public static void main(String[] args) throws IOException {
		
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
		
		ExperimentConfiguration tmp = 
				ImmutableExperimentConfiguration.copyOf(	
						ExperimentConfiguration.DEFAULT
					)
				.adjustSetup(b -> b.setNetworkSize(10000))
				.adjustExecution(e -> e.setDefaultPolicyModelName(PolicyModel.NoControl.class.getSimpleName()))
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
					)
					;
		
//		Outbreak outbreak = ExperimentBuilder.buildExperiment(
//				ConfigMerger.INSTANCE.mergeConfiguration(
//						SetupConfiguration.DEFAULT,
//						PartialSetupConfiguration.builder().setNetworkSize(10000).build()
//				).build(),
//				ConfigMerger.INSTANCE.mergeConfiguration(
//						ExecutionConfiguration.DEFAULT,
//						PartialExecutionConfiguration.builder()
//							.setDefaultPolicyModelName(NoControl.class.getSimpleName())
//							.setDefaultBehaviourModelName(SmartAgentTesting.class.getSimpleName())
//							.build()
//				).build(),
//				"experiment");
		
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
				ExportSelector.ofMany(ImmutableOutbreakFinalStateCSV.class, o -> CSVMapper.INSTANCE.finalCSV(o.getCurrentState()), "rt-final.csv")
		);
		
		ExperimentBuilder.runExperiments(tmp, "test", updater, exporter, 100, finalState);
		
//		IntStream.range(0, 100).forEach(i -> {
//			updater.update(outbreak);
//			exporter.export(outbreak);
//			System.out.println(outbreak.getCurrentState());
//		});
//		
//		Person per = outbreak.getPeople()
//				.stream().filter(p -> p.getCurrentState().isInfectious())
//				.findFirst().get();
//		
//		System.out.println(per);
		
		System.out.println("finished");
	} 

}
