package io.github.ai4ci.config;

import org.immutables.value.Value;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Configuration for running a set of experiments as a batch job. This 
 * auto detects if the job is running in the context of a SLURM multi-node 
 * batch job and if so provides a mechanism to subset the overall configuration
 * depending on the number of SLURM nodes involved. This means that the instance 
 * of JPanSim2 running on one SLURM node does not repeat the work done by others.
 * It also prevents SLURM nodes from overwriting each others outputs by providing
 * node specific output directories.  
 */
@Value.Immutable
@JsonSerialize(as = ImmutableBatchConfiguration.class)
@JsonDeserialize(as = ImmutableBatchConfiguration.class)
public interface BatchConfiguration {

	/**
	 * A sensible default batch configuration used in examples and tests.
	 *
	 * <p>The default values provide a compact set of parameters used as a
	 * baseline in examples and tests. Downstream callers that rely on this simple default include example experiments.
	 *
	 * @see io.github.ai4ci.example.Experiment
	 */
	public static ImmutableBatchConfiguration DEFAULT = 
			ImmutableBatchConfiguration.builder()
			.setSimulationDuration(200)
			.setUrnBase("default")
			.setExporters(
				Exporters.DEMOGRAPHICS,
				Exporters.SUMMARY,
				Exporters.INFECTIVITY_PROFILE,
				Exporters.FINAL_STATE,
				Exporters.BEHAVIOUR,
				Exporters.CONTACT_COUNTS
			)
			.build();

	// static Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

	/**
	 * Gets the base URN for the batch configuration.
	 * @return The base URN as a string.
	 */
	@Value.Default default String getUrnBase() {return "";}

	/**
	 * Gets the duration of the simulation.
	 * @return The simulation duration in time units.
	 */
	int getSimulationDuration();

	/**
	 * Checks if the current execution is part of a SLURM batch job by looking for the presence of the SLURM_ARRAY_TASK_ID environment variable.
	 * @return true if the SLURM_ARRAY_TASK_ID environment variable is set, indicating that the code is running as part of a SLURM batch job; false otherwise.
	 */
	@JsonIgnore
	@Value.Derived default boolean isSlurmBatch() {
		return System.getenv("SLURM_ARRAY_TASK_ID") != null;
	};

	/**
	 * Default value will populate from the SLURM_ARRAY_TASK_ID environment
	 * variable. Only set this if you know what you are doing
	 * @return The batch number, which is the index of the current job in the SLURM array. If not running in a SLURM batch job, it defaults to 1.
	 */
	@JsonIgnore
	@Value.Derived default int getBatchNumber() {
		String tmp = System.getenv("SLURM_ARRAY_TASK_ID"); // will be set to the job array index value or null.
		if (tmp != null)  return Integer.parseInt(tmp);
		return 1;
	}

	/**
	 * Default value will populate from the SLURM_ARRAY_TASK_COUNT environment
	 * variable. Only set this if you know what you are doing
	 * 
	 * @return The total number of batches, which is the total number of jobs in the
	 *         SLURM array. If not running in a SLURM batch job, it defaults to 1.
	 */
	@JsonIgnore
	@Value.Derived default int getBatchTotal() {
		String tmp = System.getenv("SLURM_ARRAY_TASK_COUNT"); //will be set to the number of tasks in the job array or null.
		if (tmp != null) return  Integer.parseInt(tmp);
		return 1;
	}

	/**
	 *  Gets the batch name, which is a combination of the batch number and total number of batches. This is useful for identifying the specific batch job when running multiple batches in parallel.
	 *  @return The batch name in the format "batchNumber/batchTotal".
	 */
	@JsonIgnore
	@Value.Derived default String getBatchName() {
		return getBatchNumber()+"/"+getBatchTotal();
	} 

	/**
	 * Gets the list of exporters to be used in the simulation.
	 * @return An array of exporters.
	 */
	@Value.Default default Exporters[] getExporters() {
		return new Exporters[] {
				Exporters.DEMOGRAPHICS,
				Exporters.SUMMARY,
				Exporters.INFECTIVITY_PROFILE,
				Exporters.LINELIST,
				Exporters.FINAL_STATE
		};
	};

}