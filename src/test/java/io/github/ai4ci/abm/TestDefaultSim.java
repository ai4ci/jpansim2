package io.github.ai4ci.abm;

import java.io.IOException;
import java.util.stream.IntStream;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import io.github.ai4ci.abm.BehaviourModel.NonCompliant;
import io.github.ai4ci.abm.BehaviourModel.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.PolicyModel.NoControl;
import io.github.ai4ci.config.ConfigMerger;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.flow.ExperimentBuilder;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutablePersonCSV;
import io.github.ai4ci.output.StateExporter;
import io.github.ai4ci.output.StateExporter.ExportSelector;
import io.github.ai4ci.util.Distribution;

class TestDefaultSim {

		
	
	public static void main(String[] args) throws IOException {
		
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.DEBUG);
		
		Outbreak outbreak = ExperimentBuilder.buildExperiment(
				ConfigMerger.INSTANCE.mergeConfiguration(
						SetupConfiguration.DEFAULT,
						PartialSetupConfiguration.create().setNetworkSize(10000)
				),
				ConfigMerger.INSTANCE.mergeConfiguration(
						ExecutionConfiguration.DEFAULT,
						PartialExecutionConfiguration.create()
							.setImmuneTargetRatio(Distribution.point(1.0))
							.setAppUseProbability(Distribution.point(0.8))
							.setDefaultPolicyModelName(NoControl.class.getSimpleName())
							.setDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
				),
				"experiment");
		
		Updater updater = new Updater();
		
		StateExporter exporter = StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve("tmp"),
				ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState()), "summary.csv"),
				ExportSelector.ofMany(ImmutablePersonCSV.class, o -> o.getPeople().stream().map(CSVMapper.INSTANCE::toCSV), "linelist.csv")
		);
			
		IntStream.range(0, 100).forEach(i -> {
			updater.update(outbreak);
			exporter.export(outbreak);
			System.out.println(outbreak.getCurrentState());
		});
		
		Person per = outbreak.getPeople()
				.stream().filter(p -> p.getCurrentState().isInfectious())
				.findFirst().get();
		
		System.out.println(per);
		
		System.out.println("finished");
	} 

}
