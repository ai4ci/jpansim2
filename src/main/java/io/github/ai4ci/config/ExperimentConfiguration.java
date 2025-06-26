package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.io.Files;

import io.github.ai4ci.abm.mechanics.Abstraction.Modification;
import io.github.ai4ci.config.ExperimentFacet.ExecutionFacet;
import io.github.ai4ci.config.ExperimentFacet.SetupFacet;
import io.github.ai4ci.config.setup.ImmutableSetupConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.flow.StateExporter;
import io.github.ai4ci.util.ReflectionUtils;


@Value.Immutable @Value.Modifiable
@JsonSerialize(as = ImmutableExperimentConfiguration.class)
@JsonDeserialize(as = ImmutableExperimentConfiguration.class)
public interface ExperimentConfiguration {


	ImmutableExperimentConfiguration DEFAULT = ImmutableExperimentConfiguration.builder()
			.setBatchConfig(BatchConfiguration.DEFAULT)
			.setSetupConfig(
				List.of(
					SetupFacet.of(SetupConfiguration.DEFAULT)
				)
			)
			.setExecutionConfig(
					ExecutionConfiguration.DEFAULT
			)
			.setExecutionReplications(1)
			.setSetupReplications(1)
			.build();


	ImmutableBatchConfiguration getBatchConfig();
	List<SetupFacet> getSetupConfig();
	int getSetupReplications();
	ImmutableExecutionConfiguration getExecutionConfig();
	List<ImmutableExecutionFacet> getFacets();
	int getExecutionReplications();

	/**
	 * Generate a subset of the experiment setups that are relevant to this 
	 * SLURM node if there is more than one node. If this is  
	 * @return
	 */
	@JsonIgnore
	default List<SetupConfiguration> getBatchSetupList() {

		if (getBatchConfig().getBatchTotal() <= 1) return this.getSetup();

		int size = this.getSetup().size();
		if (size == 1) return this.getSetup();

		// Handle SLURM parallelisation. Split list into N chunks based on number 
		// of batches
		int chunkSize = (int) Math.ceil( ((double) size) / getBatchConfig().getBatchTotal());

		// This is going to be made up of a list of setups and replicates.
		// each replicate is stand alone so we can just filter this list to 
		// get to the list for this node.
		int start = (getBatchConfig().getBatchNumber()-1) * chunkSize;
		int end = Math.min(getBatchConfig().getBatchNumber() * chunkSize, size);
		if (start >= size) return Collections.emptyList();
		return this.getSetup().subList(start, end );

	}

	@JsonIgnore
	default List<SetupConfiguration> getSetup() {

		List<SetupConfiguration> tmp = new ArrayList<>();
		List<SetupFacet> setupCfg = getSetupConfig();

		for (SetupFacet facet: setupCfg) {

			SetupConfiguration base = facet.getDefault();
			
			if (facet.getModifications().isEmpty()) tmp.add(base);
			else {
				for (Modification<? extends SetupConfiguration> mod: facet.getModifications()) {
	
					if (mod.self().getName() == null) throw new RuntimeException("Modifications must have a value for name");
	
					SetupConfiguration modified = (SetupConfiguration) 
	//						ConfigMerger.INSTANCE
	//						.mergeConfiguration(
							((ImmutableSetupConfiguration) ReflectionUtils.merge(
									base, mod
							))
							.withName(
									facet.getDefault().getName()+":"+mod.self().getName()
									);
	
					tmp.add(modified);
				}
			}
			
		}

		List<SetupConfiguration> tmp2 = new ArrayList<>();
		for (int i =0; i<this.getSetupReplications(); i++) {
			for (SetupConfiguration b: tmp) {
				tmp2.add(((ImmutableSetupConfiguration) b).withReplicate(i));
			}
		}

		return tmp2;

	}

	@JsonIgnore
	default List<ExecutionConfiguration> getExecution() {

		ImmutableExecutionConfiguration base = getExecutionConfig();
		List<ImmutableExecutionConfiguration> out = new ArrayList<>();
		out.add(base);
		for (ExecutionFacet facet: getFacets()) {
			
			String facetName = facet.getName();
			List<ImmutableExecutionConfiguration> tmp = new ArrayList<>();

			for (Modification<ExecutionConfiguration> mod: facet.getModifications()) {

				if (mod instanceof PartialExecutionConfiguration) {

					if (mod.self().getName() == null) throw new RuntimeException("Modifications must have a value for name");

					out.forEach(b -> {
						ImmutableExecutionConfiguration modified = 
								(ImmutableExecutionConfiguration) ReflectionUtils.merge(b, mod)
						.withName(
								(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
								+facetName+":"+mod.self().getName());
						tmp.add(modified);
					});
				}
			}
			out = tmp;
			
		}
		

		List<ExecutionConfiguration> tmp = new ArrayList<>();
		for (int i =0; i<this.getExecutionReplications(); i++) {
			for (ExecutionConfiguration b: out) {
				tmp.add((ExecutionConfiguration) b.withReplicate(i));
			}
		}

		return tmp;

	}

	default void writeConfig(Path directoryOrFile) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		if (!Files.getFileExtension(directoryOrFile.toString()).equals("json")) {
			directoryOrFile = directoryOrFile.resolve("config.json");
		}
		Files.createParentDirs(directoryOrFile.toFile());
		om.writeValue(directoryOrFile.toFile(), this);
	}

	static ExperimentConfiguration readConfig(Path file) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.enable(JsonParser.Feature.ALLOW_COMMENTS);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		ExperimentConfiguration rt = om.readerFor(ExperimentConfiguration.class).readValue(file.toFile());
		return rt;
	}

	default ImmutableExperimentConfiguration withSetupConfig(SetupConfiguration config) {
		return ImmutableExperimentConfiguration.builder().from(this)
				.setSetupConfig(
						List.of(
							ImmutableSetupFacet.builder().setDefault(config).build()))
				.build();
	}

	default ImmutableExperimentConfiguration withExecutionConfig(ImmutableExecutionConfiguration config) {
		return ImmutableExperimentConfiguration.builder().from(this)
				.setExecutionConfig(config)
				.build();
	}

	default ImmutableExperimentConfiguration withFacet(String name, PartialExecutionConfiguration... config) {
		return ImmutableExperimentConfiguration.builder().from(this)
				.addFacets(
						ImmutableExecutionFacet.builder()
						.setName(name)
						.setModifications(Arrays.asList(config))
						.build()
						)
				.build();
	}

	@JsonIgnore
	default StateExporter exporter(Path baseDirectory) {
		return StateExporter.of(
				getBatchDirectoryPath(baseDirectory), 
				Arrays.stream(getBatchConfig().getExporters()).map(e ->e.getSelector()).collect(Collectors.toList())
				);
	}

	@JsonIgnore
	default Path getBatchDirectoryPath(Path baseDirectory) {
		if (this.getBatchConfig().getBatchTotal()<=1) return baseDirectory; 
		return baseDirectory.resolve(""+this.getBatchConfig().getBatchNumber());
	}
}
