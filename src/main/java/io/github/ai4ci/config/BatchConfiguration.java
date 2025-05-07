package io.github.ai4ci.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.output.StateExporter;
import io.github.ai4ci.output.StateExporter.ExportSelector;

@Value.Immutable
public interface BatchConfiguration {
	
	static Logger log = LoggerFactory.getLogger(BatchConfiguration.class);

	@Value.Default default String getUrnBase() {return "";}
	
	@Value.Default default Path getDirectoryPath() {
		return SystemUtils.getUserDir().toPath();
	};
	
	@Value.Default default Path getExecutionConfigurationPath() {
		Path tmp = getDirectoryPath().resolve("config.json");
		log.info("No input configuration given defaulting to: "+tmp);
		return getDirectoryPath().resolve("config.json");
	};
	
	int getSimulationDuration();
	
	@Value.Default default int getBatchNumber() {return 1;}
	@Value.Default default int getBatchTotal() {return 1;}
	
	@Value.Default default Exporters[] getExporters() {
		return new Exporters[] {
				Exporters.DEMOGRAPHICS,
				Exporters.SUMMARY,
				Exporters.INFECTIVITY_PROFILE,
				Exporters.INTERNAL_STATE
				// TODO: true test positive line list.
		};
	};
	
	
	default Path getBatchDirectoryPath() {
		if (this.getBatchTotal()<=1) return getDirectoryPath(); 
		return getDirectoryPath().resolve(""+this.getBatchNumber());
	}
	
	default StateExporter exporter() {
		return StateExporter.of(
				getBatchDirectoryPath(), 
				Arrays.stream(getExporters()).map(e ->e.getSelector()).collect(Collectors.toList())
		);
	}
	
}
