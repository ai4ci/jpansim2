package io.github.ai4ci.abm;

import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.ModelNav;

@Value.Immutable
public interface OutbreakState extends OutbreakTemporalState {

	
	// @Value.Default default Double getViralActivityModifier() {return 1.0D;}
	
	/**
	 * An odds ratio describing day to day changes in the transmission due
	 * to exogenous factors such as weather, or potentially viral evolution.  
	 * @return
	 */
	Double getTransmissibilityModifier();
	
	Integer getPresumedInfectiousPeriod();
	Double getPresumedSymptomSpecificity();
	Double getPresumedSymptomSensitivity();
	

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
	
	@Value.Lazy default long getCumulativeInfections() {
		return this.getIncidence() + ModelNav.history(this).map(h -> h.getCumulativeInfections()).orElse(0L);
	}
	
	/**
	 * Count of people with test positives in the results that become available today
	 * @return
	 */
	@Value.Lazy default long getTestPositives() {
		return ModelNav.peopleState(this)
				.mapToInt(p ->
					// If any of a persons results are positive today
					p.getResults().stream()
						.map(t -> t.resultOnDay(this.getTime()))
						.anyMatch(tr -> tr.equals(Result.POSITIVE))
						 ? 1 : 0
				)
				.sum();
	};
	
	/**
	 * Count of people with test negatives in their results that become available today
	 * @return
	 */
	@Value.Lazy default long getTestNegatives() {
		return ModelNav.peopleState(this)
				.mapToInt(p -> {// If any of a persons results are positive today
					if (p.getResults().isEmpty()) return 0;
					return p.getResults().stream()
						.map(t -> t.resultOnDay(this.getTime()))
						.allMatch(tr -> tr.equals(Result.NEGATIVE))
						 ? 1 : 0;
				}).sum();
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
	
	
	// This is a forward looking R number based on the day at which someone
	// becomes infectious.
//	default List<Double> getRtForward() {
//		int size = this.getTime()+1;
//		List<MutablePair<Double,Double>> ts = new ArrayList<>(size);
//		for (int i=0;i<size;i++) ts.add(i, MutablePair.of(0D,0D));
//		NetworkBuilder<PersonHistory, Infection> infections = this.getEntity().getInfections();
//		infections.vertexSet().forEach(v -> {
//			int infTime = v.getTime(); 
//			MutablePair<Double,Double> tmp = ts.get(infTime);
//			tmp.setLeft(tmp.getLeft()+infections.outDegreeOf(v));
//			tmp.setRight(tmp.getRight()+1);
//		});
//		return ts.stream().map(p -> p.getLeft()/p.getRight()).collect(Collectors.toList());
//	}
	
	default Double getRtEffective() {
		// people who are newly exposed today
		long numerator = this.getIncidence();
		// people with capability to infect today. (n.b. those infected today will
		// have zero capability)
		DelayDistribution dd = this.getEntity().getExecutionConfiguration().getInfectivityProfile();
		
		double denominator = IntStream.range(0, (int) dd.size()).mapToDouble(tau -> 
			this.getEntity().getHistory(tau).map(oh -> oh.getIncidence()).orElse(0L) * dd.condDensity(tau)
		).sum();
		
		return denominator == 0 ? Double.NaN : ((double) numerator)/denominator;
	}
	
//	default Double getRtEffective() {
//		// people who are newly exposed today
//		double numerator = this.getEntity().getPeople().stream()
//				.filter(p -> p.getCurrentHistory().map(ph -> ph.isIncidentInfection()).orElse(Boolean.FALSE))
//				.mapToDouble(p -> p.getCurrentState().getContactExposure())
//				.sum();
//		
//		double denominator = this.getEntity().getPeople().stream()
//				.mapToDouble(p -> p.getCurrentState().getNormalisedViralLoad())
//				.sum();
//		
//		return denominator == 0 ? Double.NaN : ((double) numerator)/denominator;
//	}
	
}