package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.immutables.value.Value;

/**
 * Historical patient data relevant to the ongoing simulation. Some of this is copied 
 * from the patients state, and some is updated by patient behaviour. Notably
 * this is where contacts, and testing is stored as the history of both are
 * needed. The simulation stores enough history for an individual to inform the
 * current state (decided in terms of multiples of the infectious period).
 * 
 * @see PersonTemporalState
 * @see Person
 * @see Outbreak
 * 
 */
@Value.Immutable
public interface PersonHistory extends PersonTemporalState {
	
	/**
	 * The list of tests taken on this day. Initially the result will be marked as 
	 * PENDING until the test is processed. (the true result is available 
	 * internally to the model immediately, but not necessarily observed).
	 * This list is populated by the simulation engine when a person undergoes a test.
	 *
	 * @return A {@link List} of {@link TestResult} objects representing tests taken today.
	 */
	List<TestResult> getTodaysTests();
	// TestResult[] getTodaysTests(); // cannot do because builder is added to incrementally
	
	/**
	 * A list of contacts made in this time period and their weight from
	 * the perspective of transmission (i.e. context). Weights might be less if for example
	 * the participants are wearing masks. In the future we might build
	 * contact venue into this context. This array is populated by the simulation
	 * based on the social network and mobility models.
	 *
	 * @return An array of {@link Contact} objects representing contacts made today.
	 */
	Contact[] getTodaysContacts();
	
	/**
	 * A list of exposures made in this time period. These are contacts who are
	 * infectious. This array is derived from {@link #getTodaysContacts()} by filtering
	 * for contacts with infectious individuals and successful transmission events.
	 *
	 * @return An array of {@link Exposure} objects representing exposures that occurred today.
	 */
	Exposure[] getTodaysExposures();
	
	// HISTORY NAVIGATION:
	
	/**
	 * Retrieves the {@link PersonHistory} entry for the day immediately preceding the current day.
	 * This is achieved by querying the associated {@link Person} entity for its history at {@code (this.getTime() - 1)}.
	 *
	 * @return An {@link Optional} containing the {@link PersonHistory} for the previous day, or empty if not available.
	 */
	default Optional<PersonHistory> getPrevious() {
		return this.getEntity().getHistoryEntry(this.getTime()-1);
	};
	
	/**
	 * Retrieves the {@link PersonHistory} entry for the day immediately following the current day.
	 * This is achieved by querying the associated {@link Person} entity for its history at {@code (this.getTime() + 1)}.
	 *
	 * @return An {@link Optional} containing the {@link PersonHistory} for the next day, or empty if not available.
	 */
	default Optional<PersonHistory> getNext() {
		return this.getEntity().getHistoryEntry(this.getTime()+1);
	};
	
	/**
	 * Recursively collects a stream of {@link PersonHistory} entries for the current day and the {@code limit} preceding days.
	 * If {@code limit} is 0, only the current {@link PersonHistory} is returned. Otherwise, it concatenates the current
	 * history with the result of calling {@code getPrevious(limit - 1)}.
	 *
	 * @param limit The number of previous days to include in the stream (0-based, so 0 means only today).
	 * @return A {@link Stream} of {@link PersonHistory} objects, starting from the current day and going backwards.
	 */
	default Stream<PersonHistory> getPrevious(int limit) {
		if (limit == 0) return Stream.of(this); 
		return Stream.concat(
				Stream.of(this),
				this.getPrevious(limit-1));
	};
	
	/**
	 * For a newly infected person, this method identifies the {@link PersonHistory} of the individual
	 * who transmitted the infection (the "infector"). The process involves:
	 * <ol>
	 *   <li>Finding the {@link Exposure} event with the maximal viral exposure using {@link #getInfectiousContact()}.</li>
	 *   <li>Retrieving the {@link PersonHistory} of the exposer from that {@link Exposure} event.</li>
	 *   <li>Determining the {@link #getInfectionStart()} for that exposer to pinpoint the exact moment of their infection.</li>
	 * </ol>
	 * This ensures that all infections originating from a single infector map back to the same infection start time for that infector.
	 *
	 * @return An {@link Optional} containing the {@link PersonHistory} of the infector at their infection start, or empty if not found.
	 */
	@Deprecated
	@Value.Lazy default Optional<PersonHistory> getInfector() {
		return getInfectiousContact()
				.map(exposure -> exposure.getExposer(this))
				.flatMap(exposer -> exposer.getInfectionStart());
	} 
	
