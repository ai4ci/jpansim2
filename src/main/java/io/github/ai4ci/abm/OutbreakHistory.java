package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.util.ModelNav;

@Value.Immutable
/** 
 * The historical state of the whole model. This is typically used to collect
 * summary statistics for output from the {@link OutbreakState}, but also can
 * play a role in informing the {@link io.github.ai4ci.abm.policy.PolicyModel} 
 */
public interface OutbreakHistory extends OutbreakTemporalState {
	
	default Optional<OutbreakHistory> getPrevious() {
		return this.getEntity().getHistoryEntry(this.getTime()-1);
	};
	
	/** Internal helper function for looking at delay of test results */
	@Value.Lazy default int getMaxDelay() {
		return ModelNav.peopleHistory(this).mapToInt(ph -> ph.getMaxDelay()).max().orElse(0);
	}
	
	/** The total test positives reported from this day, as reported on another
	 *  day (typically in the future). This handles delay distribution of 
	 *  test results and will tell us what is known about tests taken today on 
	 *  day x.
	 */
	default int getTestPositivesBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return 0;
		return getTestPositivesBySampleDate().stream().limit(lim).reduce(Integer::sum).orElse(0);
	}
	
	/** The current state of knowledge about test positive results from samples
	 * taken on this day */
	default int getCurrentTestPositivesBySampleDate() {
		return this.getTestPositivesBySampleDate(this.getEntity().getCurrentState().getTime());
	}
	
	/**
	 * The number of tests reported as positive on the day of testing is looking
	 * forward through time until the result is available. The result is a forward
	 * distribution of positive test counts indexed by delay. So first is the
	 * tests with positive results available today, second is those with results 
	 * tomorrow etc.  
	 */
	@Value.Lazy default List<Integer> getTestPositivesBySampleDate() {
		return 
			IntStream.range(0, getMaxDelay()).mapToObj(delay -> {
				return ModelNav.peopleHistory(this)
					.mapToInt(p ->
						// If any of a persons results are positive today
						p.getResultsBySampleDate(this.getTime()+delay)
						.anyMatch(t -> t.getFinalObservedResult())
						? 1 : 0
					).sum();
			}).collect(Collectors.toList())
			;
	};
	
	/** The total test negatives reported from this day, as reported on another
	 *  day (typically in the future). This handles delay distribution of 
	 *  test results and will tell us what is known about tests taken today on 
	 *  day x.
	 */
	default int getTestNegativesBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return 0;
		return getTestNegativesBySampleDate().stream().limit(lim).reduce(Integer::sum).orElse(0);
	}
	
	/** The current state of knowledge about test negative results from this 
	 * day */
	default int getCurrentTestNegativesBySampleDate() {
		return this.getTestNegativesBySampleDate(this.getEntity().getCurrentState().getTime());
	}
	
	/**
	 * The number of tests reported as positive on the given day 
	 * @see #getTestPositivesBySampleDate() for the logic here.
	 */
	@Value.Lazy default List<Integer> getTestNegativesBySampleDate() {
		return 
			IntStream.range(0, getMaxDelay()).mapToObj(delay -> {
				return ModelNav.peopleHistory(this)
					.mapToInt(p -> {
						if (p.getTodaysTests().isEmpty()) return 0;
					// If any of a persons results are positive today
						return p.getResultsBySampleDate(this.getTime()+delay)
						.allMatch(t -> !t.getFinalObservedResult())
						? 1 : 0;
					}).sum();
			}).collect(Collectors.toList())
			;
	}
}