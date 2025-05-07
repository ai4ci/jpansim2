package io.github.ai4ci.output;

import org.immutables.value.Value;

import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;

@Value.Immutable
public interface OutbreakConfigurationJson extends CommonCSV.Execution {
	
	public SetupConfiguration getSetupConfiguration();
	public ExecutionConfiguration getExecutionConfiguration();
	
}
