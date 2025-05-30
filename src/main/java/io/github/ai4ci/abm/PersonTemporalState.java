package io.github.ai4ci.abm;

import io.github.ai4ci.abm.mechanics.Abstraction.TemporalState;
import io.github.ai4ci.util.ModelNav;

/**
 * Any data related to the state of the people in the model which is valid only in a 
 * specific time point. This list of things is shared between the 
 * {@link PersonState} and {@link PersonHistory}. 
 */
public interface PersonTemporalState extends TemporalState<Person> {
	
	/**
	 * Is the persons internal viral load above the threshold for potential 
	 * infectivity.
	 */
	boolean isInfectious();
	
	/**
	 * Is the persons internal infected targets above the threshold for 
	 * exhibiting symptoms.
	 */
	boolean isSymptomatic();
	
	/**
	 * Is the persons internal infected targets above the threshold for 
	 * exhibiting symptoms and they have reported it on a smart agent.
	 */
	boolean isReportedSymptomatic();
	
	/** Is the persons disease severe enough to require hospitalisation */ 
	boolean isRequiringHospitalisation();
	boolean isDead();
	
	/** A continuous value for the severity of disease, as determined by the in
	 * host model. This is interpreted relative to disease cutoffs, which are 
	 * calibrated, so its not really normalised at all and this may change. 
	 */
	double getNormalisedSeverity();
	
	/** A continuous value for the viral load of disease, as determined by the in
	 * host model. This is scaled to be 0 if uninfected and 1 if minimally 
	 * infectious. Values smaller than 1 imply viral load may be detectible by 
	 * tests but not infectious. This may need to be revisited with a view to 
	 * more formally relating viral load with infectivity.
	 */
	double getNormalisedViralLoad();
	
	/**
	 * The viral exposure an individual experienced in a day as a result of all 
	 * contacts in the model (there is also a random chance of exposure due to 
	 * importation). This should be on the same scale as the viral load. 
	 */
	double getContactExposure();
	
//	/**
//	 * The log likelihood of disease based on the patient's symptoms at a given 
//	 * point in time. 
//	 */
//	double getSymptomLogLikelihood();
	
	/**
	 * An estimate of the local prevalence as estimated by the probability of 
	 * infection in todays contacts, as might be calculated by a smart agent.
	 */
	double getPresumedLocalPrevalence(); 
	
	default int infPeriod() {
		return incubPeriod() + ModelNav.modelState(this).getPresumedInfectiousPeriod();
	}

	default int incubPeriod() {
		return ModelNav.modelState(this).getPresumedIncubationPeriod();
	}
}