package io.github.ai4ci.abm;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;

import io.github.ai4ci.SlurmAwareLogger;
import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.behaviour.Test;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.BatchConfiguration;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.Exporters;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;
import io.github.ai4ci.flow.SimulationMonitor;
import io.github.ai4ci.util.SimpleDistribution;

class TestDefaultSim {

	static ExperimentConfiguration config1 =  
			ExperimentConfiguration.DEFAULT
			.withBatchConfig(
					BatchConfiguration.DEFAULT
					.withSimulationDuration(200)
					.withUrnBase("compare-behaviour")
			)
			.withExecutionConfig(
				ExecutionConfiguration.DEFAULT
					.withDefaultPolicyModelName(NoControl.class.getSimpleName())
					.withImportationProbability(0D)
					// .setInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withFacet(
					"behaviour", 
					PartialExecutionConfiguration.builder()
						.setName("ignore")
						.setDefaultBehaviourModelName(Test.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("smart-agent")
						.setDefaultBehaviourModelName(SmartAgentTesting.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("reactive-test")
						.setDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(NonCompliant.class.getSimpleName())
						.build()
					);
	
	static ExperimentConfiguration testR0 =  
			ExperimentConfiguration.DEFAULT
			.withBatchConfig(
					BatchConfiguration.DEFAULT
					.withSimulationDuration(200)
					.withUrnBase("r0-test")
			)
			.withSetupConfig(
				WattsStrogatzConfiguration.DEFAULT
			)
			.withSetupReplications(1)
			.withExecutionConfig(
				ExecutionConfiguration.DEFAULT
					.withDefaultPolicyModelName(NoControl.class.getSimpleName())
					.withDefaultBehaviourModelName(Test.class.getSimpleName())
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
			ExperimentConfiguration.DEFAULT
			.withBatchConfig(
					BatchConfiguration.DEFAULT
					.withSimulationDuration(200)
					.withUrnBase("age-strat")
					.withExporters(Exporters.values())
			)
			.withSetupConfig(
					AgeStratifiedNetworkConfiguration.DEFAULT
			)
			.withSetupReplications(1)
			.withExecutionConfig(
					ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(Test.class.getSimpleName())
						.withImportationProbability(0D)
						// .withInHostConfiguration(StochasticModel.DEFAULT)
			)
			.withExecutionReplications(1);
	
	public static void main(String[] args) throws IOException, InterruptedException {
		
		ExperimentConfiguration tmp = ageStrat; 
		//ExperimentConfiguration tmp = testR0;
		Path dir = SystemUtils.getUserHome().toPath().resolve("tmp");
		
		SlurmAwareLogger.setupLogger(tmp, dir, Level.INFO, Level.DEBUG);
		SimulationMonitor mon = new SimulationMonitor(tmp, dir);
		mon.run();
		
		System.out.println("finished");
	} 

}
