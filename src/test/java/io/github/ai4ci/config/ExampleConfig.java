package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.behaviour.Test;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.util.SimpleDistribution;

public class ExampleConfig {

	public static Map<String,ExperimentConfiguration> configurations = new HashMap<>();

	static {
		// The default
		configurations.put("default",ExperimentConfiguration.DEFAULT);

		configurations.put("behaviour-comparison",  
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
						.setName("reactive-test")
						.setDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(NonCompliant.class.getSimpleName())
						.build()
						)
				);

		configurations.put("test-R0",  
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
						// .withInHostConfiguration(StochasticModel.DEFAULT)
						)
				.withExecutionReplications(1)
				);

	}

	public static void main(String[] args) throws IOException {

		String resourceName = ".examples";

		ClassLoader classLoader = ExampleConfig.class.getClassLoader();
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

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

//		{
//			final SubclassesResolver resolver = new SubclassesResolverImpl()
//					.withPackagesToScan(Arrays.asList(
//							"io.github.ai4ci"
//							));
//			JsonSchemaConfig config = JsonSchemaConfig.vanillaJsonSchemaDraft4().withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);
//			config = config.withSubclassesResolver( resolver );
//
//			// If using JsonSchema to generate HTML5 GUI:
//			JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );
//
//			// If you want to configure it manually:
//			// JsonSchemaConfig config = JsonSchemaConfig.create(...);
//			// JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);
//
//			JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper, config);
//
//			JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(ImmutableExperimentConfiguration.class);
//			objectMapper.writeValue(directory.resolve("schema2.json").toFile(), jsonSchema);
//
//			JsonNode jsonHtml = html5.generateJsonSchema(ImmutableExperimentConfiguration.class);
//			objectMapper.writeValue(directory.resolve("schema2.html").toFile(), jsonHtml);
//		}

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
