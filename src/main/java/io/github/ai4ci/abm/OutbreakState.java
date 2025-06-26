package io.github.ai4ci.abm;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.function.Predicate;

import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Indication;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.policy.Trigger;
import io.github.ai4ci.util.Binomial;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.ModelNav;

/**
 * The current state of the outbreak. It contains simulation parameters that
 * change over simulation time and time varying derived summary data (marked
 * with 'Lazy' or 'Derived'). Only one state will exist at any one point in a
 * model and is replaced at each time step.
 */
@Value.Immutable
public interface OutbreakState extends OutbreakTemporalState {

	// @Value.Default default Double getViralActivityModifier() {return 1.0D;}

	/**
	 * An odds ratio describing day to day changes in the transmission due to
	 * exogenous factors such as weather, or potentially viral evolution. None of
	 * these are yet defined but {@link ModelUpdate} will be where they are
	 * implemented and configured as a function of time similar to the way
	 * {@link io.github.ai4ci.config.DemographicAdjustment} are handled (although
	 * that has an additional override that I don't think we will need here).
	 */
	double getTransmissibilityModifier();

	/**
	 * A probability that if two people are in contact and both using apps How
	 * likely is it that the app will detect the contact? The contact detected
	 * probability is a system wide parameter and describes how technically
	 * effective the app is. Then there is individual probability of app use, which
	 * covers phone actually switched on, app installed, and working:
	 * {@link PersonState#getAdjustedAppUseProbability()}
	 */
	double getContactDetectedProbability();
	
	/**
	 * A probability that any randomly selected person in the model will get a 
	 * screening test performed today.
	 */
	double getScreeningProbability();

	/**
	 * The estimate of the infectious period is important because it informs
	 * the policy and behavioural guidance on a whole range of things, from
	 * self isolation duration to maximum time that an infectious contact is 
	 * deemed relevant. A grossly mis-specified value could lead to a 
	 * decrease in effectiveness of behavioural interventions.   
	 */
	int getPresumedInfectiousPeriod();

	/** The estimate of the incubation period is important as it puts a 
	 * time on how long to isolate and when to test after exposure.
	 */
	int getPresumedIncubationPeriod();

	/**
	 * An estimate of how specific symptoms are is part of determining their
	 * significance if they are being recorded by an smart agent. 
	 */
	double getPresumedSymptomSpecificity();

	/**
	 * An estimate of how sensitive symptoms are is part of determining their
	 * significance if they are being recorded by an smart agent. 
	 */
	double getPresumedSymptomSensitivity();
	
	/** The value or statistic that is used to trigger lockdowns */
	Trigger.Value getTriggerValue();
	
	default Binomial getLockdownTrigger() {
		return this.getTriggerValue().select(this);
	};
	