	/**
	 * For a newly infected person, this method finds the {@link Exposure} event that represents
	 * the maximal viral exposure on the first day before the subject became infectious.
	 * This exposure is considered the primary infectious contact from an "infector".
	 * The calculation involves:
	 * <ol>
	 *   <li>Identifying if there's a {@link #getNewInfection()} on this day.</li>
	 *   <li>If so, finding the {@link #getLastExposure()} leading up to this new infection.</li>
	 *   <li>From the {@link #getLastExposure()}, streaming through {@link #getTodaysExposures()} and finding the one
	 *       with the maximum {@link Exposure#getExposure()} value.</li>
	 * </ol>
	 *
	 * @return An {@link Optional} containing the {@link Exposure} with the maximal viral dose, or empty if not found.
	 */
	@Deprecated
	@Value.Lazy default Optional<Exposure> getInfectiousContact() {
		
		return getNewInfection().flatMap(ph -> 
			ph.getLastExposure().flatMap(e -> 
				Arrays.stream(e.getTodaysExposures())
				.max((c1,c2) -> Double.compare(c1.getExposure(), c2.getExposure())
				))
		);
	}
	
	/**
	 * If the person is currently infectious, this method finds the {@link PersonHistory} state
	 * at the point in time they first became infectious. This is determined by:
	 * <ol>
	 *   <li>Checking if the current state {@link #isInfectious()} and {@link #isIncidentInfection()}. If both are true, this is the infection start.</li>
	 *   <li>Otherwise, recursively calling {@link #getPrevious()} and checking its {@link #getInfectionStart()} until the incident infection is found.</li>
	 * </ol>
	 *
	 * @return An {@link Optional} containing the {@link PersonHistory} at the start of the infection, or empty if not infectious.
	 */
	@Value.Lazy
	@Deprecated
	default Optional<PersonHistory> getInfectionStart() {
		if (!this.isInfectious()) return Optional.empty();
		if (this.isIncidentInfection()) return Optional.of(this);
		return this.getPrevious().flatMap(p -> p.getInfectionStart());
	}
	
	/**
	 * Determines if a person became newly infectious on this specific day. This is distinct from being newly exposed.
	 * The method checks if the current {@link PersonHistory} state is an {@link #isIncidentInfection()}.
	 *
	 * @return An {@link Optional} containing the current {@link PersonHistory} if the person is newly infectious today, otherwise empty.
	 */
	@Deprecated
	@Value.Lazy default Optional<PersonHistory> getNewInfection() {
		if (isIncidentInfection()) return Optional.of(this);
		return Optional.empty();
	}
	
	/**
	 * Finds the {@link PersonHistory} state at the start of the last episode of exposure to the virus.
	 * An "episode" is defined as a continuous run of exposures separated by less than one infectious period.
	 * The method works by:
	 * <ol>
	 *   <li>Checking if the current state {@link #isIncidentExposure()}. If true, this is the last exposure start.</li>
	 *   <li>Otherwise, recursively calling {@link #getPrevious()} and checking its {@link #getLastExposure()} until the incident exposure is found.</li>
	 * </ol>
	 *
	 * @return An {@link Optional} containing the {@link PersonHistory} at the start of the last exposure episode, or empty if no recent exposure.
	 */
	default Optional<PersonHistory> getLastExposure() {
		if (this.isIncidentExposure()) return Optional.of(this);
		return this.getPrevious().flatMap(p -> p.getLastExposure());
	}
	
	/**
	 * Retrieves a stream of all {@link TestResult} objects for this patient taken within the last {@code N} days (inclusive of today).
	 * This includes tests of all statuses (e.g., PENDING, POSITIVE, NEGATIVE).
	 * The method operates by concatenating the {@link #getTodaysTests()} with the results of recursively calling
	 * {@link #getHistoricalTests(int)}-1 on the {@link #getPrevious()} day.
	 *
	 * @param limit The number of days to look back (0-based, so 0 means only today).
	 * @return A {@link Stream} of {@link TestResult} objects from the specified historical period.
	 */
	default Stream<TestResult> getHistoricalTests(int limit) {
		if (limit == 0) return this.getTodaysTests().stream();
		return Stream.concat(
			this.getTodaysTests().stream(),
			this.getPrevious().stream().flatMap(ph -> ph.getHistoricalTests(limit-1))
		);
	}
	
	/**
	 * Retrieves a stream of {@link TestResult} objects that are still considered relevant based on the presumed infectious period.
	 * This is an observed quantity, meaning it reflects tests whose results would be considered impactful given the disease's progression.
	 * The method calls {@link #getHistoricalTests(int)} with the value returned by {@link #infPeriod()} as the limit.
	 *
	 * @return A {@link Stream} of {@link TestResult} objects relevant to the current infectious period.
	 */
	default Stream<TestResult> getStillRelevantTests() {
		return this.getHistoricalTests(infPeriod());
	}
	
	/**
	 * Collects a list of {@link TestResult} objects that generate a result on the current day, regardless of when the test was taken.
	 * This method looks backwards over all possibly relevant tests (up to the infectious period).
	 * It streams through {@link #getStillRelevantTests()} and filters them using {@link TestResult#isResultToday(int)} with the current time.
	 * Note that tests with very long processing times relative to the infectious period might not be picked up.
	 *
	 * @return A {@link List} of {@link TestResult} objects whose results become available today.
	 */
	@Value.Lazy
	default List<TestResult> getTodaysResults() {
		return this.getStillRelevantTests()
			.filter(r -> r.isResultToday(this.getTime()))
			.collect(Collectors.toList());
	}
	
