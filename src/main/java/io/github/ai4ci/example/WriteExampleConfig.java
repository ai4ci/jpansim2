package io.github.ai4ci.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

import io.github.ai4ci.config.ImmutableExperimentConfiguration;

/**
 * A small utility to write example configuration files for all experiments and
 * a JSON schema for the configuration file format. The output directory is
 * provided as the first argument to the program.
 *
 * This is executed as part of the maven build process to ensure that example
 * configuration files and the JSON schema are always up to date with the code.
 * The generated files are committed to the repository and can be used as
 * templates for new experiments and for validating configuration files.
 *
 */
public class WriteExampleConfig {

	/**
	 * Writes example configuration files for all experiments and a JSON schema
	 * for the configuration file format. The output directory is provided as the
	 * first argument to the program.
	 *
	 * @param args the first argument is the path to the output directory
	 * @throws RuntimeException if the output directory cannot be created or if
	 *                          no output directory is provided
	 *
	 */
	public static void main(String[] args) {

		if (args.length == 0) {
			throw new RuntimeException(
					"Please provide a path to the output directory as the first argument."
			);
		}

		var d = Path.of(args[0])
			.resolve("examples");

		System.out.println(
			"Writing example configuration files and JSON schema to "
					+ d.toAbsolutePath()
		);

		try {
			Files.createDirectories(d);
			if (!Files.exists(d.resolve(".nojekyll"))) {
				Files.createFile(d.resolve(".nojekyll"));
			}
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not create .nojekyll file in output directory.", e
			);
		}

		Arrays.stream(Experiment.values())
			.forEach((a) -> {
				try {
					Files.createDirectories(d.resolve(a.name));
					var tmp = d.resolve(a.name)
						.resolve("config.json");
					System.out.println(
						"Writing example configuration for " + a.name + " to "
								+ tmp.toAbsolutePath()
					);
					a.config
						// .withSetupReplications(5)
						// .withExecutionReplications(5)
						.writeConfig(tmp, true);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

		var objectMapper = new ObjectMapper();
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
			var module = new JacksonModule();
			var configBuilder = new SchemaGeneratorConfigBuilder(
					// SchemaVersion.DRAFT_7,
					SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON
			).with(
				Option.SCHEMA_VERSION_INDICATOR,
				Option.DEFINITIONS_FOR_ALL_OBJECTS,
				Option.DEFINITIONS_FOR_MEMBER_SUPERTYPES,
				Option.DEFINITION_FOR_MAIN_SCHEMA
			)
				.with(module);
			var config = configBuilder.build();
			var generator = new SchemaGenerator(config);
			JsonNode jsonSchema = generator
				.generateSchema(ImmutableExperimentConfiguration.class);
			try {
				objectMapper.writeValue(
					d.resolve("schema.json")
						.toFile(),
					jsonSchema
				);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

}
