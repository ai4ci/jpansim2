package io.github.ai4ci.flow;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;

@Value.Immutable
public interface ExperimentFacet extends Abstraction.Named {

	@Value.Default default int getReplications() {return 1;}
	@Value.Default default PartialSetupConfiguration getSetupModifier() {return null;}
	@Value.Default default PartialExecutionConfiguration getExecutionModifier()  {return null;}
	 
}
