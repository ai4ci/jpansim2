package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonModule;

public class WriteExampleConfig {

	public static void main(String[] args) throws IOException {

		String resourceName = ".examples";

		ClassLoader classLoader = ExampleConfig.class.getClassLoader();
		Path directory = Path.of(classLoader.getResource(resourceName).getFile())
				.getParent().getParent().getParent()
				.resolve("src/test/resources");
		
		Path directory2 = SystemUtils.getUserHome().toPath().resolve("Data/ai4ci");

		System.out.println(directory);

		Stream.of(directory, directory2).forEach( d -> {
			Arrays.stream(ExampleConfig.values()).forEach((a) -> {
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
