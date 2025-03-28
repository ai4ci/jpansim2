package io.github.ai4ci.flow;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.abm.Abstraction.Modification;

@Value.Immutable
@JsonSerialize(as = ImmutableExperimentFacet.class)
@JsonDeserialize(as = ImmutableExperimentFacet.class)
public interface ExperimentFacet extends Abstraction.Named {
	
	Map<String,Modification> getModifications();
	
}
