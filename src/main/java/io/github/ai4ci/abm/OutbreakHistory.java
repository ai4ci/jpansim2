package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.util.ModelNav;

@Value.Immutable
public interface OutbreakHistory extends OutbreakTemporalState {
	
	Long getInfectedCount();
	Long getIncidence();
	Optional<OutbreakHistory> getPrevious();
	
	/**
	 * The number of tests reported negative on the current simulation date. This
	 * is reported on the date the test result is received. 
	 * @return
	 */
	Long getTestPositives();
	
	/**
	 * The number of tests reported negative on the current simulation date. This
	 * is reported on the date the test result is received. 
	 * @return
	 */
	Long getTestNegatives();
	
	Long getCumulativeInfections();
	
//	default Long getTestPositivesBySampleDate() {
//		return getTestPositivesBySampleDate(this.getEntity().getCurrentState().getTime());
//	}
	
	@Value.Lazy default int getMaxDelay() {
		return ModelNav.peopleHistory(this).mapToInt(ph -> ph.getMaxDelay()).max().orElse(0);
	}
	
	default int getTestPositivesBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return 0;
		return getTestPositivesBySampleDate().stream().limit(lim).reduce(Integer::sum).orElse(0);
	}
	
	default int getCurrentTestPositivesBySampleDate() {
		return this.getTestPositivesBySampleDate(this.getEntity().getCurrentState().getTime());
	}
	
	/**
	 * The number of tests reported as positive on the given day 
	 * @param time
	 * @return
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
	
	default int getTestNegativesBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return 0;
		return getTestNegativesBySampleDate().stream().limit(lim).reduce(Integer::sum).orElse(0);
	}
	
	default int getCurrentTestNegativesBySampleDate() {
		return this.getTestNegativesBySampleDate(this.getEntity().getCurrentState().getTime());
	}
	
	/**
	 * The number of tests reported as positive on the given day 
	 * @param time
	 * @return
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
	};
	
	
	

}