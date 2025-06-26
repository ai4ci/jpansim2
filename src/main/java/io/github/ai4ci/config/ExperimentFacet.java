package io.github.ai4ci.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.config.setup.ImmutableSetupConfiguration;
import io.github.ai4ci.config.setup.PartialSetupConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;

public interface ExperimentFacet {
	
//	@Value.Default default List<? extends Modification<X>> getModifications() { 
//		return Collections.emptyList(); 
//	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableExecutionFacet.class)
	@JsonDeserialize(as = ImmutableExecutionFacet.class)
	public static interface ExecutionFacet extends Abstraction.Named {
		@Value.Default default List<PartialExecutionConfiguration> getModifications() { 
			return Collections.emptyList(); 
		}
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableSetupFacet.class)
	@JsonDeserialize(as = ImmutableSetupFacet.class)
	public static interface SetupFacet {
		SetupConfiguration getDefault();
		@Value.Default default List<PartialSetupConfiguration> getModifications() { 
			return Collections.emptyList(); 
		}
		
		public static ImmutableSetupFacet of(ImmutableSetupConfiguration config, PartialSetupConfiguration... modifications ) {
			return ImmutableSetupFacet.builder().setDefault(config).addModifications(modifications).build();
		}
	}
	
	
}
