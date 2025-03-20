package io.github.ai4ci.abm;

import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ModelNav;

@Value.Immutable
public interface OutbreakState extends OutbreakTemporalState {

	@Value.Default default Double getViralActivityModifier() {return 1.0D;}
	@Value.Default default Integer getPresumedInfectiousPeriod() {return 10;}
	@Value.Default default double getPresumedSymptomSpecificity() {return 0.9;}
	@Value.Default default double getPresumedSymptomSensitivity() {return 0.5;}

	@Value.Lazy 
	default double getAverageMobility() {
		return ModelNav.peopleState(this)
				.mapToDouble(p -> p.getAdjustedMobility())
				.average().orElse(1);
	};
	
	@Value.Lazy 
	default double getAverageViralLoad() {
		return ModelNav.peopleState(this)
				.mapToDouble(p -> p.getNormalisedViralLoad())
				.average().orElse(1);
	};
	
	@Value.Lazy 
	default double getAverageCompliance() {
		return ModelNav.peopleState(this)
				.mapToDouble(p -> p.getAdjustedCompliance())
				.average().orElse(1);
	};
	
	/**
	 * Count of people with test positives in the results that become available today
	 * @return
	 */
	@Value.Lazy default long getTestPositives() {
		return ModelNav.peopleState(this)
				.mapToInt(p ->
					// If any of a persons results are positive today
					p.getResults().stream()
						.filter(t -> t.resultOnDay(this.getTime()).equals(Result.POSITIVE))
						.findAny().isPresent() ? 1 : 0
				)
				.sum();
	};
	
	/**
	 * Count of people with test negatives in their results that become available today
	 * @return
	 */
	@Value.Lazy default long getTestNegatives() {
		return ModelNav.peopleState(this)
				.mapToInt(p -> // If any of a persons results are positive today
					p.getResults().stream()
						.filter(t -> t.resultOnDay(this.getTime()).equals(Result.NEGATIVE))
						.findAny().isPresent() ? 1 : 0
				).sum();
	};
	
	@Override
	@Value.Lazy 
	default Long getInfectedCount() {
		return ModelNav.peopleState(this)
				.filter(p -> p.isInfectious()).count();
	}
	
	@Value.Lazy 
	default Long getIncidence() {
		return ModelNav.peopleCurrentHistory(this)
				.filter(p -> p.isIncidentInfection()).count();
	}
	
	@Value.Lazy 
	default Long getSymptomaticCount() {
		return ModelNav.peopleState(this)
				.filter(p -> p.isSymptomatic()).count();
	}
	
	@Value.Lazy default String getPolicy() {
		return this.getEntity().getStateMachine().getState().getName();
	}
	
	/**
	 * Sum of all the people who tested positive over the last infectious 
	 * period. This could potentially count people multiple times if they 
	 * have multiple tests. Testing protocols may prevent this from happening
	 * @return
	 */
	@Value.Derived default double getPresumedTestPositivePrevalence() {
		int period = this.getPresumedInfectiousPeriod();
		long pos = ModelNav.history(this, period).mapToLong(p -> p.getTestPositives()).sum();
		// long neg = ModelNav.history(this, period).mapToLong(p -> p.getTestNegatives()).sum();
		// return ((double) pos)/((double) pos+neg);
		return ((double) pos)/this.getEntity().getSetupConfiguration().getNetworkSize();
	}
	
	
	
	
	
}