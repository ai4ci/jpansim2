package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.State.PolicyState;
import io.github.ai4ci.functions.DelayDistribution;

@Value.Immutable
/**
 * Simulation wide parameters that do not change over the course of the
 * simulation. Many of these are calibrated from the initial configuration of
 * the model.
 *
 * @see io.github.ai4ci.abm.builders.DefaultOutbreakBaseliner.baselineOutbreak(Builder,
 *      Outbreak, Sampler)
 * @see io.github.ai4ci.abm.Calibration
 */
public interface OutbreakBaseline extends Serializable {

	// Simulation baseline viral factors

	/**
	 * Used in calibration of risk models, to help determine the information value
	 * of a contact's risk.
	 *
	 * @return the expected number of contacts per person per day in the baseline
	 *         contact network, calibrated from the initial configuration of the
	 *         model (e.g. network topology and contact rates).
	 *
	 */
	double getExpectedContactsPerPersonPerDay();

	/**
	 * Calibrated from the infection case ratio this gives a simulation wide cutoff
	 * that the average person will experience symptoms. Obtained from the running
	 * the in host model over a set of samples.
	 *
	 * @return severity value threshold for symptomatic infections
	 */
	double getSeveritySymptomsCutoff();

	/**
	 * Calibrated from the infection case ratio and the case hospitalisation rate
	 * this gives a simulation wide cutoff that the average person will experience
	 * symptoms bad enough to require hospitalisation.
	 *
	 * @return severity value threshold for hospitalisation
	 */
	double getSeverityHospitalisationCutoff();

	/**
	 * Calibrated from the infection case ratio and the case fatality rate this
	 * gives a simulation wide cutoff that the average person will experience
	 * symptoms bad enough to die.
	 *
	 * @return severity value threshold for death
	 */
	double getSeverityDeathCutoff();

	/**
	 * This value is a scale factor for the viral load to make a probability that a
	 * transmission will occur given a particular viral load. This is calibrated
	 * using the viral load profile and an overall probability of per edge
	 * transmission.
	 * {@link Calibration#inferViralLoadTransmissionParameter(Outbreak, double)}
	 *
	 * @return the calibrated transmissibility scale parameter (k) applied to viral
	 *         load
	 */
	double getViralLoadTransmissibilityParameter();

	/**
	 * This is the per contact network edge baseline probability of transmission
	 * (e.g. T in Koch et al 2013 Edge removal in random contact networks).
	 *
	 * The baseline probability of transmission to someone else given an infectious
	 * contact is calibrated via R0, given a default contact rate but depoends on
	 * the viral load from the in host model. It is modified by exogenous day to day
	 * factors such as seasonality at an individual level in
	 * {@link PersonState#getAdjustedTransmissibility()}, which accounts for factors
	 * such as individual personal variation of susceptibility to infection (e.g.
	 * medications, such as PreP) and individual day to day variation of
	 * transmission due to things such as mask wearing.
	 *
	 * @param viralLoad the (unitless) viral load value from the in-host model
	 * @return baseline per-contact transmission probability for the given viral
	 *         load
	 */
	default Double getTransmissibilityBaseline(double viralLoad) {
		return transmissibilityFromViralLoad(viralLoad, getViralLoadTransmissibilityParameter());
	};

	/**
	 * All translation of viral load to transmission probability happens through
	 * this method, including threshold behaviour. The parameter is calibrated to
	 * match simulation R0 given the network topology by
	 * {@link Calibration#inferViralLoadTransmissionParameter(Outbreak, double)}.
	 *
	 * The function that links viral load to transmission is:
	 *
	 * <pre>
	 * P(transmission) = 1 - exp(-k * (v - 1))
	 * </pre>
	 *
	 * Behaviour: returns 0 for viral loads less than 1 (below threshold), otherwise
	 * applies the exponential mapping.
	 *
	 * @param viralLoad the viral load value (unitless)
	 * @param parameter the calibrated scale parameter (k)
	 * @return probability in [0,1] of transmission on a single contact
	 */
	static double transmissibilityFromViralLoad(double viralLoad, double parameter) {
		if (viralLoad < 1) {
			return 0D;
		}
		return 1 - Math.exp(-parameter * (viralLoad - 1));
	}

	/**
	 * The default policy state applied to agents at simulation start. This encodes
	 * baseline non-pharmaceutical interventions such as masking, social distancing
	 * or closures that are present for the outbreak baseline.
	 *
	 * @return the default {@link PolicyState} for agents at simulation start
	 */
	State.PolicyState getDefaultPolicyState();

	/**
	 * The infectivity profile is a time-varying distribution (indexed by days since
	 * infection) that describes relative infectiousness. It is used to compute
	 * time-dependent infectivity and to derive infective duration.
	 *
	 * @return the {@link DelayDistribution} representing the infectivity profile
	 */
	DelayDistribution getInfectivityProfile();

	/**
	 * Calibrated from the 95% quantile of the infectivity profile of the in host
	 * model run over a set of unadjusted parameters
	 *
	 * @return the infective duration in model time units (e.g. days) derived from
	 *         the infectivity profile, used as the default duration of
	 *         infectiousness in the simulation and for calibration of risk models
	 *         that rely on a fixed infectious duration. Note that the actual
	 *         duration of infectiousness for a given person in the simulation may
	 *         be shorter or longer than this value, depending on their viral load
	 *         trajectory and the infectiousness cutoff used to determine when they
	 *         are considered infectious.
	 *
	 */
	@Value.Derived
	default int getInfectiveDuration() {
		return (int) getInfectivityProfile().size();
	};

	/**
	 * Calibrated from the 95% quantile of the severity profile of the in host model
	 * run over a set of unadjusted parameters
	 *
	 * @return the symptom duration in model time units (e.g. days) derived from the
	 *         severity profile, used as the default duration of symptoms in the
	 *         simulation and for calibration of risk models that rely on a fixed
	 *         symptom duration. Note that the actual duration of symptoms for a
	 *         given person in the simulation may be shorter or longer than this
	 *         value, depending on their viral load trajectory and the severity
	 *         cutoff used to determine when they are considered symptomatic.
	 *
	 */
	int getSymptomDuration();

}
