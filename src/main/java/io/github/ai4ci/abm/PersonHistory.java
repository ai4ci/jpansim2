package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.util.ModelNav;

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
	 * Is this person newly exposed on this day? defined as the first time a non zero
	 * exposure is recorded within one infectious period.
	 * @return
	 */
	@Value.Lazy default boolean isIncidentExposure() {
		return isIncident(
			ph -> ph.getContactExposure() > 0,
			ModelNav.modelBase(this).getInfectiveDuration()
		);
	}
	
	/**
	 * Is this person newly infectious on this day.
	 * @return
	 */
	@Value.Lazy default boolean isIncidentInfection() {
		return isIncident(
			ph -> ph.isInfectious(),
			ModelNav.modelBase(this).getInfectiveDuration()
		);
	}
	
	/**
	 * Is this person newly needing hospitalisation on this day, or is this part of 
	 * the same infection episode (as defined by the average duration of 
	 * symptoms.
	 * @return
	 */
	@Value.Lazy default boolean isIncidentHospitalisation() {
		return isIncident(
			ph -> ph.isRequiringHospitalisation(),
			ModelNav.modelBase(this).getSymptomDuration()
		);
	}
	
	
	private boolean isIncident(Predicate<PersonHistory> test, int limit) {
		if (!test.test(this)) return false;
		if (getPrevious().isPresent()) {
			if (!getPrevious().get().isRecently(
				test,
				limit
			)) return true;
		}
		return false;
	}
	
	private boolean isRecently(Predicate<PersonHistory> test, int limit) {
		if (limit == 0) return false;
		if (test.test(this)) return true;
		return getPrevious().map(p -> p.isRecently(test, limit-1)).orElse(Boolean.FALSE);
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
	
	default Stream<TestResult> getHistoricalTests(int limit) {
		if (limit == 0) return this.getTodaysTests().stream();
		return Stream.concat(
			this.getTodaysTests().stream(),
			this.getPrevious().stream().flatMap(ph -> ph.getHistoricalTests(limit-1))
		);
	}
	
	/**
	 * All the tests done in the last presumed infectious period
	 */
	default Stream<TestResult> getStillRelevantTests() {
		return this.getHistoricalTests(infPeriod());
	}
	
	/**
	 * The collection of possibly still relevant test results for an individual that
	 * generate a result on this day, regardless of when they were taken. This does look
	 * backwards over all possibly relevant tests, there is possibility that some
	 * tests that take a very long time to process compared to the infectious period
	 * will not get picked up by this logic and we may have to revisit
	 */
	@Value.Lazy
	default List<TestResult> getResults() {
		return this.getStillRelevantTests().filter(r -> r.isResultToday(this.getTime())).collect(Collectors.toList());
	};
	
	default Stream<Contact> getHistoricalContacts(int limit) {
		if (limit == 0) return Arrays.stream(this.getTodaysContacts());
		return Stream.concat(
			Arrays.stream(this.getTodaysContacts()),
			this.getPrevious().stream().flatMap(ph -> ph.getHistoricalContacts(limit-1))
		);
	}
	
	/**
	 * All the contacts in the last presumed infectious period
	 */
	default Stream<Contact> getStillRelevantContacts() {
		return this.getHistoricalContacts(infPeriod());
	}
	
//	/**
//	 * On a day in the future what do the test results taken up to this day 
//	 * tell us about the relative odds of a person being infected on this day
//	 * now that we know the results. 
//	 */
//	default double getHistoricalTestLogLikelihood(int day, int limit) {
//		return this.getHistoricalTests(limit)
//			.mapToDouble(t -> t.logLikelihoodRatio(day, limit) )
//			.sum();
//	}
//	
//	/**
//	 * The log-likelihood of an individual being infected at some point in the 
//	 * past. We need this in the history because we need to know for a historical 
//	 * contact what their log-liklihood was at the time of the contact
//	 * @param day when are we interested in (i.e. usually the day of contact) 
//	 * @param limit how long prior to the day are we interested in? so a test a 
//	 * long time age is irrelevant.
//	 */
//	default double getDirectLogLikelihood(int day, int limit) {
//		return 
//				this.getSymptomLogLikelihood() +
//				this.getHistoricalTestLogLikelihood(day, limit);
//	}
	
	default Stream<TestResult> getResultsBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return Stream.empty();
		if (lim >= this.getMaxDelay()) return this.getTodaysTests().stream();
		List<List<TestResult>> tmp = getResultsBySampleDate();
		return tmp.stream().limit(lim).flatMap(l -> l.stream());
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
	 * The list of tests taken on this day, indexed by the delay until the results 
	 * are available.
	 */
	@Value.Lazy default List<List<TestResult>> getResultsBySampleDate() {
		return 
			IntStream.range(0, getMaxDelay()).mapToObj(delay -> 
						this.getTodaysTests().stream().filter(tr -> tr.getDelay() == delay).collect(Collectors.toList())
			)
			.collect(Collectors.toList());
	}

//	/**
//	 * An estimate of the person's infectiousness on this day based on test, symptoms
//	 * and contacts, as might be calculated by a smart agent.
//	 */
//	default double getProbabilityInfectiousToday() {
//		return this.getRiskModel().getProbabilityInfectiousToday();
//	};
	
}