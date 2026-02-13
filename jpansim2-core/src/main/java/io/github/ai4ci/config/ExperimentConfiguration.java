package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Files;
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

import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.execution.ImmutableExecutionConfiguration;
import io.github.ai4ci.config.setup.ImmutableSetupConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.flow.output.SimulationExporter;
import io.github.ai4ci.util.ReflectionUtils;

/**
 * Central configuration interface for JPanSim2 experiments.
 *
 * <p>
 * This immutable configuration interface defines all parameters needed to run
 * an experiment in JPanSim2, including:
 * <ul>
 * <li>Batch job configurations (via {@link BatchConfiguration})</li>
 * <li>Setup configurations (via {@link SetupConfiguration} and
 * {@link SetupFacet})</li>
 * <li>Execution configurations (via {@link ExecutionConfiguration} and
 * {@link ExecutionFacet})</li>
 * <li>Replication counts for both setup and execution phases</li>
 * </ul>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Supports SLURM parallelisation through batch configuration</li>
 * <li>Provides methods for generating configuration subsets per batch node</li>
 * <li>Includes JSON serialisation/deserialisation capabilities</li>
 * <li>Allows configuration composition through facets and modifications</li>
 * </ul>
 *
 * <p>
 * Each experiment consists of a set of simulation setups (defined by setup
 * facets) and a set of execution configurations (defined by execution facets).
 * The configuration supports generating all combinations of setups and
 * executions, as well as replicating each configuration multiple times for
 * statistical robustness. In the end the number of simulations run will be:
 * <ul>
 * <li>Number of setups = product of modifications across all setup facets</li>
 * <li>Number of executions = product of modifications across all execution
 * facets</li>
 * <li>Total simulations = setups * executions * setup replications * execution
 * replications</li>
 * </ul>
 * Different setup facets or setup replications will usually be shared between
 * different SLURM nodes, but the execution configurations and execution
 * replications will be typically be run in parallel within one node according
 * to the batch configuration. The optimal SLURM parallelisation will therefore
 * be to have a node per setup facet * setup replication and then run all
 * execution facets and execution replications in parallel within each node.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * ExperimentConfiguration config = ExperimentConfiguration.readConfig(path);
 * BatchConfiguration batchConfig = config.getBatchConfig();
 * List<SetupConfiguration> setups = config.getBatchSetupList();
 * }</pre>
 *
 * @author Rob Challen
 * @see BatchConfiguration
 * @see SetupConfiguration
 * @see ExecutionConfiguration
 */
@Value.Immutable
@Value.Modifiable
@JsonSerialize(as = ImmutableExperimentConfiguration.class)
@JsonDeserialize(as = ImmutableExperimentConfiguration.class)
public interface ExperimentConfiguration {

	/**
	 * Default immutable configuration instance used when no other configuration
	 * is provided. Includes:
	 * <ul>
	 * <li>Defaults from {@link BatchConfiguration#DEFAULT}</li>
	 * <li>Single setup facet with {@link SetupConfiguration#DEFAULT}</li>
	 * <li>{@link ExecutionConfiguration#DEFAULT}</li>
	 * <li>1 replication for both setup and execution</li>
	 * </ul>
	 */
	ImmutableExperimentConfiguration DEFAULT = ImmutableExperimentConfiguration
		.builder()
		.setBatchConfig(BatchConfiguration.DEFAULT)
		.setSetupConfig(List.of(SetupFacet.of(SetupConfiguration.DEFAULT)))
		.setExecutionConfig(ExecutionConfiguration.DEFAULT)
		.setExecutionReplications(1)
		.setSetupReplications(1)
		.build();

	/**
	 * Reads and deserializes an ExperimentConfiguration from a JSON file.
	 * Supports JSON with comments and handles Guava collection types.
	 *
	 * @param file Path to JSON configuration file
	 * @return Deserialized ExperimentConfiguration
	 * @throws StreamWriteException on JSON parsing errors
	 * @throws DatabindException    on data binding issues
	 * @throws IOException          on file system errors
	 * @see #writeConfig(Path, boolean)
	 */
	static ExperimentConfiguration readConfig(Path file)
			throws StreamWriteException, DatabindException, IOException {
		var om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.enable(JsonParser.Feature.ALLOW_COMMENTS);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		var rt = (ExperimentConfiguration) om
			.readerFor(ExperimentConfiguration.class)
			.readValue(file.toFile());
		return rt;
	}

	/**
	 * Creates a SimulationExporter configured for this experiment's batch
	 * settings. The exporter will use the batch-specific output directory if
	 * running in a multi-node SLURM batch job.
	 *
	 * @param baseDirectory Root directory for output files
	 * @return Configured SimulationExporter instance
	 * @see #getBatchDirectoryPath(Path)
	 */
	@JsonIgnore
	default SimulationExporter exporter(Path baseDirectory) {
		return SimulationExporter.of(
			this.getBatchDirectoryPath(baseDirectory),
			Arrays.stream(
				this.getBatchConfig()
					.getExporters()
			)
				.map(e -> e.getSelector())
				.collect(Collectors.toList())
		);
	}

