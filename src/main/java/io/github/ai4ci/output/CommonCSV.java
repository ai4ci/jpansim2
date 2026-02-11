package io.github.ai4ci.output;

import io.github.ai4ci.flow.output.CSVWriter;

/**
 * CommonCSV provides a hierarchical interface structure for CSV output data
 * models in agent-based simulation systems.
 *
 * <p>
 * Main purpose: define common metadata and execution context shared by the
 * various CSV record types used across the simulation output pipeline. The
 * interfaces here form the base contract consumed by CSV and DuckDB writer
 * implementations.
 *
 * <p>
 * Downstream uses: consumed by {@link io.github.ai4ci.flow.output.CSVWriter}
 * and {@link io.github.ai4ci.flow.output.DuckDBWriter} implementations and by
 * exporters in {@link io.github.ai4ci.flow.output.Export} to standardise
 * exportable records.
 *
 * @author Rob Challen
 */

public interface CommonCSV {

	/**
	 * Execution interface extends Model with experiment execution context. Adds
	 * experiment replica information to track multiple runs.
	 */
	public interface Execution extends Model {
		/**
		 * Get the replica identifier for the execution run.
		 *
		 * @return the replica identifier for experiment execution; used to
		 *         distinguish between independent simulation runs within the same
		 *         experiment
		 */
		int getExperimentReplica();
	}

	/**
	 * Model interface represents the core simulation model metadata. Extends
	 * CSVWriter.Writeable to ensure CSV serialization capability. Contains basic
	 * model identification and experiment context.
	 */
	public interface Model extends CSVWriter.Writeable {
		/**
		 * Get the experiment name this record belongs to.
		 *
		 * @return the experiment name used in the top‑level experiment
		 *         configuration and exported alongside simulation data
		 */
		String getExperimentName();

		/**
		 * Get the name of the simulation model.
		 *
		 * @return the name identifier of the simulation model; typically a short
		 *         string used to identify the model implementation in exported
		 *         metadata
		 */
		String getModelName();

		/**
		 * Get the replica index used for the model configuration.
		 *
		 * @return the replica identifier for model variation; this is an integer
		 *         used to distinguish repeated parameter replicates when
		 *         analysing outputs
		 */
		int getModelReplica();
	}

	/**
	 * State interface extends Execution with time-specific state data.
	 * Represents the complete simulation state at a specific time point. Adds
	 * temporal dimension to the execution context.
	 */
	public interface State extends Execution {
		/**
		 * Get the simulation time step associated with this record.
		 *
		 * @return the simulation time step or timestamp; typically an integer
		 *         representing the discrete time step at which the exported state
		 *         was recorded
		 */
		int getTime();
	}

}