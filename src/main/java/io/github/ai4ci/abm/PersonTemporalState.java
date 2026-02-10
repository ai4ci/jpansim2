package io.github.ai4ci.abm;

import io.github.ai4ci.abm.Abstraction.TemporalState;

/**
 * Any data related to the state of the people in the model which is valid only in a 
 * specific time point. This list of things is shared between the 
 * {@link PersonState} and {@link PersonHistory}. 
 */
public interface PersonTemporalState extends TemporalState<Person> {
	
	/**
	 * Determines if the person's internal viral load is above the threshold for potential infectivity.
	 * This is calculated by comparing the value returned by {@link #getNormalisedViralLoad()}
	 * against a predefined infectivity threshold, which is typically configured within the
	 * {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 * A person is considered infectious if their normalized viral load surpasses this threshold.
	 *
	 * @return {@code true} if the person is infectious, {@code false} otherwise.
	 */
	boolean isInfectious();
	
	/**
	 * Determines if the person's internal infected targets are above the threshold for 
	 * exhibiting symptoms. This is calculated by comparing the value returned by 
	 * {@link #getNormalisedSeverity()} against a predefined symptom threshold, 
	 * which is typically configured within the 
	 * {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 *
	 * @return {@code true} if the person is symptomatic, {@code false} otherwise.
	 */
	boolean isSymptomatic();
	
	/**
	 * Determines if the person's internal infected targets are above the threshold for 
	 * exhibiting symptoms and they have reported it, typically through a smart agent mechanism.
	 * This state is dependent on {@link #isSymptomatic()} being true and the person actively
	 * reporting their symptoms based on their behavioral model.
	 *
	 * @return {@code true} if the person is symptomatic and has reported it, {@code false} otherwise.
	 */
	boolean isReportedSymptomatic();
	
	/** 
	 * Determines if the person's disease severity is high enough to require hospitalisation.
	 * This is calculated by comparing the value returned by {@link #getNormalisedSeverity()}
	 * against a predefined hospitalisation threshold, which is typically configured within the
	 * {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 *
	 * @return {@code true} if the person requires hospitalisation, {@code false} otherwise.
	 */ 
	boolean isRequiringHospitalisation();
	/**
	 * Determines if the person is deceased. This state is typically set when the person's
	 * disease severity surpasses a mortality threshold or a specific mortality event occurs
	 * within the simulation, as defined in the in-host model and {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 *
	 * @return {@code true} if the person is dead, {@code false} otherwise.
	 */
	boolean isDead();
	
	/**
	 * A continuous value for the severity of disease, as determined by the in-host
	 * model. This value is interpreted relative to disease cutoffs, which are
	 * calibrated during the model setup (e.g., in
	 * {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's
	 * SetupConfiguration}). It is not strictly normalized but provides a relative
	 * measure of disease progression.
	 * 
	 * @return A severity value, where higher values indicate more severe disease.
	 *         The interpretation of this value is relative to predefined thresholds
	 *         for symptoms, hospitalisation, and mortality.
	 */
	double getNormalisedSeverity();
	
	/**
	 * A continuous value for the viral load of disease, as determined by the
	 * in-host model. This is scaled such that it is 0 if uninfected and 1 if
	 * minimally infectious. Values smaller than 1 imply that viral load may be
	 * detectable by tests but not necessarily infectious. This scaling may be
	 * revisited to more formally relate viral load with infectivity, potentially
	 * referencing parameters in
	 * {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's
	 * SetupConfiguration}.
	 * 
	 * @return A normalized viral load value, where 0 indicates no infection and 1
	 *         indicates the threshold for minimal infectivity, it has no upper bound. Values between 0 and
	 *         1 represent varying levels of viral load, with higher values
	 *         indicating greater potential infectivity.
	 */
	double getNormalisedViralLoad();
	
	/**
	 * The viral exposure an individual experienced in a day as a result of all 
	 * contacts in the model. This value is accumulated from {@link Contact} events
	 * and may also include a random chance of exposure due to importation, as defined
	 * by the simulation's contact and importation models. This value should be on the same
	 * scale as the {@link #getNormalisedViralLoad() viral load} for consistent comparison.
	 * 
	 * @return The total viral exposure for the day, which may be 0 if no exposure occurred.  
	 */
	double getContactExposure();
	
	/**
	 * An estimate of the local prevalence as perceived by a smart agent, calculated
	 * based on the probability of infection in today's contacts. This estimation
	 * typically involves aggregating information from observed contacts and applying
	 * a heuristic or model to infer local infection rates.
	 * 
	 * @return a probability value representing the smart agent's estimate of local prevalence
	 * ,
	 */
	double getPresumedLocalPrevalence(); 
	
	/**
	 * Indicates if this {@link PersonTemporalState} represents the first day of a new exposure episode.
	 * An incident exposure is typically defined as the first day a person experiences any
	 * {@link Exposure} after a period of no exposure, or after a sufficiently long period
	 * to consider it a new episode, as determined by the simulation's exposure tracking logic.
	 *
	 * @return {@code true} if this is an incident exposure, {@code false} otherwise.
	 */
	boolean isIncidentExposure();
	/**
	 * Indicates if this {@link PersonTemporalState} represents the first day the person becomes infectious
	 * in a new infection episode. This is distinct from merely being exposed. An incident infection
	 * occurs when the person's {@link #getNormalisedViralLoad()} crosses the infectivity threshold
	 * for the first time in a given infection cycle, as defined by the in-host model and
	 * {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 *
	 * @return {@code true} if this is an incident infection, {@code false} otherwise.
	 */
	boolean isIncidentInfection();
	
	/**
	 * The infectious period as currently estimated by the model, which may not be the true biological value.
	 * This is calculated by summing the {@link #incubPeriod() incubation period} and the
	 * {@link io.github.ai4ci.abm.ModelNav#modelState(PersonTemporalState) model's presumed infectious period},
	 * which is typically sourced from the {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 *
	 * @return The estimated infectious period in days.
	 */
	default int infPeriod() {
		return incubPeriod() + ModelNav.modelState(this).getPresumedInfectiousPeriod();
	}

	/**
	 * The incubation period as currently estimated by the model, which may not be the true biological value.
	 * This value is retrieved from the {@link io.github.ai4ci.abm.ModelNav#modelState(PersonTemporalState) model's presumed incubation period},
	 * which is typically sourced from the {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() Outbreak's SetupConfiguration}.
	 *
	 * @return The estimated incubation period in days.
	 */
	default int incubPeriod() {
		return ModelNav.modelState(this).getPresumedIncubationPeriod();
	}
}