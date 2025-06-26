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
 * current state (in terms of 
 */
@Value.Immutable
public interface PersonHistory extends PersonTemporalState {
	
	/**
	 * The list of tests taken on this day. Initially the result will be marked as 
	 * PENDING until the test is processed. (the true result is available 
	 * internally to the model immediately, but not necessarily observed).
	 */
	List<TestResult> getTodaysTests();
	// TestResult[] getTodaysTests(); // cannot do because builder is added to incrementally
	
	/**
	 * A list of contacts made in this time period and their weight from
	 * the perspective of transmission (i.e. context). Weights might be less if for example
	 * the participants are wearing masks. In the future we might build
	 * contact venue into this context.
	 * @return
	 */
	Contact[] getTodaysContacts();
	
	/**
	 * A list of exposures made in this time period. These are contacts who are
	 * infectious.
	 */
	Exposure[] getTodaysExposures();
	
	// HISTORY NAVIGATION:
	
	default Optional<PersonHistory> getPrevious() {
		return this.getEntity().getHistoryEntry(this.getTime()-1);
	};
	
	default Optional<PersonHistory> getNext() {
		return this.getEntity().getHistoryEntry(this.getTime()+1);
	};
	
	default Stream<PersonHistory> getPrevious(int limit) {
		if (limit == 0) return Stream.of(this); 
		return Stream.concat(
				Stream.of(this),
				this.getPrevious(limit-1));
	};
	
	/**
	 * For a new infection this finds the contact with maximal viral exposure 
	 * on the first day before the subject is infectious when the subject was 
	 * exposed by an "infector". From this "infector" the time of their 
	 * infection is identified so that all the infections from one infector 
	 * should map back to the same instant in time for that infector.
	 * @return
	 */
	@Deprecated
	@Value.Lazy default Optional<PersonHistory> getInfector() {
		return getInfectiousContact()
				.map(exposure -> exposure.getExposer(this))
				.flatMap(exposer -> exposer.getInfectionStart());
	} 
	
	/**
	 * For a new infection this finds the contact with maximal viral exposure 
	 * on the first day before the subject is infectious when the subject was 
	 * exposed by an "infector". From this "infector" the time of their 
	 * infection is identified so that all the infections from one infector 
	 * should map back to the same instant in time for that infector.
	 * @return
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
	 * If person is infected here find the infectee state at the point in time 
	 * they became infectious. 
	 */
	@Value.Lazy
	@Deprecated
	default Optional<PersonHistory> getInfectionStart() {
		if (!this.isInfectious()) return Optional.empty();
		if (this.isIncidentInfection()) return Optional.of(this);
		return this.getPrevious().flatMap(p -> p.getInfectionStart());
	}
	
	/**
	 * Find if a person is newly infectious on this day. This is not the same as being
	 * newly exposed.
	 * @return
	 */
	@Deprecated
	@Value.Lazy default Optional<PersonHistory> getNewInfection() {
		if (isIncidentInfection()) return Optional.of(this);
		return Optional.empty();
	}
	
	/**
	 * Find the infectee's state at the start of last episode of exposure to virus,
	 * I.e. the first in a run of exposures, which are separated by less that 
	 * one infectious period. 
	 */
	default Optional<PersonHistory> getLastExposure() {
		if (this.isIncidentExposure()) return Optional.of(this);
		return this.getPrevious().flatMap(p -> p.getLastExposure());
	}
	
	/**
	 * A set of all the tests for this patient taken in the last N days. This
	 * includes tests of all statuses
	 */
	default Stream<TestResult> getHistoricalTests(int limit) {
		if (limit == 0) return this.getTodaysTests().stream();
		return Stream.concat(
			this.getTodaysTests().stream(),
			this.getPrevious().stream().flatMap(ph -> ph.getHistoricalTests(limit-1))
		);
	}
	
	/**
	 * All the tests done in the last presumed infectious period. Relies on the
	 * best available assumption of the infectious period, and is an observed 
	 * quantity;
	 */
	default Stream<TestResult> getStillRelevantTests() {
		return this.getHistoricalTests(infPeriod());
	}
	
	/**
	 * The collection of possibly still relevant test results for an individual that
	 * generate a result on this day, regardless of when they were taken. This does look
	 * backwards over all possibly relevant tests, there is possibility that some
	 * tests that take a very long time to process compared to the infectious period
	 * will not get picked up by this. As such this is an observed quantity.
	 */
	@Value.Lazy
	default List<TestResult> getTodaysResults() {
		return this.getStillRelevantTests()
			.filter(r -> r.isResultToday(this.getTime()))
			.collect(Collectors.toList());
	}
	
	/**
	 * All the contacts in the last X days. This is regardless of whether they
	 * were observed and is a modelled quantity. 
	 */
	default Stream<Contact> getHistoricalContacts(int limit) {
		if (limit == 0) return Arrays.stream(this.getTodaysContacts());
		return Stream.concat(
			Arrays.stream(this.getTodaysContacts()),
			this.getPrevious().stream().flatMap(ph -> ph.getHistoricalContacts(limit-1))
		);
	}
	
	/**
	 * All the contacts in the last presumed infectious period that were
	 * detected. This is an observed quantity.
	 */
	default Stream<Contact> getStillRelevantDetectedContacts() {
		return this.getHistoricalContacts(infPeriod()).filter(c -> c.isDetected());
	}
	
	/**
	 * Test results from tests taken today as assessed at some point in the
	 * future. This is only tests that have results on day X.
	 */
	default Stream<TestResult> getResultsBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return Stream.empty();
		return this.getTodaysTests().stream().filter(tr -> tr.isResultAvailable(time));
	}
	
	/**
	 * The maximum delay for test results for this individual is used when we
	 * assemble the forward looking list of test results on a per day basis. 
	 * i.e. the list of results organised by sample date as they would appear
	 * on the release date, including delay distribution.
	 */
	@Value.Derived default int getMaxDelay() {
		return (int) this.getTodaysTests().stream().mapToLong(tr -> tr.getDelay()).max().orElse(0);
	}
	
	/**
	 * Tests that an event has happened at least once in the last N days
	 */
	default boolean isRecently(Predicate<PersonTemporalState> test, int limit) {
		if (limit == 0) return test.test(this);
		if (test.test(this)) return true;
		return getPrevious().map(p -> p.isRecently(test, limit-1)).orElse(Boolean.FALSE);
	}
	
	/**
	 * Tests that an event has happened every day in the last N days
	 */
	default boolean isContinuously(Predicate<PersonTemporalState> test, int limit) {
		if (limit == 0) return test.test(this);
		if (!test.test(this)) return false;
		return getPrevious().map(p -> p.isContinuously(test, limit-1)).orElse(Boolean.FALSE);
	}
	
}