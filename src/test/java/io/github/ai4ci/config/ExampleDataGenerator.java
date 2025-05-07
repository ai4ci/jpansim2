package io.github.ai4ci.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import io.github.ai4ci.abm.BehaviourModel;
import io.github.ai4ci.abm.PolicyModel;
import io.github.ai4ci.util.SimpleDistribution;

public class ExampleDataGenerator {

	public static Map<String,ExperimentConfiguration> configurations = new HashMap<>();
	
	static {
		// The default
		configurations.put("default",ExperimentConfiguration.DEFAULT);
		
		
		configurations.put("behaviour-comparison",  
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
						)
		);
		
		configurations.put("test-R0",  
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
				)
		);
		
		configurations.put("age-stratification",  
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
				.withExecutionReplications(1)
		);
		
	}
	
	public static void main(String[] args) {
		
		String resourceName = ".examples";

		ClassLoader classLoader = ExampleDataGenerator.class.getClassLoader();
		Path directory = Path.of(classLoader.getResource(resourceName).getFile())
				.getParent().getParent().getParent()
				.resolve("src/test/resources");
		
		System.out.println(directory);

		configurations.forEach((s,c) -> {
			try {
				System.out.println(directory.resolve(s+".json"));
				c.writeConfig(directory.resolve(s+".json"));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
	}

}