	/**
	 * Gets the batch configuration parameters for SLURM job parallelisation.
	 *
	 * @return Immutable batch configuration instance
	 * @see BatchConfiguration
	 */
	ImmutableBatchConfiguration getBatchConfig();

	/**
	 * Gets the batch-specific output directory path. In multi-node SLURM jobs,
	 * this returns a subdirectory named with the batch number. Otherwise returns
	 * the base directory unchanged.
	 *
	 * @param baseDirectory Root directory for output files
	 * @return Batch-specific output directory path
	 */
	@JsonIgnore
	default Path getBatchDirectoryPath(Path baseDirectory) {
		if (this.getBatchConfig()
			.getBatchTotal() <= 1) { return baseDirectory; }
		return baseDirectory.resolve(
			"" + this.getBatchConfig()
				.getBatchNumber()
		);
	}

	/**
	 * Generates a subset of experiment setups relevant to the current SLURM
	 * node. When running in a multi-node SLURM batch job, this method splits the
	 * full setup list into chunks and returns only the portion assigned to this
	 * node.
	 *
	 * @return List of setup configurations for this batch node
	 * @see #getSetup()
	 */
	@JsonIgnore
	default List<SetupConfiguration> getBatchSetupList() {

		if (this.getBatchConfig()
			.getBatchTotal() <= 1) {
			return this.getSetup();
		}

		var size = this.getSetup()
			.size();
		if (size == 1) { return this.getSetup(); }

		// Handle SLURM parallelisation. Split list into N chunks based on number
		// of batches
		var chunkSize = (int) Math.ceil(
			((double) size) / this.getBatchConfig()
				.getBatchTotal()
		);

		// This is going to be made up of a list of setups and replicates.
		// each replicate is stand alone so we can just filter this list to
		// get to the list for this node.
		var start = (this.getBatchConfig()
			.getBatchNumber() - 1) * chunkSize;
		var end = Math.min(
			this.getBatchConfig()
				.getBatchNumber() * chunkSize,
			size
		);
		if (start >= size) { return Collections.emptyList(); }
		return this.getSetup()
			.subList(start, end);

	}

	/**
	 * Generates all execution configurations by applying all modifications from
	 * execution facets and expanding replications. This includes:
	 * <ul>
	 * <li>Applying all modifications from execution facets</li>
	 * <li>Generating replicated configurations when replications > 1</li>
	 * <li>Ensuring each configuration has a unique name</li>
	 * </ul>
	 *
	 * @return Complete list of execution configurations with modifications
	 *         applied
	 * @throws RuntimeException if any modification lacks a name
	 */
	@JsonIgnore
	default List<ExecutionConfiguration> getExecution() {

		var base = this.getExecutionConfig();
		List<ImmutableExecutionConfiguration> out = new ArrayList<>();
		out.add(base);
		for (ExecutionFacet facet : this.getFacets()) {

			var facetName = facet.getName();
			List<ImmutableExecutionConfiguration> tmp = new ArrayList<>();

			for (Modification<ExecutionConfiguration> mod : facet
				.getModifications()) {

				if (mod instanceof PartialExecutionConfiguration) {

					if (mod.self()
						.getName() == null) {
						throw new RuntimeException(
								"Modifications must have a value for name"
						);
					}

					out.forEach(b -> {
						var modified = (ImmutableExecutionConfiguration) ReflectionUtils
							.merge(b, mod)
							.withName(
								(!b.getName()
									.equals(base.getName()) ? b.getName() + ":" : "")
										+ facetName + ":" + mod.self()
											.getName()
							);
						tmp.add(modified);
					});
				}
			}
			out = tmp;

		}

		List<ExecutionConfiguration> tmp = new ArrayList<>();
		for (var i = 0; i < this.getExecutionReplications(); i++) {
			for (ExecutionConfiguration b : out) {
				tmp.add(b.withReplicate(i));
			}
		}

		return tmp;

	}

	/**
	 * Gets the base execution configuration before modifications.
	 *
	 * @return Immutable base execution configuration
	 * @see ExecutionConfiguration
	 */
	ImmutableExecutionConfiguration getExecutionConfig();

	/**
	 * Gets the number of replications to generate for each execution
	 * configuration.
	 *
	 * @return Number of execution replications (minimum 1)
	 */
	int getExecutionReplications();

	/**
	 * Gets the list of execution facets containing modifications to apply.
	 * Facets are processed in order with each modification building on previous
	 * results.
	 *
	 * @return List of execution configuration facets
	 * @see ExecutionFacet
	 */
	List<ExecutionFacet> getFacets();

