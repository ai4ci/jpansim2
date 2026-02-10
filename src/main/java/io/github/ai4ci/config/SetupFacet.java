package io.github.ai4ci.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.config.setup.ImmutableSetupConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;

@Value.Immutable
@JsonSerialize(as = ImmutableSetupFacet.class)
@JsonDeserialize(as = ImmutableSetupFacet.class)
public interface SetupFacet {
	SetupConfiguration getDefault();
	@Value.Default default List<PartialSetupConfiguration> getModifications() { 
		return Collections.emptyList(); 
	}
	
	public static ImmutableSetupFacet of(ImmutableSetupConfiguration config, PartialSetupConfiguration... modifications ) {
		return ImmutableSetupFacet.builder().setDefault(config).addModifications(modifications).build();
	}
}