package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;
import io.github.ai4ci.util.DelayDistribution;

@Value.Immutable
/**
 * Simulation wide parameters that do not change over the course of the
 * simulation. Many of these are calibrated from the initial configuration of
 * the model.
 */
public interface OutbreakBaseline extends Serializable {

	// Simulation baseline viral factors

	/**
	 * Used in calibration of risk models, to help determine the information value
	 * of a contact's risk.
	 */
	double getExpectedContactsPerPersonPerDay();

	/**
	 * Calibrated from the infection case ratio this gives a simulation wide cutoff
	 * that the average person will experience symptoms. Obtained from the running
	 * the in host model over a set of samples.
	 * 
	 * @return
	 */
	double getSeveritySymptomsCutoff();

	/**
	 * Calibrated from the infection case ratio and the case hospitalisation rate
	 * this gives a simulation wide cutoff that the average person will experience
	 * symptoms bad enough to require hospitalisation.
	 * 
	 * @return
	 */
	double getSeverityHospitalisationCutoff();

	/**
	 * Calibrated from the infection case ratio and the case fatality rate this
	 * gives a simulation wide cutoff that the average person will experience
	 * symptoms bad enough to die.
	 */
	double getSeverityDeathCutoff();

	/**
	 * This value is a scale factor for the viral load to make a probability that a
	 * transmission will occur given a particular viral load. This is calibrated
	 * using the viral load profile and an overall probability of per edge
	 * transmission.
	 * {@link Calibration#inferViralLoadTransmissionParameter(Outbreak, double)}
	 */
	double getViralLoadTransmissibilityParameter();

	/**
	 * This is the per contact network edge baseline probability of transmission
	 * (e.g. T in Koch et al 2013 Edge removal in random contact networks).
	 * 
	 * The baseline probability of transmission to someone else given an infectious
	 * contact is calibrated via R0, given a default contact rate. It is modified by
	 * exogenous day to day factors such as seasonality at a population level.
	 * Factors such as individual personal variation of susceptibility to infection
	 * (e.g. medications, such as PreP) and individual day to day variation of
	 * transmission due to things such as mask wearing.
	 */
	default Double getTransmissibilityBaseline(double viralLoad) {
		return transmissibilityFromViralLoad(viralLoad, getViralLoadTransmissibilityParameter());
	};

	/**
	 * All translation of viral load to transmission probability happens through
	 * this method, including threshold behaviour. The parameter is calibrated to
	 * match simulation R0 given the network topology by
	 * {@link Calibration#inferViralLoadTransmissionParameter(Outbreak, double)}.
	 */
	static double transmissibilityFromViralLoad(double viralLoad, double parameter) {
		if (viralLoad < 1)	return 0D;
		return 1-Math.exp(-parameter*(viralLoad - 1));
	}

	PolicyState getDefaultPolicyState();

	DelayDistribution getInfectivityProfile();
	
	/**
	 * Calibrated from the 95% quantile of the infectivity profile of the in host
	 * model run over a set of unadjusted parameters
	 */
	@Value.Derived default int getInfectiveDuration() {
		return (int) getInfectivityProfile().size();
	};

	/**
	 * Calibrated from the 95% quantile of the severity profile of the in host model
	 * run over a set of unadjusted parameters
	 */
	int getSymptomDuration();

}
