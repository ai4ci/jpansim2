package io.github.ai4ci.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

import io.github.ai4ci.config.ImmutableExperimentConfiguration;

public class WriteExampleConfig {

	public static void main(String[] args) throws IOException {

		String resourceName = ".examples";

		ClassLoader classLoader = Experiment.class.getClassLoader();
		Path directory = Path.of(classLoader.getResource(resourceName).getFile())
				.getParent().getParent().getParent()
				.resolve("src/test/resources");
		
		Path directory2 = SystemUtils.getUserHome().toPath().resolve("Data/ai4ci");

		System.out.println(directory);

		Stream.of(directory, directory2).forEach( d -> {
			Arrays.stream(Experiment.values()).forEach((a) -> {
				try {
					Files.createDirectories(d.resolve(a.name));
					Path tmp = d.resolve(a.name).resolve("config.json");
					System.out.println(tmp);
					if (d.equals(directory)) {
						a.config.writeConfig(tmp);
					} else {
						a.config
						.withSetupReplications(5)
						.withExecutionReplications(5)
						.writeConfig(tmp);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		});

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		
//		SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
//		try {
//		   objectMapper.acceptJsonFormatVisitor(
//				   objectMapper.constructType(ImmutableExperimentConfiguration.class), visitor);
//		   JsonSchema jsonSchema = visitor.finalSchema();
//		   objectMapper.writeValue(directory.resolve("schema.json").toFile(), jsonSchema);
//		   // System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema));
//		 } catch (JsonProcessingException jsonEx) {
//		   throw new RuntimeException();
//		 }

		{
			JacksonModule module = new JacksonModule();
			SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
					//SchemaVersion.DRAFT_7, 
					SchemaVersion.DRAFT_2020_12,
					OptionPreset.PLAIN_JSON
			).with(
				Option.SCHEMA_VERSION_INDICATOR,
				Option.DEFINITIONS_FOR_ALL_OBJECTS,
				Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES,
				Option.DEFINITION_FOR_MAIN_SCHEMA
				
			)
			.with(module);
			SchemaGeneratorConfig config = configBuilder.build();
			SchemaGenerator generator = new SchemaGenerator(config);
			JsonNode jsonSchema = generator.generateSchema(ImmutableExperimentConfiguration.class);
			objectMapper.writeValue(directory.resolve("schema.json").toFile(), jsonSchema);
		}
		
		

	}
	
}
