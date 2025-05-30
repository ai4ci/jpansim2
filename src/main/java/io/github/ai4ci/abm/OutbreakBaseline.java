package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;

@Value.Immutable
/** 
 * Simulation wide parameters that do not change over the course of the 
 * simulation. Many of these are calibrated from the initial configuration of 
 * the model.
 */
public interface OutbreakBaseline extends Serializable {

	// Simulation baseline viral factors
	
	/**
	 * This value is a scale factor for the viral load to make a 
	 * probability that a transmission will occur given a particular
	 * viral load.  
	 */
	double getViralLoadTransmissibilityProbabilityFactor();
	
	/**
	 * Used in calibration of risk models, to help determine the information
	 * value of a contact's risk. 
	 */
	double getExpectedContactsPerPersonPerDay();

	/**
	 * Calibrated from the infection case ratio this gives a simulation
	 * wide cutoff that the average person will experience symptoms. Obtained
	 * from the running the in host model over a set of samples.
	 * @return
	 */
	double getSeveritySymptomsCutoff();
	
	/**
	 * Calibrated from the infection case ratio and the case hospitalisation 
	 * rate this gives a simulation wide cutoff that the average person will 
	 * experience symptoms bad enough to require hospitalisation.
	 * @return
	 */
	double getSeverityHospitalisationCutoff();
	
	/**
	 * Calibrated from the infection case ratio and the case fatality 
	 * rate this gives a simulation wide cutoff that the average person will 
	 * experience symptoms bad enough to die.
	 */
	double getSeverityDeathCutoff();
	
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
	
	/**
	 * Calibrated from the 95% quantile of the infectivity profile of the in host
	 * model run over a set of unadjusted parameters
	 */
	int getInfectiveDuration();
	
	/**
	 * Calibrated from the 95% quantile of the severity profile of the in host
	 * model run over a set of unadjusted parameters
	 */
	int getSymptomDuration();
	
	
	
}
