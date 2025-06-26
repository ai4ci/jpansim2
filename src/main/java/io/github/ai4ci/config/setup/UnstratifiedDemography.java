package io.github.ai4ci.config.setup;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
@JsonSerialize(as = ImmutableUnstratifiedDemography.class)
@JsonDeserialize(as = ImmutableUnstratifiedDemography.class)
public interface UnstratifiedDemography extends DemographicConfiguration{

	ImmutableUnstratifiedDemography DEFAULT = ImmutableUnstratifiedDemography.builder().build(); 
	
	@JsonIgnore
	default ModifiablePerson createPersonStub(Outbreak outbreak) {
		return Person.createPersonStub(outbreak);
	}
	
	@JsonIgnore
	default double getRelationshipStrength(Person source, Person target, Sampler sampler) {
		return sampler.uniform();
	}
	
}
