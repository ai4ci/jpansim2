package io.github.ai4ci.abm;

import io.github.ai4ci.abm.mechanics.Abstraction.TemporalState;

/**
 * Any data related to the state of the outbreak which is valid only in a 
 * specific time point. This list of things is shared between the 
 * {@link OutbreakState} and {@link OutbreakHistory} 
 */
public interface OutbreakTemporalState extends TemporalState<Outbreak>, Cloneable {
	
	long getInfectedCount();
	long getIncidence();
	/**
	 * The number of tests reported positive on the current simulation date. This
	 * is reported on the date the test result is available (not when the test
	 * was taken). 
	 */
	long getTestPositivesByResultDate();
	
	/**
	 * The number of tests reported negative on the current simulation date. This
	 * is reported on the date the test result is available (not when the test
	 * was taken).
	 */
	long getTestNegativesByResultDate();
	long getCumulativeInfections();
	long getCumulativeAdmissions();
	long getMaximumIncidence();
	long getTimeToMaximumIncidence();
	long getMaximumHospitalBurden();
	double getMaximumPrevalence();
	
	double getCumulativeMobilityDecrease();
	double getCumulativeComplianceDecrease();
}