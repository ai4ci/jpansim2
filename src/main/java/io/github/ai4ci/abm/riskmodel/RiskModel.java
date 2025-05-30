package io.github.ai4ci.abm.riskmodel;

import java.io.Serializable;
import java.util.Optional;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.riskmodel.ImmutableRiskModel.Builder;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ModelNav;

/**
 * Establish an individuals risk on a given day. Risk on a give n day is a 
 * function of the day of contact and the day on which the risk is estimated
 * (usually this is going to be now). The reason for this is that evidence
 * changes over time, as new information is obtained. For example given a 
 * contact on day X, if the exposer develops symptoms on day X+1 (and reports 
 * them) then the risk to the exposee as calculated on day X+1 will be more than
 * on day X. Because of this there is a limited amount you can precalculate as
 * the answer will depend on observations that may or may not have happened yet.
 * What we can calculate is what the results of symptoms today mean for contacts
 * in the past and what tests taken today mean for 
 */
@Value.Immutable
public interface RiskModel extends Serializable {
	
	
	// This is 1 here because the symptoms are only recorded if the person
	// has already been tested as compliant && is symptomatic. It is still 
	// possible if compliant 
	// that the person won't report negative symptoms in which case the absence
	// of symptoms is not informative. The whole risk model is very sensitive to 
	// these values along with the kernels
	double PROB_REPORTING_POSITIVE_SYMPTOMS = 1;
	double PROB_REPORTING_NEGATIVE_SYMPTOMS = 0.02;
	
	Person getEntity();
	
	@Value.Redacted Kernel getSymptomKernel();
	@Value.Redacted Kernel getTestKernel();
	@Value.Redacted Kernel getContactsKernel();
	
	/** 
	 * What is this evidence of this person being infectious 0..N days in the past
	 * judged today expressed as a log odds, based on symptoms and test results
	 * that are available today.
	 */
	@Value.Default default double[] getDirectLogOdds() {return new double[0];}
	
	
	/** 
	 * What is this individuals risk of infection today based on a prior?
	 */
	@Value.Lazy default double getProbabilityInfectiousToday() {
		double prior = 0.0025;
		// double prior = 0.5;
		// double prior = getEntity().getOutbreak().getCurrentState().getPresumedTestPositivePrevalence();
		// double prior = getEntity().getCurrentHistory().map(p -> p.getPresumedLocalPrevalence()).orElse(0.0001);
		return Conversions.expit(
			Conversions.logit(prior) + 
				getDirectLogOddsInPast(0) + 
				getIndirectLogOdds()
		);
	};
	
	/** 
	 * What is this individuals odds ratio of infection today compared to the
	 * baseline of the population
	 */
	@Value.Lazy default double getLogOddsInfectiousToday() {
		return getDirectLogOddsInPast(0) + 
			getIndirectLogOdds();
	};
	
	int getTime();
	
	// What is the longest it is worth holding information about infection risk
	// for? The conacts kernel retrospectiveSize is one sensible limit. Beyond that there
	// is no value to the information anyway for determining future risk. Is this 
	@Value.Derived default int getMaxLength() {
		return Math.max(Math.max(
				getContactsKernel().retrospectiveSize(),
				getSymptomKernel().retrospectiveSize()),
				getTestKernel().retrospectiveSize());
	}
	
	// truncated copy of array
	private double[] copyOf(double[] old) {
		int len = Math.min(old.length+1, getMaxLength());
		double[] out = new double[len];
		for (int i=1; i<out.length; i++) out[i] = old[i-1];
		return out;
	}
	
	private double[] updateDirectLogOdds(double[] old) {
		double[] newer = copyOf(old);
		Kernel symptoms = getSymptomKernel();
		
		// the old state accounts for information from symptoms up to last time 
		// point and we can reuse that (since results are not updated in this model).
		
		// old symptoms inform new time point. the symptoms are relevant up
		// to the size of the future part of the kernel. Todays value included here
		for (int i=0; i < symptoms.retrospectiveSize(); i++) {
			Optional<PersonHistory> ph = this.getEntity().getHistory(i);
			final double density = symptoms.getDensity(i);
			if (ph.isPresent()) { 
				newer[0] += symptomLogLik(ph.get().isReportedSymptomatic())*density;
			}
		}
		
		// todays symptom state informs past time points, todays results inform
		// old time points up to past part of the kernel
		
		{
			Optional<PersonHistory> ph = this.getEntity().getCurrentHistory();
			if (ph.isPresent()) {
				int maxIndex = Math.min(newer.length-1, symptoms.prospectiveSize());
				for (int i=1; i <= maxIndex; i++) {
					newer[i] += symptomLogLik(ph.get().isReportedSymptomatic())*symptoms.getDensity(-i);
				}
			}
		}
		
		// Tests are harder. The tests taken today will only become informative
		// when the result is available. When the result is available it will 
		// inform the period around the test sample date.
		// we do this as before in two stages. all previous tests and how they 
		// inform today.
		Kernel tests = getTestKernel();
		for (int i=0; i < tests.retrospectiveSize(); i++) {
			Optional<PersonHistory> ph = this.getEntity().getHistory(i);
			double density = tests.getDensity(i);
			final int tmpI = i;
			if (ph.isPresent()) {
				// on day i in the past there were tests taken.
				for (TestResult tr: ph.get().getTodaysTests()) {
					// the information value of that test depends on the time
					// delay, while the result is pending
					// tests taken today with no delay should be included here.
					// e.g. LFTs
					newer[0] += tr.logLikelihoodRatio(tmpI) * density;
				}
			}
		}
		
		// OK what do todays results tell us about the past? 
		
		{
			Optional<PersonHistory> ph = this.getEntity().getCurrentHistory();
			if (ph.isPresent()) {
				for (TestResult tr: ph.get().getResults()) {
					// for a given test result delayed by 3 days with a kernel
					// with max past size of 7 days
					// a positive result influences everything up to the past size
					// of the kernel plus the delay (from 1 to 10 days in the past).
					int maxIndex = Math.min(newer.length-1, tests.prospectiveSize()+(int) tr.getDelay());
					for (int i=1; i <= maxIndex; i++) {
						// The kernel offset has to be calculated such that the
						// delay is accounted for.
						int offset = (int) tr.getDelay()-i;
						newer[i] += tr.trueLogLikelihoodRatio() * symptoms.getDensity(offset);
					}
				}
			}
		}
		
		return newer;
	}
	
