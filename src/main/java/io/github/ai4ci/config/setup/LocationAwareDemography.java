package io.github.ai4ci.config.setup;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
@JsonSerialize(as = ImmutableLocationAwareDemography.class)
@JsonDeserialize(as = ImmutableLocationAwareDemography.class)
public interface LocationAwareDemography extends DemographicConfiguration {
 
	public static ImmutableLocationAwareDemography DEFAULT = 
			ImmutableLocationAwareDemography.builder()
				.setContactProximityBias(2.0)
				.build();
	
	default ModifiablePerson createPersonStub(Outbreak outbreak) {
		ModifiablePerson tmp = Person.createPersonStub(outbreak);
		return tmp;
	}
	
	/**
	 * Calibrates the strength of association between distance and contact 
	 * probability. This is an inverse odds ratio which is applied to the baseline
	 * probability of contact. Values above 1 will tend to make
	 * peoples contact radius smaller. Values below 1 will make it bigger. 
	 */
	Double getContactProximityBias();
	
	default public double getRelationshipStrength(Person source, Person target, Sampler sampler) {
		return Conversions.scaleProbabilityByOR( getProximity(source, target), 1/getContactProximityBias() );
	}
	
	

}