	@Value.Lazy
	default double getAverageMobility() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getAdjustedMobility()).average().orElse(1);
	};

	@Value.Lazy
	default double getAverageViralLoad() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getNormalisedViralLoad()).average().orElse(1);
	};

	@Value.Lazy
	default double getAverageImmuneActivity() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getImmuneActivity()).average().orElse(1);
	};
	
	@Value.Lazy
	default double getAverageCompliance() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getAdjustedCompliance()).average().orElse(1);
	};

	@Value.Lazy
	default long getCumulativeInfections() {
		return this.getIncidence() + ModelNav.history(this).map(h -> h.getCumulativeInfections()).orElse(0L);
	}

	@Value.Lazy
	default long getMaximumIncidence() {
		return Math.max(this.getIncidence(), ModelNav.history(this).map(h -> h.getMaximumIncidence()).orElse(0L));
	};
	
	@Value.Lazy
	default long getTimeToMaximumIncidence() {
		if (this.getIncidence() > ModelNav.history(this).map(h -> h.getMaximumIncidence()).orElse(0L)) {
			return this.getTime();
		} else {
			return ModelNav.history(this).map(h -> h.getTimeToMaximumIncidence()).orElse(0L);
		}
	};

	/**
	 * Count of people with test positives in the results that become available
	 * today.The number of tests reported positive on the current simulation date.
	 * This is reported on the date the test result is available (not when the test
	 * was taken). N.B. largely only used for reporting now - questionable if useful
	 * 
	 */
	@Value.Lazy
	default long getTestPositivesByResultDate() {
		return ModelNav.peopleCurrentHistory(this).mapToInt(p ->
		// If any of a persons results are positive today
			p.getTodaysResults().stream()
				.map(t -> t.getFinalResult())
				.anyMatch(tr -> tr.equals(Result.POSITIVE)) ? 1 : 0).sum();
	};

	/**
	 * Count of people with test negatives in the results that become available
	 * today. (Does not count people with no test results today). The number of
	 * tests reported negative on the current simulation date. This is reported on
	 * the date the test result is available (not when the test was taken).
	 * N.B. largely only used for reporting now - questionable if useful
	 */
	@Value.Lazy
	default long getTestNegativesByResultDate() {
		return ModelNav.peopleCurrentHistory(this).mapToInt(p -> {// If any of a persons results are positive today
			if (p.getTodaysResults().isEmpty()) return 0;
			return p.getTodaysResults().stream()
					.map(t -> t.getFinalResult())
					.allMatch(tr -> tr.equals(Result.NEGATIVE)) ? 1 : 0;}
		).sum();
	};

	@Override
	@Value.Lazy
	default long getInfectedCount() {
		return ModelNav.peopleState(this).filter(p -> p.isInfectious()).count();
	}

	@Value.Lazy
	default double getMaximumPrevalence() {
		return Math.max(getPrevalence(), ModelNav.history(this).map(h -> h.getMaximumPrevalence()).orElse(0D));
	};

	@Value.Lazy
	default long getIncidence() {
		return ModelNav.peopleCurrentHistory(this).filter(p -> p.isIncidentInfection()).count();
	}

	@Value.Lazy
	default long getSymptomaticCount() {
		return ModelNav.peopleState(this).filter(p -> p.isSymptomatic()).count();
	}

	/**
	 * Count of people newly requiring hospitalisation at any given time point. This
	 * would be equivalent to hospital admission incidence.
	 */
	@Value.Lazy
	default long getAdmissionIncidence() {
		return ModelNav.peopleState(this).filter(p -> p.isIncidentHospitalisation()).count();
	}

	@Value.Lazy
	default long getCumulativeAdmissions() {
		return this.getAdmissionIncidence() + ModelNav.history(this).map(h -> h.getCumulativeAdmissions()).orElse(0L);
	}

	@Value.Lazy
	default long getMaximumHospitalBurden() {
		return Math.max(this.getHospitalisedCount(),
				ModelNav.history(this).map(h -> h.getMaximumHospitalBurden()).orElse(0L));
	};

	/**
	 * Average absolute loss of mobility compared to baseline. 
	 */
	@Value.Lazy
	default double getAverageMobilityDecrease() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getAbsoluteMobilityDecrease()).average().orElse(0);
	}
	
	@Value.Lazy
	default double getCumulativeMobilityDecrease() {
		return this.getAverageMobilityDecrease() + ModelNav.history(this).map(h -> h.getCumulativeMobilityDecrease()).orElse(0D);
	}
	
	/**
	 * Average absolute loss of mobility compared to baseline. 
	 */
	@Value.Lazy
	default double getAverageComplianceDecrease() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getAbsoluteComplianceDecrease()).average().orElse(0);
	}
	
	@Value.Lazy
	default double getCumulativeComplianceDecrease() {
		return this.getAverageComplianceDecrease() + ModelNav.history(this).map(h -> h.getCumulativeComplianceDecrease()).orElse(0D);
	}
	
	/**
	 * Count of people requiring hospitalisation at any given time point. This would
	 * be equivalent to hospital occupancy.
	 */
	@Value.Lazy
	default long getHospitalisedCount() {
		return this.getHospitalisationRate().getNumerator();
	}

	@Value.Lazy
	default long getCumulativeDeaths() {
		return ModelNav.peopleState(this).filter(p -> p.isDead()).count();
	}

	@Value.Lazy
	default String getPolicy() {
		return this.getEntity().getStateMachine().getState().getName();
	}

	/**
	 * All the people who tested positive over the last infectious period versus
	 * all the non dead people. This is one of the key determinants of the 
	 * {@link io.github.ai4ci.abm.policy.PolicyModel}
	 * lockdown decision making, but may be somewhat biased by symptomatic testing
	 */
	@Value.Lazy
	default Binomial getPresumedTestPositivePrevalence() {
		return getPresumedTestPositivity( t-> true, false);
		// return ((double) pos) / (this.getEntity().getSetupConfiguration().getNetworkSize() - this.getCumulativeDeaths());
	}
	
	/**
	 * All the people who had at least one positive test over the last infectious 
	 * period versus those that has tests that were all negative.
	 * This is one of the key determinants of the {@link io.github.ai4ci.abm.policy.PolicyModel}
	 * lockdown decision making, but is heavily biased by symptomatic testing
	 */
	@Value.Lazy
	default Binomial getPresumedTestPositivity() {
		return getPresumedTestPositivity( t-> true, true);
	}
		
	/**
	 * Count of people with at least one positive vs either all non dead or
	 * all people who have a test result.
	 * @param filter a predicate to select the type of test to look at (e.g. by indication
	 * or by test type)
	 * @param wesTested should the denominator include only people with tests done,
	 * or everyone?
	 */
	default Binomial getPresumedTestPositivity(Predicate<TestResult> filter, boolean wasTested) {
		return ModelNav.peopleState(this)
			.filter(p -> !p.isDead())
			.map(p -> 
				p.getStillRelevantTests()
					.filter(tr -> tr.isResultAvailable(this.getTime()))
					.filter(filter)
					.map(tr -> tr.getFinalObservedResult())
					.collect(Binomial.collectBinary())
				// result of this is the Binomial of tests for an individual
			).filter(
				// exclude people that have had no tests
				b -> wasTested ? b.getDenominator() != 0 : true
			).map(
				// any positive results will be collected in the numerator
				// This is true = any positive tests; false = no positive tests
				b -> b.getNumerator() > 0
			).collect(
				Binomial.collectBinary()
			);
	}
	
	/**
	 * All the people who had at least one positive test over the last infectious 
	 * period versus those that has tests that were all negative.
	 * This is one of the key determinants of the {@link io.github.ai4ci.abm.policy.PolicyModel}
	 * lockdown decision making, and excludes reactive / symptomatic testing
	 * so is in theory unbiased.
	 */
	@Value.Lazy
	default Binomial getScreeningTestPositivity() {
		return getPresumedTestPositivity( t-> t.getIndication().equals(Indication.SCREENING), true);
	}
	
	/**
	 * Rate of all the people who are needing hospitalisation (excluding dead) 
	 */
	@Value.Lazy
	default Binomial getHospitalisationRate() {
		return ModelNav.peopleState(this)
				.filter(p -> !p.isDead())
				.map(p -> p.isRequiringHospitalisation())
				.collect(Binomial.collectBinary());
	}
	
	

	/**
	 * An estimate of the R_t value based on the renewal equation. This is not
	 * using a realised infection network as in the present model there are 
	 * exposures but it is not explicit which exposure is actually responsible for 
	 * infection.
	 */
	default double getRtEffective() {
		// people who are newly exposed today
		long numerator = this.getIncidence();
		// people with capability to infect today. (n.b. those infected today will
		// have zero capability)
		DelayDistribution dd = this.getEntity().getBaseline().getInfectivityProfile();

		double denominator = IntStream.range(0, (int) dd.size()).mapToDouble(
				tau -> this.getEntity().getHistory(tau).map(oh -> oh.getIncidence()).orElse(0L) * dd.condDensity(tau))
				.sum();

		return denominator == 0 ? Double.NaN : ((double) numerator) / denominator;
	}

	@Value.Lazy
	default double getPrevalence() {
		return ((double) this.getInfectedCount())
				/ (this.getEntity().getPopulationSize() - this.getCumulativeDeaths());
	};

	
	@Value.Lazy
	default Map<String,Long> getBehaviourCounts() {
		return ModelNav.people(this).map(p -> p.getCurrentState()).collect(
				Collectors.groupingByConcurrent(ps -> ps.getBehaviour(), Collectors.counting())
		);
	}

	@Value.Lazy
	default Map<Long, Long> getContactCounts() {
		return ModelNav.people(this).map(p -> p.getCurrentState()).collect(
				Collectors.groupingByConcurrent(ps -> ps.getContactCount(), Collectors.counting())
		);
	}

	

	
	

}