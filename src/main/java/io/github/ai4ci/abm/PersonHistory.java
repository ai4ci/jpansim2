package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.immutables.value.Value;

@Value.Immutable
public interface PersonHistory extends PersonTemporalState {
	
	/**
	 * The list of tests taken today. Initially the result will be marked as 
	 * PENDING until the test is processed. (the true result will be available 
	 * to the model immediately). The TestResult objects
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
	Exposure[] getTodaysExposures();
	
	default Optional<PersonHistory> getPrevious() {
		return this.getEntity().getHistoryEntry(this.getTime()-1);
	};
	
	boolean isSymptomatic();
	boolean isReportedSymptomatic();
	Double getNormalisedSeverity();
	Double getNormalisedViralLoad();
	Double getAdjustedTransmissibility();
	Double getAdjustedMobility();
	Double getSusceptibilityModifier();
	String getBehaviour();
	Double getContactExposure();
	
	double getSymptomLogLikelihood();
	double getProbabilityInfectiousToday();
	
	public static class Infection implements Serializable {
		Contact contact;
		public static Infection create(Contact contact) {
			return new Infection(contact);
		}
		Infection(Contact contact) {this.contact = contact;}
		public Contact getContact() {return contact;}
	}
	
	/**
	 * For a new infection this finds the contact with maximal viral exposure 
	 * on the first day before the subject is infectious when the subject was 
	 * exposed by an "infector". From this "infector" the time of their 
	 * infection is identified so that all the infections from one infector 
	 * should map back to the same instant in time for that infector.
	 * @return
	 */
	@Value.Lazy default Optional<PersonHistory> getInfector() {
		return getInfectiousContact()
				.map(c -> c.getExposer(this))
				.flatMap(ph2 -> ph2.getInfectionStart());
	} 
	
	/**
	 * For a new infection this finds the contact with maximal viral exposure 
	 * on the first day before the subject is infectious when the subject was 
	 * exposed by an "infector". From this "infector" the time of their 
	 * infection is identified so that all the infections from one infector 
	 * should map back to the same instant in time for that infector.
	 * @return
	 */
	@Value.Lazy default Optional<Exposure> getInfectiousContact() {
		return getNewInfection().flatMap(ph -> 
			ph.getLastExposure().flatMap(e -> 
				Arrays.stream(e.getTodaysExposures())
				.max((c1,c2) -> Double.compare(c1.getExposure(), c2.getExposure())
				))
		);
	}
	
	default Optional<PersonHistory> getInfectionStart() {
		return this.getInfectionStart(getTime());
	}
	
	/**
	 * If person is infected here find the index at the point in time they 
	 * were infected. 
	 * @return
	 */
	@Value.Lazy default Optional<PersonHistory> getInfectionStart(int limit) {
		if (limit==0) return Optional.empty();
		if (!isInfectious()) return Optional.empty();
		if (getPrevious().isPresent()) {
			// This is infectious and previous is not.
			if (!getPrevious().get().isInfectious()) return Optional.of(this);
			// This is infectious and previous is infectious. Continue looking.
			else return getInfectionStart(limit-1);
		}
		// This is infectious and no previous can be found. Assume this is an 
		// imported case, we will set it as the start, but empty might be just as
		// valid.
		return Optional.of(this);
	}
	
	/**
	 * Find if a person is newly infectious today.
	 * @return
	 */
	@Value.Lazy default Optional<PersonHistory> getNewInfection() {
		if (!isInfectious()) return Optional.empty();
		if (getPrevious().isPresent()) {
			if (!getPrevious().get().isInfectious()) return Optional.of(this);
		}
		return Optional.empty();
	}
	
	/**
	 * Is this person newly infectious today.
	 * @return
	 */
	@Value.Lazy default boolean isIncidentInfection() {
		if (!isInfectious()) return false;
		if (getPrevious().isPresent()) {
			if (!getPrevious().get().isInfectious()) return true;
		}
		return false;
	}
	
	/**
	 * Find the infectee's state at the time of last exposure to virus.
	 * @return
	 */
	default Optional<PersonHistory> getLastExposure() {
		if (this.getContactExposure() > 0 &&
				this.getPrevious().map(p -> p.getContactExposure() == 0).orElse(Boolean.FALSE)) 
					return Optional.of(this);
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
	 * On a day in the future what do the test results taken up to this day 
	 * tell us about the relative odds of a person being infected on this day
	 * now that we know the results. 
	 * @param day
	 * @param limit
	 * @return
	 */
	default double getHistoricalTestLogLikelihood(int day, int limit) {
		return this.getHistoricalTests(limit)
			.mapToDouble(t -> t.logLikelihoodRatio(day, limit) )
			.sum();
	}
	
	/**
	 * The log-likelihood of an individual being infected at some point in the 
	 * past.
	 * @param day when are we interested in (i.e. usually the day of contact) 
	 * @param limit how long prior to the day are we interested in? so a test a 
	 * long time age is irrelevant
	 * @return
	 */
	default double getDirectLogLikelihood(int day, int limit) {
		return 
				this.getSymptomLogLikelihood() +
				this.getHistoricalTestLogLikelihood(day, limit);
				// TODO: immunity? proven previous covid?
	}
	
	default Stream<TestResult> getResultsBySampleDate(int time) {
		int lim = time - this.getTime();
		if (lim < 0) return Stream.empty();
		if (lim >= this.getMaxDelay()) return this.getTodaysTests().stream();
		List<List<TestResult>> tmp = getResultsBySampleDate();
		return tmp.stream().limit(lim).flatMap(l -> l.stream());
	}
	
	/**
	 * The maximum delay for test results for this individual is used when we
	 * assemble
	 * @return
	 */
	@Value.Derived default int getMaxDelay() {
		return (int) this.getTodaysTests().stream().mapToLong(tr -> tr.getDelay()).max().orElse(0);
	}
	
	/**
	 * The list of tests taken today, indexed by the delay until the results 
	 * are available.
	 * @return
	 */
	@Value.Lazy default List<List<TestResult>> getResultsBySampleDate() {
		return 
			IntStream.range(0, getMaxDelay()).mapToObj(delay -> 
						this.getTodaysTests().stream().filter(tr -> tr.getDelay() == delay).collect(Collectors.toList())
			)
			.collect(Collectors.toList());
	}
	
}