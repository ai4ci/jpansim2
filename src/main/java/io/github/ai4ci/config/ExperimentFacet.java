package io.github.ai4ci.config;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Modification;

@Value.Immutable
@JsonSerialize(as = ImmutableExperimentFacet.class)
@JsonDeserialize(as = ImmutableExperimentFacet.class)
public interface ExperimentFacet extends Abstraction.Named {
	
	Map<String,Modification> getModifications();
	
}
