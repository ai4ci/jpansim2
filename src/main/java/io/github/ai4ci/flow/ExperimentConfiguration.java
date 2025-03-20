package io.github.ai4ci.flow;

import java.util.List;

import org.immutables.value.Value;

import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.ExecutionConfiguration;

@Value.Immutable
public interface ExperimentConfiguration {

	ExperimentConfiguration DEFAULT = ImmutableExperimentConfiguration.builder()
			.setSetupConfig(SetupConfiguration.DEFAULT)
			.setExecutionConfig(ExecutionConfiguration.DEFAULT)
			.build();
	 
	SetupConfiguration getSetupConfig();
	ExecutionConfiguration getExecutionConfig();
	List<ExperimentFacet> getFacets();
	@Value.Default default int getReplications() {return 1;}
}
