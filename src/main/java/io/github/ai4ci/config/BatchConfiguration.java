package io.github.ai4ci.config;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableBatchConfiguration.class)
@JsonDeserialize(as = ImmutableBatchConfiguration.class)
public interface BatchConfiguration {

	public static ImmutableBatchConfiguration DEFAULT = 
			ImmutableBatchConfiguration.builder()
			.setSimulationDuration(200)
			.setUrnBase("default")
			.build();

	static Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

	@Value.Default default String getUrnBase() {return "";}

	//	@Value.Default default Path getDirectoryPath() {
	//		return SystemUtils.getUserDir().toPath();
	//	};
	//	
	//	@Value.Default default Path getExecutionConfigurationPath() {
	//		Path tmp = getDirectoryPath().resolve("config.json");
	//		log.info("No input configuration given defaulting to: "+tmp);
	//		return tmp;
	//	};
	//	
	int getSimulationDuration();

	@JsonIgnore
	@Value.Derived default boolean isSlurmBatch() {
		return System.getProperty("SLURM_ARRAY_TASK_ID") != null;
	};

	/**
	 * Default value will populate from the SLURM_ARRAY_TASK_ID environment
	 * variable. Only set this if you know what you are doing
	 */
	@JsonIgnore
	@Value.Derived default int getBatchNumber() {
		String tmp = System.getProperty("SLURM_ARRAY_TASK_ID"); // will be set to the job array index value or null.
		if (tmp != null)  return Integer.parseInt(tmp);
		return 1;
	}

	/**
	 * Default value will populate from the SLURM_ARRAY_TASK_COUNT environment
	 * variable. Only set this if you know what you are doing
	 */
	@JsonIgnore
	@Value.Derived default int getBatchTotal() {
		String tmp = System.getProperty("SLURM_ARRAY_TASK_COUNT"); //will be set to the number of tasks in the job array or null.
		if (tmp != null) return  Integer.parseInt(tmp);
		return 1;
	}

	@JsonIgnore
	@Value.Derived default String getBatchName() {
		return getBatchNumber()+"/"+getBatchTotal();
	} 

	@Value.Default default Exporters[] getExporters() {
		return new Exporters[] {
				Exporters.DEMOGRAPHICS,
				Exporters.SUMMARY,
				Exporters.INFECTIVITY_PROFILE,
				Exporters.INTERNAL_STATE
				// TODO: true test positive line list.
		};
	};

}
