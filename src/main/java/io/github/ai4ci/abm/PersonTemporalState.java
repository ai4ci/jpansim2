package io.github.ai4ci.abm;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Abstraction.TemporalState;

public interface PersonTemporalState extends TemporalState<Person> {
	
	/**
	 * Is the persons internal viral load above the threshold for potential 
	 * infectivity.
	 */
	@Value.Derived default boolean isInfectious() {
		return getNormalisedViralLoad() > 1;
	}
	
	/**
	 * Is the persons internal infected targets above the threshold for 
	 * exhibiting symptoms.
	 */
	boolean isSymptomatic();
	
	Double getNormalisedSeverity();
	Double getNormalisedViralLoad();
	Double getAdjustedTransmissibility();
	Double getAdjustedMobility();
	
	
}