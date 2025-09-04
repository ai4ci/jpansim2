package io.github.ai4ci.config.setup;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.ExoticDistributions;
import io.github.ai4ci.util.Sampler;

@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonSubTypes( {
	@Type(value = ImmutableUnstratifiedDemography.class, name = "unstratified"), 
	@Type(value = ImmutableAgeStratifiedDemography.class, name = "age-stratified"),
	@Type(value = ImmutableLocationAwareDemography.class, name = "location-aware") 
} )
public interface DemographicConfiguration extends Serializable {

	ModifiablePerson createPersonStub(Outbreak outbreak);
	double getRelationshipStrength(Person source, Person target, Sampler sampler);
	
	/*
	 * Used internally to get the quantile of distance between 2 people in the 
	 * network. 
	 */
	@JsonIgnore
	@Value.Lazy default EmpiricalDistribution getEuclidianDistanceCDF() {
		return ExoticDistributions.getEuclidianDistanceDistribution();
	}
	
	/**
	 * A distance measure from 0 (distant) to 1 (identity) based on the quantile
	 * of the euclidian distance in this persons position versus the distribution 
	 * of distances between location of random points in the space.   
	 */
	default double getProximity(Person one, Person two) {
		var dist = Math.sqrt(
			Math.pow(one.getDemographic().getLocationX()-two.getDemographic().getLocationX(),2)+
			Math.pow(one.getDemographic().getLocationY()-two.getDemographic().getLocationY(),2)
		) / Math.sqrt(2.0);
		return 1 - getEuclidianDistanceCDF().getCumulative(dist);
	}
}
