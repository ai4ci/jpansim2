package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;

@Value.Immutable
public interface OutbreakBaseline extends Serializable {

	// Simulation baseline viral factors
	
	/**
	 * This value is a scale factor for the viral load to make a 
	 * probability that a transmission will occur given a particular
	 * viral load.  
	 * @return
	 */
	double getViralLoadTransmissibilityProbabilityFactor();

	/**
	 * The baseline probability of transmission to someone else given an 
	 * infectious contact is calibrated via R0, given a default contact rate. 
	 * It is modified by exogenous
	 * day to day factors such as seasonality at a population level. Factors such as
	 * individual personal variation, due to individual susceptilibity to infection
	 * ??? and individual day to day 
	 * variation due to things such as mask wearing.   
	 * @return
	 */
	default Double getTransmissibilityBaseline(double viralLoad) {
		return viralLoad*this.getViralLoadTransmissibilityProbabilityFactor();
	};
	
	PolicyState getDefaultPolicyState();
	
	
}
