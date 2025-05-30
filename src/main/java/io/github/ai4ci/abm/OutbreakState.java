package io.github.ai4ci.abm;

import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Result;
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
	
	@Value.Lazy
	default double getAverageMobility() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getAdjustedMobility()).average().orElse(1);
	};

	@Value.Lazy
	default double getAverageViralLoad() {
		return ModelNav.peopleState(this).mapToDouble(p -> p.getNormalisedViralLoad()).average().orElse(1);
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
	 * today.
	 */
	@Value.Lazy
	default long getTestPositivesByResultDate() {
		return ModelNav.peopleCurrentHistory(this).mapToInt(p ->
		// If any of a persons results are positive today
		p.getResults().stream().map(t -> t.resultOnDay(this.getTime())).anyMatch(tr -> tr.equals(Result.POSITIVE)) ? 1
				: 0).sum();
	};

	/**
	 * Count of people with test negatives in their results that become available
	 * today
	 */
	@Value.Lazy
	default long getTestNegativesByResultDate() {
		return ModelNav.peopleCurrentHistory(this).mapToInt(p -> {// If any of a persons results are positive today
			if (p.getResults().isEmpty())
				return 0;
			return p.getResults().stream().map(t -> t.resultOnDay(this.getTime()))
					.allMatch(tr -> tr.equals(Result.NEGATIVE)) ? 1 : 0;
		}).sum();
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
		return ModelNav.peopleCurrentHistory(this).filter(p -> p.isIncidentHospitalisation()).count();
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
		return ModelNav.peopleState(this).filter(p -> p.isRequiringHospitalisation()).count();
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
	 * Sum of all the people who tested positive over the last infectious period.
	 * This could potentially count people multiple times if they have multiple
	 * tests. Testing protocols may prevent this from happening. This is one of the
	 * key determinants of the {@link io.github.ai4ci.abm.policy.PolicyModel}
	 * lockdown decision making.
	 */
	@Value.Lazy
	default double getPresumedTestPositivePrevalence() {
		int period = this.getPresumedInfectiousPeriod();
		long pos = ModelNav.history(this, period).mapToLong(p -> p.getTestPositivesByResultDate()).sum();
		// long neg = ModelNav.history(this, period).mapToLong(p ->
		// p.getTestNegatives()).sum();
		// return ((double) pos)/((double) pos+neg);
		return ((double) pos) / (this.getEntity().getSetupConfiguration().getNetworkSize() - this.getCumulativeDeaths());
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
		DelayDistribution dd = this.getEntity().getExecutionConfiguration().getInfectivityProfile();

		double denominator = IntStream.range(0, (int) dd.size()).mapToDouble(
				tau -> this.getEntity().getHistory(tau).map(oh -> oh.getIncidence()).orElse(0L) * dd.condDensity(tau))
				.sum();

		return denominator == 0 ? Double.NaN : ((double) numerator) / denominator;
	}

	@Value.Lazy
	default double getPrevalence() {
		return ((double) this.getInfectedCount())
				/ (ModelNav.modelSetup(this).getNetworkSize() - this.getCumulativeDeaths());
	};

	

}