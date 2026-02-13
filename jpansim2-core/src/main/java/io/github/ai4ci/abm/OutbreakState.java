package io.github.ai4ci.abm;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Indication;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.policy.Trigger;
import io.github.ai4ci.util.Binomial;

/**
 * The current state of the outbreak. It contains simulation parameters that
 * change over simulation time and time varying derived summary data (marked
 * with 'Lazy' or 'Derived'). Only one state will exist at any one point in a
 * model and is replaced at each time step.
 */
@Value.Immutable
public interface OutbreakState extends OutbreakTemporalState {

	// @Value.Default default Double getViralActivityModifier() {return 1.0D;}

	@Override @Value.Lazy
	default long getAdmissionIncidence() {
		return ModelNav.peopleState(this).filter(p -> !p.isDead())
				.filter(p -> p.isIncidentHospitalisation()).count();
	}

	/**
	 * This derived value is the average compliance of the population.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return per person average compliance (as a probability)
	 */
	@Value.Lazy
	default double getAverageCompliance() {
		return ModelNav.peopleState(this)
				.mapToDouble(p -> p.getAdjustedCompliance()).average().orElse(1);
	}

	/**
	 * This derived value is the average immune activity of the population.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return per person average immune status
	 */
	@Value.Lazy
	default double getAverageImmuneActivity() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getImmuneActivity())
				.average().orElse(1);
	}

	/**
	 * This derived value is the average mobility of the population.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return per person average of the adjusted mobility
	 */
	@Value.Lazy
	default double getAverageMobility() {
		return ModelNav.peopleState(this)
				.mapToDouble(p -> p.getAdjustedMobility()).average().orElse(1);
	}

	/**
	 * This derived value is the average viral load of the population.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return per person average viral load
	 */
	@Value.Lazy
	default double getAverageViralLoad() {
		return ModelNav.peopleState(this)
				.mapToDouble(p -> p.getNormalisedViralLoad()).average().orElse(1);
	}

	/**
	 * This derived value is a multinomial count of behaviours of individual
	 * agents in the simulation. This is useful for understanding the
	 * distribution of behaviours in the population. It is a map where the keys
	 * are the behaviour categories and the values are the counts of people in
	 * each category. The behaviour categories are defined by the behaviour model
	 * and can include things like "compliant", "non-compliant", "partially
	 * compliant", etc. The behaviour model is defined by the individual agents
	 * and can change over time based on the state of the outbreak and the
	 * policies in place. This derived value can be used to track how behaviour
	 * changes over time and how it correlates with the course of the outbreak.
	 *
	 * A true value:<br>
	 *
	 * @return A multi-nomial count of behaviours of individual agents in the
	 *         simulation
	 */
	@Value.Lazy
	default Map<String, Long> getBehaviourCounts() {
		return ModelNav.people(this).map(p -> p.getCurrentState()).collect(
				Collectors.groupingByConcurrent(
						ps -> ps.getBehaviour(), Collectors.counting()
				)
		);
	}

	/**
	 * This derived value is a multinomial count of the number of contacts people
	 * have in the simulation. This is useful for understanding the distribution
	 * of contact patterns in the population. It is a map where the keys are the
	 * number of contacts and the values are the counts of people with that
	 * number of contacts. The contact patterns can be influenced by the
	 * behaviour model, policies in place, and the state of the outbreak. This
	 * derived value can be used to track how contact patterns change over time
	 * and how they correlate with the course of the outbreak.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return A multinomial count of people in the simulation who have had a
	 *         given number of contacts that day.
	 */
	@Value.Lazy
	default Map<Long, Long> getContactCounts() {
		return ModelNav.people(this).map(p -> p.getCurrentState()).collect(
				Collectors.groupingByConcurrent(
						ps -> ps.getContactCount(), Collectors.counting()
				)
		);
	}

	/**
	 * The contact detected probability is a key parameter for the effectiveness
	 * of contact tracing apps. It represents the technical effectiveness of the
	 * app but not the actual use of the app by individuals.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return A probability that if two people are in contact and both using
	 *         apps then how likely is it that the app will detect the contact?
	 *         The contact detected probability is a system wide parameter and
	 *         describes how technically effective the app is. The individual
	 *         probability of app use, which covers phone actually switched on,
	 *         app installed, and working is captured by
	 *         {@link PersonState#getAdjustedAppUseProbability()}
	 */
	double getContactDetectedProbability();

	@Override @Value.Lazy
	default long getCumulativeAdmissions() {
		return this.getAdmissionIncidence() + ModelNav.history(this)
				.map(h -> h.getCumulativeAdmissions()).orElse(0L);
	}

	@Override @Value.Lazy
	default double getCumulativeComplianceDecrease() {
		return this.getTotalComplianceDecrease() + ModelNav.history(this)
				.map(h -> h.getCumulativeComplianceDecrease()).orElse(0D);
	}

	/**
	 * This derived value is the cumulative number of people who have died since
	 * the start of the outbreak.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return the cumulative number of people who have died since the start of
	 *         the outbreak.
	 */
	@Value.Lazy
	default long getCumulativeDeaths() {
		return ModelNav.peopleState(this).filter(p -> p.isDead()).count();
	}

	@Override @Value.Lazy
	default long getCumulativeInfections() {
		return this.getIncidence() + ModelNav.history(this)
				.map(h -> h.getCumulativeInfections()).orElse(0L);
	}

	@Override @Value.Lazy
	default double getCumulativeMobilityDecrease() {
		return this.getTotalMobilityDecrease() + ModelNav.history(this)
				.map(h -> h.getCumulativeMobilityDecrease()).orElse(0D);
	}

	/**
	 * This derived value is the number of people who are currently hospitalised
	 * at a given point in time versus all the non dead people. This is a key
	 * metric for understanding the burden on the healthcare system and is used
	 * in various places such as to determine healthcare capacity needs and to
	 * inform policy decisions.
	 *
	 * @return Rate of all the people who are needing hospitalisation (excluding
	 *         dead)
	 */
	@Value.Lazy
	default Binomial getHospitalisationRate() {
		return ModelNav.peopleState(this).filter(p -> !p.isDead())
				.map(p -> p.isRequiringHospitalisation())
				.collect(Binomial.collectBinary());
	}

	/**
	 * This derived value is the number of people who are currently hospitalised
	 * at a given point in time.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return the number of people who are currently hospitalised at a given
	 *         point in time.
	 */
	@Override @Value.Lazy
	default long getHospitalisedCount() {
		return this.getHospitalisationRate().getNumerator();
	}

	@Override @Value.Lazy
	default long getIncidence() {
		return ModelNav.peopleCurrentHistory(this)
				.filter(p -> p.isIncidentInfection()).count();
	}

	@Override @Value.Lazy
	default long getInfectedCount() {
		return ModelNav.peopleState(this).filter(p -> !p.isDead())
				.filter(p -> p.isInfectious()).count();
	}

	/**
	 * An observed value:<br>
	 * <br>
	 * The value of the lockdown trigger metric at this moment in this outbreak.
	 *
	 * @return a numerator denominator pair.
	 */
	default Binomial getLockdownTrigger() {
		return this.getTriggerValue().select(this);
	}

	@Override @Value.Lazy
	default long getMaximumHospitalBurden() {
		return Math.max(
				this.getHospitalisedCount(),
				ModelNav.history(this).map(h -> h.getMaximumHospitalBurden())
						.orElse(0L)
		);
	}

	@Override @Value.Lazy
	default long getMaximumIncidence() {
		return Math.max(
				this.getIncidence(),
				ModelNav.history(this).map(h -> h.getMaximumIncidence()).orElse(0L)
		);
	}

	@Override @Value.Lazy
	default double getMaximumPrevalence() {
		return Math.max(
				this.getPrevalence(),
				ModelNav.history(this).map(h -> h.getMaximumPrevalence()).orElse(0D)
		);
	}

	/**
	 * The current policy state for this moment in the outbreak. This is derived
	 * from the state machine of the outbreak.
	 *
	 * @return the current policy state for this moment in the outbreak.
	 */
	@Value.Lazy
	default String getPolicy() {
		return this.getEntity().getStateMachine().getState().getName();
	}

	/**
	 * The estimate of the incubation period is important as it puts a time on
	 * how long to isolate and when to test after exposure.
	 *
	 * @return an estimate of the incubation period in days.
	 */
	int getPresumedIncubationPeriod();

	/**
	 * The current best estimate of how long an infectious person is infectious
	 * for.
	 *
	 * The estimate of the infectious period is important because it informs the
	 * policy and behavioural guidance on a whole range of things, from self
	 * isolation duration to maximum time that an infectious contact is deemed
	 * relevant. A grossly mis-specified value could lead to a decrease in
	 * effectiveness of behavioural interventions. Assumptions around this define
	 * how long policy models suggest self isolation.
	 *
	 * @return an estimate of the infectious period in days.
	 */
	int getPresumedInfectiousPeriod();

	/**
	 * An estimate of how sensitive symptoms are is part of determining their
	 * significance if they are being recorded by an smart agent.
	 *
	 * @return an estimate of the symptom sensitivity as a probability.
	 */
	double getPresumedSymptomSensitivity();

	/**
	 * An estimate of how specific symptoms are is part of determining their
	 * significance if they are being recorded by an smart agent.
	 *
	 * @return an estimate of the symptom specificity as a probability.
	 */
	double getPresumedSymptomSpecificity();

	/**
	 * This derived value is the number of people who have had at least one
	 * positive test over the last infectious period versus all the non dead
	 * people who have had at least one test result. This is one of the key
	 * determinants of the {@link io.github.ai4ci.abm.policy.PolicyModel}
	 * lockdown decision making, but is heavily biased by symptomatic testing.
	 *
	 * @return All the people who tested positive over the last infectious period
	 *         versus all the non dead people. This is one of the key
	 *         determinants of the {@link io.github.ai4ci.abm.policy.PolicyModel}
	 *         lockdown decision making, but may be somewhat biased by
	 *         symptomatic testing
	 */
	@Value.Lazy
	default Binomial getPresumedTestPositivePrevalence() {
		return this.getPresumedTestPositivity(t -> true, false);
		// return ((double) pos) /
		// (this.getEntity().getSetupConfiguration().getNetworkSize() -
		// this.getCumulativeDeaths());
	}

	/**
	 * This derived value is the number of people who have had at least one
	 * positive test over the last infectious period versus all the people who
	 * have had at least one test result. This is one of the key determinants of
	 * the {@link io.github.ai4ci.abm.policy.PolicyModel} lockdown decision
	 * making, but is heavily biased by symptomatic testing.
	 *
	 * @return All the people who had at least one positive test over the last
	 *         infectious period versus those that has tests that were all
	 *         negative. This is one of the key determinants of the
	 *         {@link io.github.ai4ci.abm.policy.PolicyModel} lockdown decision
	 *         making, but is heavily biased by symptomatic testing
	 */
	@Value.Lazy
	default Binomial getPresumedTestPositivity() {
		return this.getPresumedTestPositivity(t -> true, true);
	}

	/**
	 * Count of people with at least one positive vs either all non dead or all
	 * people who have a test result.
	 *
	 * @param filter    a predicate to select the type of test to look at (e.g.
	 *                  by indication or by test type)
	 * @param wasTested should the denominator include only people with tests
	 *                  done, or everyone?
	 * @return All the people who had at least one positive test over the last
	 *         infectious period versus either all the non dead people or all the
	 *         people who have had at least one test result. This is one of the
	 *         key determinants of the
	 *         {@link io.github.ai4ci.abm.policy.PolicyModel} lockdown decision
	 *         making, but is heavily biased by symptomatic testing
	 *
	 */
	default Binomial getPresumedTestPositivity(
			Predicate<TestResult> filter, boolean wasTested
	) {
		return ModelNav.peopleState(this).filter(p -> !p.isDead())
				.map(
						p -> p.getStillRelevantTests()
								.filter(tr -> tr.isResultAvailable(this.getTime()))
								.filter(filter).map(tr -> tr.getFinalObservedResult())
								.collect(Binomial.collectBinary())
						// result of this is the Binomial of tests for an individual
				).filter(
						// exclude people that have had no tests
						b -> wasTested ? b.getDenominator() != 0 : true
				).map(
						// any positive results will be collected in the numerator
						// This is true = any positive tests; false = no positive
						// tests
						b -> b.getNumerator() > 0
				).collect(Binomial.collectBinary());
	}

	@Override @Value.Lazy
	default double getPrevalence() {
		return ((double) this.getInfectedCount())
				/ (this.getEntity().getPopulationSize()
						- this.getCumulativeDeaths());
	}

	/**
	 * This derived value is an estimate of the R_t value based on the renewal
	 * equation. This is not using a realised infection network as in the present
	 * model there are exposures but it is not explicit which exposure is
	 * actually responsible for infection. It is based on the number of people
	 * who are newly exposed today (the numerator) and the number of people with
	 * capability to infect today (the denominator). The denominator is
	 * calculated using the infectivity profile from the baseline configuration
	 * and the history of incidence.
	 *
	 * @return An estimate of the R_t value based on the renewal equation. This
	 *         is not using a realised infection network as in the present model
	 *         there are exposures but it is not explicit which exposure is
	 *         actually responsible for infection.
	 */
	default double getRtEffective() {
		// people who are newly exposed today
		var numerator = this.getIncidence();
		// people with capability to infect today. (n.b. those infected today will
		// have zero capability)
		var dd = this.getEntity().getBaseline().getInfectivityProfile();

		var denominator = IntStream.range(0, (int) dd.size()).mapToDouble(
				tau -> this.getEntity().getHistory(tau).map(oh -> oh.getIncidence())
						.orElse(0L) * dd.condDensity(tau)
		).sum();

		return denominator == 0 ? Double.NaN : (numerator) / denominator;
	}

	/**
	 * The screening probability is the probability that any randomly selected
	 * person in the model will get a screening test performed today. This
	 * represents mass screening or asymptomatic testing programmes which inform
	 * policy decisions and can give less biased estimates of prevalence than
	 * symptomatic testing.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return A probability that any randomly selected person in the model will
	 *         get a screening test performed today.
	 */
	double getScreeningProbability();

	/**
	 * This derived value is the number of people who have had at least one
	 * positive test over the last infectious period versus all the people who
	 * have had at least one test result for screening tests only. This is one of
	 * the key determinants of the {@link io.github.ai4ci.abm.policy.PolicyModel}
	 * lockdown decision making, but is less biased by symptomatic testing as it
	 * only includes screening tests.
	 *
	 * @return All the people who had at least one positive test over the last
	 *         infectious period versus those that has tests that were all
	 *         negative. This is one of the key determinants of the
	 *         {@link io.github.ai4ci.abm.policy.PolicyModel} lockdown decision
	 *         making, and excludes reactive / symptomatic testing so is in
	 *         theory unbiased.
	 */
	@Value.Lazy
	default Binomial getScreeningTestPositivity() {
		return this.getPresumedTestPositivity(
				t -> t.getIndication().equals(Indication.SCREENING), true
		);
	}

	/**
	 * This derived value is the number of people experiencing symptoms at a
	 * given point in time.
	 *
	 * <p>
	 * A true value
	 *
	 * @return the number of people experiencing symptoms at a given point in
	 *         time
	 */
	@Value.Lazy
	default long getSymptomaticCount() {
		return ModelNav.peopleState(this).filter(p -> !p.isDead())
				.filter(p -> p.isSymptomatic()).count();
	}

	/**
	 * This derived value is the number of people with test negatives in the
	 * results that become available
	 *
	 * A true but also observed value (assuming no additional reporting delay).
	 * This does not account for the nature of the test.
	 *
	 * @return count of people with test negatives in the results that become
	 *         available today. (Does not count people with no test results
	 *         today). The number of tests reported negative on the current
	 *         simulation date. This is reported on the date the test result is
	 *         available (not when the test was taken). N.B. largely only used
	 *         for reporting now
	 */
	@Value.Lazy
	default long getTestNegativesByResultDate() {
		return ModelNav.peopleCurrentHistory(this).mapToInt(p -> {// If any of a
																						// persons
																						// results are
																						// positive
																						// today
			if (p.getTodaysResults().isEmpty()) return 0;
			return p.getTodaysResults().stream().map(t -> t.getFinalResult())
					.allMatch(tr -> tr.equals(Result.NEGATIVE)) ? 1 : 0;
		}).sum();
	}

	/**
	 * This derived value is the number of people with test positives in the
	 * results that become available
	 *
	 * A true but also observed value (assuming no additional reporting delay).
	 * This does not account for the nature of the test.
	 *
	 * @return Count of people with test positives in the results that become
	 *         available today.The number of tests reported positive on the
	 *         current simulation date. This is reported on the date the test
	 *         result is available (not when the test was taken). N.B. largely
	 *         only used for reporting now
	 */
	@Value.Lazy
	default long getTestPositivesByResultDate() {
		return ModelNav.peopleCurrentHistory(this).mapToInt(p ->
		// If any of a persons results are positive today
		p.getTodaysResults().stream().map(t -> t.getFinalResult())
				.anyMatch(tr -> tr.equals(Result.POSITIVE)) ? 1 : 0
		).sum();
	}

	@Override @Value.Lazy
	default long getTimeToMaximumIncidence() {
		if (this.getIncidence() > ModelNav.history(this)
				.map(h -> h.getMaximumIncidence()).orElse(0L))
			return this.getTime();
		return ModelNav.history(this).map(h -> h.getTimeToMaximumIncidence())
				.orElse(0L);
	}

	/**
	 * Average absolute loss of compliance compared to baseline.
	 *
	 * A true value:<br>
	 *
	 * @return per person average of the absolute compliance decrease compared to
	 *         baseline. This is the average of the absolute value of the
	 *         difference between the current compliance and the baseline
	 *         compliance for each person in the population.
	 *
	 */
	@Value.Lazy
	default double getTotalComplianceDecrease() {
		return ModelNav.peopleState(this).filter(state -> !state.isDead())
				.mapToDouble(p -> p.getAbsoluteComplianceDecrease()).sum();
	}

	/**
	 * Average absolute loss of mobility compared to baseline.
	 *
	 * A true value:<br>
	 *
	 * @return per person average of the absolute mobility decrease compared to
	 *         baseline. This is the average of the absolute value of the
	 *         difference between the current mobility and the baseline mobility
	 *         for each person in the population. It is a measure of how much
	 *         mobility has decreased compared to the baseline, regardless of
	 *         whether it is due to increased or decreased mobility.
	 *
	 */
	@Value.Lazy
	default double getTotalMobilityDecrease() {
		return ModelNav.peopleState(this).filter(state -> !state.isDead())
				.mapToDouble(p -> p.getAbsoluteMobilityDecrease()).sum();
	}

	/**
	 * The transmissibility modifier is a catch all parameter that can be used to
	 * capture changes in transmission that are not captured by the other
	 * parameters such as mobility, compliance, contact detection probability or
	 * viral load. It is intended to capture things like weather effects, or
	 * viral evolution that may change the transmissibility of the virus over
	 * time.
	 *
	 * A true value:<br>
	 * <br>
	 *
	 * @return An odds ratio describing day to day changes in the transmission
	 *         due to exogenous factors such as weather, or potentially viral
	 *         evolution. None of these are yet defined but {@link ModelUpdate}
	 *         will be where they are implemented and configured as a function of
	 *         time similar to the way
	 *         {@link io.github.ai4ci.config.execution.DemographicAdjustment} are
	 *         handled (although that has an additional override that I don't
	 *         think we will need here).
	 */
	double getTransmissibilityModifier();

	/**
	 * The value or statistic that is used to trigger lockdowns. There are
	 * various metrics that could trigger a policy change such as absolute case
	 * count, or test positivity rate
	 *
	 * @see io.github.ai4ci.abm.policy.Trigger.Value
	 *
	 * @return the value or statistic that is used to trigger lockdowns
	 */
	Trigger.Value getTriggerValue();

}