	/**
	 * Generates all setup configurations by applying all modifications from
	 * setup facets and expanding replications. This includes:
	 * <ul>
	 * <li>Applying all modifications from setup facets</li>
	 * <li>Generating replicated configurations when replications > 1</li>
	 * <li>Ensuring each configuration has a unique name</li>
	 * </ul>
	 *
	 * @return Complete list of setup configurations with modifications applied
	 * @throws RuntimeException if any modification lacks a name
	 */
	@JsonIgnore
	default List<SetupConfiguration> getSetup() {

		List<SetupConfiguration> tmp = new ArrayList<>();
		var setupCfg = this.getSetupConfig();

		for (SetupFacet facet : setupCfg) {

			var base = facet.getDefault();

			if (facet.getModifications()
				.isEmpty()) {
				tmp.add(base);
			} else {
				for (Modification<? extends SetupConfiguration> mod : facet
					.getModifications()) {

					if (mod.self()
						.getName() == null) {
						throw new RuntimeException(
								"Modifications must have a value for name"
						);
					}

					SetupConfiguration modified = // ConfigMerger.INSTANCE
//					.mergeConfiguration(
							((ImmutableSetupConfiguration) ReflectionUtils
								.merge(base, mod)).withName(
									facet.getDefault()
										.getName() + ":"
											+ mod.self()
												.getName()
								);

					tmp.add(modified);
				}
			}

		}

		List<SetupConfiguration> tmp2 = new ArrayList<>();
		for (var i = 0; i < this.getSetupReplications(); i++) {
			for (SetupConfiguration b : tmp) {
				tmp2.add(((ImmutableSetupConfiguration) b).withReplicate(i));
			}
		}

		return tmp2;

	}

	/**
	 * Gets the list of setup facets defining possible experiment configurations.
	 * Each facet contains a base configuration and optional modifications.
	 *
	 * @return List of setup configuration facets
	 * @see SetupFacet
	 */
	List<SetupFacet> getSetupConfig();

	/**
	 * Gets the number of replications to generate for each setup configuration.
	 *
	 * @return Number of setup replications (minimum 1)
	 */
	int getSetupReplications();

	/**
	 * Creates a new configuration with the specified execution configuration.
	 *
	 * @param config The new execution configuration to use
	 * @return New ExperimentConfiguration instance
	 */
	default ImmutableExperimentConfiguration withExecutionConfig(
			ImmutableExecutionConfiguration config
	) {
		return ImmutableExperimentConfiguration.builder()
			.from(this)
			.setExecutionConfig(config)
			.build();
	}

	/**
	 * Creates a new configuration with an additional execution facet containing
	 * the specified modifications. The facet will be named with the provided
	 * name parameter.
	 *
	 * @param name   Name for the new execution facet
	 * @param config Modifications to include in the new facet
	 * @return New ExperimentConfiguration instance
	 */
	default ImmutableExperimentConfiguration withFacet(
			String name, PartialExecutionConfiguration... config
	) {
		return ImmutableExperimentConfiguration.builder()
			.from(this)
			.addFacets(
				ImmutableExecutionFacet.builder()
					.setName(name)
					.setModifications(Arrays.asList(config))
					.build()
			)
			.build();
	}

	/**
	 * Creates a new configuration with the specified setup configuration.
	 * Replaces any existing setup configurations with a single facet containing
	 * the provided configuration as its default.
	 *
	 * @param config The new setup configuration to use
	 * @return New ExperimentConfiguration instance
	 */
	default ImmutableExperimentConfiguration withSetupConfig(
			SetupConfiguration config
	) {
		return ImmutableExperimentConfiguration.builder()
			.from(this)
			.setSetupConfig(
				List.of(
					ImmutableSetupFacet.builder()
						.setDefault(config)
						.build()
				)
			)
			.build();
	}

	/**
	 * Serializes this configuration to JSON and writes it to the specified path.
	 * If the path doesn't end with '.json', creates a 'config.json' file in the
	 * specified directory. Creates parent directories if needed.
	 *
	 * @param directoryOrFile Path to directory or file for output
	 * @param overwrite       If true, overwrites existing file; otherwise,
	 *                        throws an exception if the file already exists
	 * @throws StreamWriteException on JSON serialization errors
	 * @throws DatabindException    on data binding issues
	 * @throws IOException          on file system errors
	 * @see #readConfig(Path)
	 */
	default void writeConfig(Path directoryOrFile, boolean overwrite)
			throws StreamWriteException, DatabindException, IOException {
		var om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		if (!directoryOrFile.toString()
			.endsWith(".json")) {
			directoryOrFile = directoryOrFile.resolve("config.json");
		}
		Files.createDirectories(directoryOrFile.getParent());
		if (overwrite) { Files.deleteIfExists(directoryOrFile); }
		om.writeValue(directoryOrFile.toFile(), this);
	}
}
