package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.immutables.value.Value;

/**
 * The historical state of the whole model. This is typically used to collect
 * summary statistics for output from the {@link OutbreakState}, but also can
 * play a role in informing the {@link io.github.ai4ci.abm.policy.PolicyModel}
 *
 * OutbreakHistory items are generated from OutbreakState items using the
 * {@link HistoryMapper} during the update cycle.
 */
@Value.Immutable
public interface OutbreakHistory extends OutbreakTemporalState {

	/**
	 *
	 * The current state of knowledge about people with test negative results
	 * from this day (which is potentially in the past) as assessed at current
	 * simulation time. <br>
	 * An observed value:
	 *
	 * @return the number of people with all negative test results reported for
	 *         tests taken on this day, as observed at the current simulation
	 *         time.
	 */
	default int getCurrentTestNegativesBySampleDate() {
		return this.getTestNegativesBySampleDate(
				this.getEntity().getCurrentState().getTime()
		);
	}

	/**
	 * An observed value:<br>
	 * <br>
	 * The current state of knowledge about test positive results from samples
	 * taken on this day (potentially in the past). This is output as part of the
	 * delay distribution of tests, as it gives a positive test count for a given
	 * day that increases in the future. (N.B. This looks over complicated and I
	 * question its utility, it is also misleading as it is a count of people
	 * with any positive test)
	 *
	 * @return the number of people with any positive test result reported for
	 *         tests taken on this day, as observed at the current simulation
	 *         time.
	 */
	default int getCurrentTestPositivesBySampleDate() {
		return this.getTestPositivesBySampleDate(
				this.getEntity().getCurrentState().getTime()
		);
	}

	/**
	 * Internal helper function for looking at delay of test results
	 *
	 * @return the maximum delay of test results for any person in the model, as
	 *         observed in the history. This is used to determine how far forward
	 *         to look when calculating test positive/negative counts by sample
	 *         date.
	 */
	@Value.Lazy
	default int getMaxDelay() {
		return ModelNav.peopleHistory(this).mapToInt(ph -> ph.getMaxDelay()).max()
				.orElse(0);
	}

	/**
	 * The previous entry in the history of this outbreak.
	 *
	 * @return An optional containing the previous history entry, or empty if
	 *         this is the first entry (time=0).
	 */
	default Optional<OutbreakHistory> getPrevious() {
		return this.getEntity().getHistoryEntry(this.getTime() - 1);
	}

	/**
	 *
	 * The number of people with all tests reported as negative on the given day
	 * <br>
	 * An observed value:
	 *
	 * @see #getTestPositivesBySampleDate() for the logic here.
	 *
	 * @return a list of counts of people with all negative test results reported
	 *         retrospective to today.
	 */
	@Value.Lazy
	default List<Integer> getTestNegativesBySampleDate() {
		return IntStream.range(0, this.getMaxDelay()).mapToObj(delay -> {
			return ModelNav.peopleHistory(this).mapToInt(p -> {
				if (p.getTodaysTests().isEmpty()) { return 0; }
				// If any of a persons results are positive today
				return p.getResultsBySampleDate(this.getTime() + delay)
						.allMatch(t -> !t.getFinalObservedResult()) ? 1 : 0;
			}).sum();
		}).collect(Collectors.toList());
	}

	/**
	 * The total test negatives reported from this day, as reported on another
	 * day (typically in the future). This handles delay distribution of test
	 * results and will tell us what is known about tests taken today on day x.
	 * All tests taken on this day must be negative for the person to count as a
	 * test negative.
	 *
	 * An observed value:<br>
	 *
	 * @param time the simulation time at which to assess the test negative
	 *             counts for
	 * @return the number of people with all negative test results reported for
	 *         tests
	 *
	 */
	default int getTestNegativesBySampleDate(int time) {
		var lim = time - this.getTime();
		if (lim < 0) { return 0; }
		return this.getTestNegativesBySampleDate().stream().limit(lim)
				.reduce(Integer::sum).orElse(0);
	}

	/**
	 * An observed value:<br>
	 * <br>
	 * The number of people with tests reported as positive on the day of
	 * testing. looking forward through time until the result is available. The
	 * result is a forward distribution of positive test counts indexed by delay.
	 * So first is the tests with positive results available today, second is
	 * those with results tomorrow etc.
	 *
	 * @return a list of counts of people with any positive test result reported
	 *         retrospective to today.
	 */
	@Value.Lazy
	default List<Integer> getTestPositivesBySampleDate() {
		return IntStream.range(0, this.getMaxDelay()).mapToObj(delay -> {
			return ModelNav.peopleHistory(this).mapToInt(p ->
			// If any of a persons results are positive today
			p.getResultsBySampleDate(this.getTime() + delay)
					.anyMatch(t -> t.getFinalObservedResult()) ? 1 : 0
			).sum();
		}).collect(Collectors.toList());
	}

	/**
	 * An observed value:<br>
	 * <br>
	 * The total test positive people reported from this day, as reported on
	 * another day (typically in the future). This handles delay distribution of
	 * test results and will tell us what is known about tests taken today on day
	 * X. Any positive value is counted as a test positive person on this day.
	 *
	 * @param time the simulation time at which to assess the test positive
	 *             counts for
	 * @return the number of people with any positive test result reported for
	 *         tests taken on this day, as observed at the given time.
	 */
	default int getTestPositivesBySampleDate(int time) {
		var lim = time - this.getTime();
		if (lim < 0) { return 0; }
		return this.getTestPositivesBySampleDate().stream().limit(lim)
				.reduce(Integer::sum).orElse(0);
	}

}