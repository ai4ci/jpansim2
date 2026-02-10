package io.github.ai4ci.abm;

import io.github.ai4ci.abm.Abstraction.TemporalState;

/**
 * Any data related to the state of the outbreak which is valid only in a
 * specific time point. This list of things is shared between the
 * {@link OutbreakState} and {@link OutbreakHistory} and will be automatically
 * copied across by the {@link HistoryMapper}. Generally these things are only
 * implemented in {@link OutbreakState} and the temporal version is just a copy
 * of the data. As such they have to be things that don't change retrospectively
 * and you won't find observed values here, which depend on the output of
 * testing and hence tend to be in {@link OutbreakHistory}.
 */
public interface OutbreakTemporalState extends TemporalState<Outbreak>, Cloneable {

	/**
	 * A true value:
	 *
	 * @return the number of infected people today (a prevalence)
	 */
	long getInfectedCount();

	/**
	 * A true value:
	 *
	 * @return the number of newly infected people today
	 */
	long getIncidence();

	/**
	 * This derived value is the number of people who are currently infected at a
	 * given point in time versus all the non dead people. This is a key metric for
	 * understanding the overall scale of the outbreak and is used in various places
	 * such as to determine how many people are immune and to inform policy
	 * decisions.
	 *
	 * @return the number of people who are currently infected at a given point in
	 *         time versus all the non dead people.
	 */
	double getPrevalence();

	/**
	 * This derived value is the number of people requiring hospitalisation at a
	 * given point in time.
	 *
	 * @return Count of people newly requiring hospitalisation at any given time
	 *         point. This would be equivalent to hospital admission incidence.
	 */
	long getAdmissionIncidence();

	/**
	 * Cumulative infections is the total number of infections that have occurred
	 * since the start of the outbreak. This is a key metric for understanding the
	 * overall scale of the outbreak and is used in various places such as to
	 * determine how many people are immune and to inform policy decisions.
	 *
	 * <br>
	 * A true value
	 *
	 * @return the total number of infections that have occurred since the start of
	 *         the outbreak.
	 */
	long getCumulativeInfections();

	/**
	 * This derived value is the cumulative number of people who have required
	 * hospitalisation since the start of the outbreak.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return the cumulative number of people who have required hospitalisation
	 *         since the start of the outbreak.
	 */
	long getCumulativeAdmissions();

	/**
	 * Maximum incidence is the highest number of new infections that have occurred
	 * in a single day since the start of the outbreak. This is a key metric for
	 * understanding the peak of the outbreak and is used in various places such as
	 * to determine healthcare capacity needs and to inform policy decisions.
	 *
	 * <br>
	 * A true value:
	 *
	 * @return the highest number of new infections that have occurred in a single
	 *         day since the start of the outbreak.
	 */
	long getMaximumIncidence();

	/**
	 * Time to maximum incidence is the time it takes for the outbreak to reach its
	 * peak incidence. This is a key metric for understanding the speed of the
	 * outbreak and is used in various places such as to determine the timing of
	 * interventions and to inform policy decisions.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return the time it takes for the outbreak to reach its peak incidence.
	 */
	long getTimeToMaximumIncidence();

	/**
	 * A true value:
	 *
	 * @return count of people in hospital at any given time point. This would be
	 *         equivalent to hospital occupancy.
	 */
	long getHospitalisedCount();

	/**
	 * This derived value is the maximum number of people who have required
	 * hospitalisation at any given point in time since the start of the outbreak.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return the maximum number of people who have required hospitalisation at any
	 *         given point in time since the start of the outbreak.
	 */
	long getMaximumHospitalBurden();

	/**
	 * A true value:
	 *
	 * @return the maximum prevalence in the outbreak so far.
	 */
	double getMaximumPrevalence();

	/**
	 * Cumulative average absolute loss of mobility compared to baseline.
	 *
	 * A true value:<br>
	 *
	 * @return per person average of the cumulative absolute mobility decrease
	 *         compared to baseline. This is the sum of the total mobility decrease
	 *         for the current state and the cumulative mobility decrease from the
	 *         history. It represents the overall decrease in mobility compared to
	 *         baseline over the course of the outbreak, and is a measure of the
	 *         impact of restrictions and/or disease.
	 */
	double getCumulativeMobilityDecrease();

	/**
	 * Cumulative average absolute loss of compliance compared to baseline.
	 *
	 * A true value:<br>
	 *
	 * @return per person average of the cumulative absolute compliance decrease
	 *         compared to baseline. This is the sum of the total compliance
	 *         decrease for the current state and the cumulative compliance decrease
	 *         from the history. It represents the overall decrease in compliance
	 *         compared to baseline over the course of the outbreak, and is a
	 *         measure of the impact of restrictions and/or disease on compliance.
	 */
	double getCumulativeComplianceDecrease();
}