	/**
	 * Retrieves a stream of all {@link Contact} objects for this patient made within the last {@code X} days (inclusive of today).
	 * This is a modelled quantity, meaning it represents all contacts regardless of whether they were observed.
	 * The method operates by concatenating the {@link #getTodaysContacts()} with the results of recursively calling
	 * {@link #getHistoricalContacts(int)}-1 on the {@link #getPrevious()} day.
	 *
	 * @param limit The number of days to look back (0-based, so 0 means only today).
	 * @return A {@link Stream} of {@link Contact} objects from the specified historical period.
	 */
	default Stream<Contact> getHistoricalContacts(int limit) {
		if (limit == 0) return Arrays.stream(this.getTodaysContacts());
		return Stream.concat(
			Arrays.stream(this.getTodaysContacts()),
			this.getPrevious().stream().flatMap(ph -> ph.getHistoricalContacts(limit-1))
		);
	}
	
	/**
	 * Retrieves a stream of {@link Contact} objects that occurred within the last presumed infectious period
	 * and were detected. This is an observed quantity.
	 * The method filters the {@link #getHistoricalContacts(int)} (using {@link #infPeriod()} as the limit)
	 * to include only those contacts for which {@link Contact#isDetected()} returns true.
	 *
	 * @return A {@link Stream} of detected {@link Contact} objects relevant to the current infectious period.
	 */
	default Stream<Contact> getStillRelevantDetectedContacts() {
		return this.getHistoricalContacts(infPeriod()).filter(c -> c.isDetected());
	}
	
	/**
	 * Retrieves a stream of {@link TestResult} objects that were sampled on the current day
	 * and have their results available by a specified future {@code time}.
	 * The method calculates the difference between the specified {@code time} and the current time.
	 * If the difference is negative, an empty stream is returned. Otherwise, it filters {@link #getTodaysTests()}
	 * to include only those tests for which {@link TestResult#isResultAvailable(int)} is true at the given {@code time}.
	 *
	 * @param time The future time at which to check for result availability.
	 * @return A {@link Stream} of {@link TestResult} objects sampled today with results available by the specified time.
	 */
	default Stream<TestResult> getResultsBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return Stream.empty();
		return this.getTodaysTests().stream().filter(tr -> tr.isResultAvailable(time));
	}
	
	/**
	 * Calculates the maximum delay among all {@link TestResult} objects taken on the current day.
	 * This value is used when assembling the forward-looking list of test results, organized by sample date,
	 * including the delay distribution. The method streams through {@link #getTodaysTests()}
	 * and finds the maximum value of {@link TestResult#getDelay()}.
	 *
	 * @return The maximum test result delay in days for tests taken today, or 0 if no tests were taken.
	 */
	@Value.Derived default int getMaxDelay() {
		return (int) this.getTodaysTests().stream().mapToLong(tr -> tr.getDelay()).max().orElse(0);
	}
	
	/**
	 * Checks if a given {@code test} condition (a {@link Predicate} on {@link PersonTemporalState})
	 * has been true at least once within the last {@code limit} days, including the current day.
	 * The method works by first checking the current day. If the test is true, it returns true.
	 * Otherwise, it recursively calls {@link #isRecently(Predicate, int)} on the {@link #getPrevious()} day
	 * with a decremented limit, returning {@code false} if no previous history exists or the limit is reached.
	 *
	 * @param test The {@link Predicate} to apply to each {@link PersonTemporalState}.
	 * @param limit The number of days to look back (0-based, so 0 means only today).
	 * @return {@code true} if the test condition was met at least once in the period, {@code false} otherwise.
	 */
	default boolean isRecently(Predicate<PersonTemporalState> test, int limit) {
		if (limit == 0) return test.test(this);
		if (test.test(this)) return true;
		return getPrevious().map(p -> p.isRecently(test, limit-1)).orElse(Boolean.FALSE);
	}
	
	/**
	 * Checks if a given {@code test} condition (a {@link Predicate} on {@link PersonTemporalState})
	 * has been true continuously every day for the last {@code limit} days, including the current day.
	 * The method works by first checking the current day. If the test is false, it immediately returns false.
	 * Otherwise, it recursively calls {@link #isContinuously(Predicate, int)} on the {@link #getPrevious()} day
	 * with a decremented limit, returning {@code false} if no previous history exists or the limit is reached.
	 *
	 * @param test The {@link Predicate} to apply to each {@link PersonTemporalState}.
	 * @param limit The number of days to look back (0-based, so 0 means only today).
	 * @return {@code true} if the test condition was met continuously throughout the period, {@code false} otherwise.
	 */
	default boolean isContinuously(Predicate<PersonTemporalState> test, int limit) {
		if (limit == 0) return test.test(this);
		if (!test.test(this)) return false;
		return getPrevious().map(p -> p.isContinuously(test, limit-1)).orElse(Boolean.FALSE);
	}
	
}