	/** 
	 * What will the evidence be for this person being infectious today 
	 * as a result of their past contacts. This value is would change retrospectively 
	 * if you estimated it again the next day as the contacts test results and 
	 * symptoms change. However this is only useful information if we are interested
	 * in contacts of contacts. We are no so we do the simpler thing which is
	 * look at recent contacts and get an estimate for today only.
	 */
	@Value.Derived default double getIndirectLogOdds() {
		// double[] newer = new double[getMaxLength()];
		
		Kernel contacts = getContactsKernel();
		
		// old contacts inform new time points, but old contacts change also. 
		// Unlike symptoms and tests we don't get new information about the 
		// contacts but the information value of each contact changes. This
		// means we can't update the old value in the same way, and we 
		// basically have to recalculate it each time. If we wanted to do this
		// retrospectively we would have to check every contact for every past 
		// day which would be expensive.
		
		// This is only really useful if we are actually using historical
		// indirect log odds to inform a contact. So for that to be useful
		// we need to be considering if the contacts of an indirect contact are
		// informative in addition to the tests / symptoms of a direct contact. 
		// This seems unlikely to be beneficial.
		
		// What we are actually doing instead is getting a single value for the 
		// most up to date contacts. This value is only right for the point in
		// time at which it is calculated.
		
		double logOdds = 0;
		
		for (int i=0; i< contacts.retrospectiveSize(); i++) {
			Optional<PersonHistory> ph = this.getEntity().getHistory(i);
			final double density = contacts.getDensity(i);
			if (ph.isPresent()) {
				for (Contact c: ph.get().getTodaysContacts()) {
					if (c.isDetected()) {
						logOdds += c.getParticipantState(ph.get()).getRiskModel().getDirectLogOddsInPast(i) * 
							//c.getProximityDuration() *
							density;
					}
				}
			}
		}
		
		return logOdds;
	};
	
	default double getDirectLogOddsInPast(int i) {
		if (i<0 || i >= this.getDirectLogOdds().length) return 0;
		return this.getDirectLogOdds()[i];
	};

	private double symptomLogLik(boolean isKnownSymptomatic) {
		// At the moment this is not adjusted for the probability of app usage
		double sens = ModelNav.modelState(this.getEntity()).getPresumedSymptomSensitivity();
		double spec = ModelNav.modelState(this.getEntity()).getPresumedSymptomSpecificity();
		// Assume that the app only knows about positive symptoms, and not all
		// positive symptoms reported.
		return isKnownSymptomatic ?
				// Value of a symptom
				Math.log(sens / (1 - spec)) * PROB_REPORTING_POSITIVE_SYMPTOMS :
				// Value of a not reported symptom? could be 0D;
				// If symptoms
				Math.log((1 - sens) / (spec)) * PROB_REPORTING_NEGATIVE_SYMPTOMS;
	}
	
	static RiskModel initialise(Person person) {
		ExecutionConfiguration config = ModelNav.modelParam(person);
		double meanContacts = ModelNav.modelBase(person).getExpectedContactsPerPersonPerDay();
		return ImmutableRiskModel.builder()
				.setEntity(person)
				.setTime(person.getOutbreak().getCurrentState() == null ? 0 : person.getOutbreak().getCurrentState().getTime())
				.setSymptomKernel(config.getRiskModelSymptomKernel().kernel())
				.setTestKernel(config.getRiskModelTestKernel().kernel())
				.setContactsKernel(config.getRiskModelContactsKernel().kernel().scale(1/meanContacts))
				.build();
	};
	
	/**
	 * Update the risk model to the current day, based on the current state of
	 * the person. (The risk model is most likely immutable so this will be a new one).
	 * This is called during the update cycle at the point that the history
	 * is updated..
	 */
	default RiskModel update() {
		Builder tmp = ImmutableRiskModel.builder().from(this);
		tmp
			.setDirectLogOdds(updateDirectLogOdds(this.getDirectLogOdds()))
			.setTime(getTime()+1);
		return tmp.build();
	};
	
}
