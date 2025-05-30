package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
//import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
//import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
//import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
//import com.kjetland.jackson.jsonSchema.SubclassesResolver;
//import com.kjetland.jackson.jsonSchema.SubclassesResolverImpl;

import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.behaviour.SmartAgentLFTTesting;
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.behaviour.Test;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.config.setup.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.util.SimpleDistribution;

public enum ExampleConfig {

	DEFAULT ("default",ExperimentConfiguration.DEFAULT),
	
	BEHAVIOUR ("behaviour-comparison",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("behaviour-comparison")
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
						.setName("smart-agent-lft")
						.setDefaultBehaviourModelName(SmartAgentLFTTesting.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("reactive-test")
						.setDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(NonCompliant.class.getSimpleName())
						.build()
						)
				),

		R0 ("test-R0",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("test-R0")
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
//						.withContactProbability( SimpleDistribution.unimodalBeta(0.1, 0.1) )
//						// .withInHostConfiguration(StochasticModel.DEFAULT)
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
				),

		AGE_STRAT ("age-stratification",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("age-stratification")
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
						.withDemographicAdjustment(DemographicAdjustment.AGE_DEFAULT)
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
					)
				.withExecutionReplications(1)
				),

		IN_HOST ("in-host-test", 
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
							.withExporters(Exporters.values())
				)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(Test.class.getSimpleName())
						.withImportationProbability(0D)
						.withSymptomSensitivity(SimpleDistribution.point(1D))
						.withSymptomSpecificity(SimpleDistribution.point(1D))
				)
				.withFacet(
					"in-host-models",
					PartialExecutionConfiguration.builder()
						.setName("markov")
						.setInHostConfiguration(
							MarkovStateModel.DEFAULT
						)
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("phenomenological")
						.setInHostConfiguration(
							PhenomenologicalModel.DEFAULT
						)
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("stochastic")
						.setInHostConfiguration(
							StochasticModel.DEFAULT
						)
						.build()
				)
			);
			
	public String name;
	public ExperimentConfiguration config;
	ExampleConfig(String name, ExperimentConfiguration config) {this.name = name;this.config=config;}
	
	

	public static void main(String[] args) throws IOException {

		String resourceName = ".examples";

		ClassLoader classLoader = ExampleConfig.class.getClassLoader();
		Path directory = Path.of(classLoader.getResource(resourceName).getFile())
				.getParent().getParent().getParent()
				.resolve("src/test/resources");

		System.out.println(directory);

		Arrays.stream(ExampleConfig.values()).forEach((a) -> {
			try {
				Files.createDirectories(directory.resolve(a.name));
				Path tmp = directory.resolve(a.name).resolve("config.json");
				System.out.println(tmp);
				a.config.writeConfig(tmp);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		{
			JacksonModule module = new JacksonModule();
			SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
					SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
					.with(module);
			SchemaGeneratorConfig config = configBuilder.build();
			SchemaGenerator generator = new SchemaGenerator(config);
			JsonNode jsonSchema = generator.generateSchema(ImmutableExperimentConfiguration.class);
			objectMapper.writeValue(directory.resolve("schema.json").toFile(), jsonSchema);
		}

	}

}
