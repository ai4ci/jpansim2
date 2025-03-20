package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
	Set<Contact> getTodaysContacts();
	Optional<PersonHistory> getPrevious();
	
	boolean isSymptomatic();
	boolean isReportedSymptomatic();
	Double getNormalisedSeverity();
	Double getNormalisedViralLoad();
	Double getAdjustedTransmissibility();
	Double getAdjustedMobility();
	String getBehaviour();
	Double getVirionExposure();
	
	double getSymptomLogLikelihood();
	double getProbabilityInfectiousToday();
	
	public static class Infection implements Serializable {}
	
	/**
	 * For a new infection this finds the contact with maximal viral exposure 
	 * on the first day before the subject is infectious when the subject was 
	 * exposed by an "infector". From this "infector" the time of their 
	 * infection is identified so that all the infections from one infector 
	 * should map back to the same instant in time for that infector.
	 * @return
	 */
	@Value.Lazy default Optional<PersonHistory> getInfector() {
		return getNewInfection().flatMap(ph -> 
			ph.getLastExposure().flatMap(e -> 
				e.getTodaysContacts().stream()
				.max((c1,c2) -> Double.compare(c1.getExposure(), c2.getExposure()))
				.map(c -> c.getParticipant(this.getEntity().getOutbreak()))
			).flatMap(ph2 -> ph2.getInfectionStart())
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
	 * Find the previous last exposure to virus
	 * @return
	 */
	default Optional<PersonHistory> getLastExposure() {
		if (this.getVirionExposure() > 0 &&
				this.getPrevious().map(p -> p.getVirionExposure() == 0).orElse(Boolean.FALSE)) 
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
	
	default double getDirectLogLikelihood(int day, int limit) {
		return 
				this.getSymptomLogLikelihood() +
				this.getHistoricalTestLogLikelihood(day, limit);
				// TODO: immunity? proven previous covid?
	}
	
}