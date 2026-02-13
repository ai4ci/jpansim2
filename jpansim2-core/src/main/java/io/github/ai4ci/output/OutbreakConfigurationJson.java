package io.github.ai4ci.output;

import org.immutables.value.Value;

import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;

/**
 * Execution and setup configuration snapshot exported at execution start.
 *
 * <p>
 * Main purpose: capture the full setup and execution configuration used for a
 * simulation execution. Records are produced at the START stage and are written
 * per execution as JSON serialisable objects; the immutable type may be
 * included alongside CSV outputs for reproducibility.
 *
 * <p>
 * Downstream uses: used to reproduce experiments, for auditing and for
 * supplying configuration metadata to analysis pipelines.
 *
 * @author Rob Challen
 */
@Value.Immutable
public interface OutbreakConfigurationJson extends CommonCSV.Execution {

	/**
	 * Get the execution configuration used to run the outbreak.
	 *
	 * @return the execution configuration instance describing runtime policies,
	 *         testing and in‑host model selection
	 */
	public ExecutionConfiguration getExecutionConfiguration();

	/**
	 * Get the setup configuration used to create the outbreak.
	 *
	 * @return the setup configuration instance describing population and network
	 *         generation parameters
	 */
	public SetupConfiguration getSetupConfiguration();

}