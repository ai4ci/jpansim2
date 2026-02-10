package io.github.ai4ci.abm.riskmodel;

import java.io.Serializable;
import java.util.Optional;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.ModelNav;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.riskmodel.ImmutableRiskModel.Builder;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.util.Conversions;

/**
 * Bayesian risk estimation model for determining individual infectious probability.
 * 
 * <h2>Overview</h2>
 * <p> Establishes an individual's risk on a given day. Risk on a given day is a 
 * function of the day of contact and the day on which the risk is estimated
 * (usually this is going to be now). The risk is based on both direct evidence 
 * from tests and symptoms and indirect evidence from contacts. Because results
 * from tests are delayed and symptoms evolve over time the risk has to be recalculated
 * every day, as new information emerges about the index person but also about
 * their contacts.
 * 
 * <p> For example given a 
 * contact on day X, if the exposer develops symptoms on day X+1 (and reports 
 * them) then the risk to the exposee as calculated on day X+1 will be more than
 * on day X. Because of this there is a limited amount you can pre-calculate as
 * the answer will depend on observations that may or may not have happened yet.
 * What we can calculate is what the results of symptoms today mean for contacts
 * in the past and how tests taken today impact risk to contacts in the past.  
 * 
 * <p>This model calculates an individual's risk of being infectious on a given day based on:
 * <ul>
 *   <li>Direct evidence: symptoms and test results through time</li>
 *   <li>Indirect evidence: contact tracing and exposure history</li>
 * </ul>
 * 
 * <p>The risk evolves over time as new information becomes available. Calculations follow
 * Bayesian updating principles with log-odds formulations for computational stability.
 * 
 * <h2>Bayesian Framework</h2>
 * <p>Risk is calculated using Bayes' theorem with log-odds transformation:
 * \[
 * \text{posterior odds} = \text{prior odds} \times \text{likelihood ratio}
 * \]
 * In log-odds space this becomes:
 * \[
 * \log\left(\frac{p}{1-p}\right)_{\text{posterior}} = \log\left(\frac{p}{1-p}\right)_{\text{prior}} + \log(\text{likelihood ratio})
 * \]
 * 
 * <h2>Evidence Sources</h2>
 * <p>The model uses three types of convolution filters to weight evidence over time:
 * <ul>
 *   <li><b>Symptom Kernel</b>: Temporal weighting of symptom reporting</li>
 *   <li><b>Test Kernel</b>: Temporal weighting of test results including delays</li>
 *   <li><b>Contacts Kernel</b>: Temporal weighting of exposure risk from contacts</li>
 * </ul>
 * 
 * <h2>Temporal Evolution</h2>
 * <p>Evidence accumulation follows a dynamic programming approach where:
 * \[
 * \mathbf{logOdds}_{t} = f(\mathbf{logOdds}_{t-1}, \text{new evidence}_t)
 * \]
 * Each day's risk estimate incorporates both new evidence and re-evaluation of past evidence
 * in light of new developments.
 */
@Value.Immutable
public interface RiskModel extends Serializable {
	
	public Logger log = LoggerFactory.getLogger(RiskModel.class);
	
	
	
	/**
	 * Probability of reporting symptoms when symptomatic (app compliance).
	 * This represents reporting sensitivity through symptom reporting mechanisms.
	 * <p> This is 1 here because the symptoms are only recorded if the person
	 * has already been tested as compliant and is symptomatic. It is still 
	 * possible if compliant 
	 * that the person won't report negative symptoms in which case the absence
	 * of symptoms is not informative. The whole risk model is very sensitive to 
	 * these values along with the kernels
	 */
	double PROB_REPORTING_POSITIVE_SYMPTOMS = 1;
	
	/**
	 * Probability of symptom absence being reported when asymptomatic.
	 * Represents the specificity of symptom absence reporting.
	 */
	double PROB_REPORTING_NEGATIVE_SYMPTOMS = 0.02;

	/** @return the person entity associated with this risk model */
	Person getEntity();
	
	/** @return convolution filter for symptom evidence weighting */
	@Value.Redacted ConvolutionFilter getSymptomKernel();
	
	/** @return convolution filter for test evidence weighting */
	@Value.Redacted ConvolutionFilter getTestKernel();
	
	/** @return convolution filter for contact exposure evidence weighting */
	@Value.Redacted ConvolutionFilter getContactsKernel();
	
	/** 
	 * Array of direct log-odds evidence for infectiousness on past days.
	 * Index 0 represents today, increasing indices represent further in the past.
	 * Calculated as: 
         * \[\log\left(\frac{p(\text{infectious}|evidence)}{1-p(\text{infectious}|evidence)}\right)\]
         * 
         * <p> What is this evidence of this person being infectious 0..N days in the past
	 * judged today expressed as a log odds, based on symptoms and test results
	 * that are available today.
	 */
	@Value.Default default double[] getDirectLogOdds() {return new double[0];}
	
	
	/** 
	 * Probability of being infectious today, incorporating all available evidence.
	 * 
	 * <p>Calculated using Bayesian updating with logistic transformation:
	 * \[
	 * p_{\text{infectious}} = \text{expit}\left(\log\left(\frac{p_{\text{prior}}}{1-p_{\text{prior}}}\right) + LR_{\text{direct}} + LR_{\text{indirect}}\right)
	 * \]
	 * where \(\text{expit}(x) = \frac{e^x}{1 + e^x}\) is the logistic function.
	 * 
	 * <p>Uses a small prior (0.0025) to prevent numerical issues with extreme log-odds.
	 * 
	 * @return probability of infectiousness today ∈ [0,1]
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
	 * Log-odds of infectiousness today relative to baseline population prevalence.
	 * 
	 * <p>Represents the combined evidence from direct measurements and contact exposures:
	 * \[
	 * LR_{\text{total}} = LR_{\text{direct}}(0) + LR_{\text{indirect}}
	 * \]
	 * <p>What is the longest it is worth holding information about infection risk
	 * for? The contacts kernel retrospectiveSize is one sensible limit. Beyond that there
	 * is no value to the information anyway for determining future risk. Is this 
	 * 
	 * @return combined log-likelihood ratio for infectiousness today
	 */
	@Value.Lazy default double getLogOddsInfectiousToday() {
		return getDirectLogOddsInPast(0) + 
			getIndirectLogOdds();
	};
	
	/** @return current simulation time step */
	int getTime();
	
	/**
	 * Maximum temporal window for evidence retention.
	 * 
	 * <p>Determined by the largest retrospective size among the convolution kernels:
	 * \[
	 * \text{maxLength} = \max(\text{symptomKernel.retrospectiveSize}, \text{testKernel.retrospectiveSize}, \text{contactsKernel.retrospectiveSize})
	 * \]
	 * 
	 * @return maximum number of days to retain evidence
	 */
	@Value.Derived default int getMaxLength() {
		return Math.max(Math.max(
				getContactsKernel().retrospectiveSize(),
				getSymptomKernel().retrospectiveSize()),
				getTestKernel().retrospectiveSize());
	}
	
	/**
	 * Shift evidence array one day forward, adding today's slot at index 0.
	 * Past evidence beyond maxLength is truncated. This is only possible
	 * for direct evidence as indirect evidence is recalculated each day.
	 * 
	 * @param old previous day's evidence array
	 * @return new array with updated timing
	 */
	private double[] copyOf(double[] old) {
		int len = Math.min(old.length+1, getMaxLength());
		double[] out = new double[len];
		for (int i=1; i<out.length; i++) out[i] = old[i-1];
		return out;
	}
	
	/**
	 * Update direct log-odds evidence with new information from today.
	 * 
	 * <p>Implements a temporal Bayesian filter that:
	 * 1. Shifts previous evidence forward in time
	 * 2. Incorporates new symptom evidence using symptom kernel convolution
	 * 3. Incorporates new test evidence using test kernel convolution
	 * 
	 * Logic here is complex. We are updating what we know about the past given
	 * the extra information that becomes available today. The old directLogOdds
	 * array is 0..N days in the past from yesterday. We shift this one additional
	 * day and set todays directLogOdds to 0 using copyOf
     *
	 * <p>Symptom evidence convolution:
	 * \[
	 * LR_{\text{new}}[0] += \sum_{i=0}^{N_{\text{retro}}} \text{symptomLR}(i) \times K_{\text{symptom}}(i)
	 * \]
	 * \[
	 * LR_{\text{new}}[j] += \sum_{j=1}^{N_{\text{prosp}}} \text{symptomLR}(today) \times K_{\text{symptom}}(-j)
	 * \]
	 * 
	 * <p>Test evidence follows similar convolution patterns accounting for result delays.
	 * 
	 * @param old previous day's evidence array
	 * @return updated evidence array incorporating today's information
	 */
	private double[] updateDirectLogOdds(double[] old) {
		
		// the old state accounts for information from symptoms up to last time 
		// point and we can reuse that (since results are not updated in this model).
		double[] newer = copyOf(old);
		// Additional information from symptoms today:
		ConvolutionFilter symptoms = getSymptomKernel();
		
		// old symptoms inform new time point (today). the symptoms are relevant up
		// to the size of the future part of the kernel. Todays symptoms also included here
		// This is all updating index 0 of the array
		for (int i=0; i < symptoms.retrospectiveSize(); i++) {
			Optional<PersonHistory> ph = this.getEntity().getHistory(i);
			final double density = symptoms.getDensity(i);
			if (ph.isPresent()) { 
				newer[0] += symptomLogLik(ph.get().isReportedSymptomatic())*density;
			}
		}
		
		// todays symptom state informs past time points, todays results inform
		// old time points up to past part of the kernel
		// this is updating indices 1..N of the array
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
		ConvolutionFilter tests = getTestKernel();
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
				for (TestResult tr: ph.get().getTodaysResults()) {
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
	 * Indirect log-odds from contact exposures.
	 * 
	 * <p>Calculates risk contribution from contact tracing:
	 * \[
	 * LR_{\text{indirect}} = \sum_{i=0}^{N_{\text{contacts}}} \sum_{\text{contact}\in\text{contacts}_i} LR_{\text{contact}}(i) \times K_{\text{contacts}}(i)
	 * \]
	 * 
	 * <p>Unlike direct evidence, contact risk is recalculated daily rather than
	 * updated incrementally, as contact risk evolves with changing contact states.
	 * What will the evidence be for this person being infectious today 
	 * as a result of their past contacts. This value is would change retrospectively 
	 * if you estimated it again the next day as the contacts test results and 
	 * symptoms change. However this is only useful information if we are interested
	 * in contacts of contacts. We are no so we do the simpler thing which is
	 * look at recent contacts and get an estimate for today only.
     *
	 * @return log-likelihood ratio from contact exposures
	 */
	@Value.Lazy default double getIndirectLogOdds() {
		// double[] newer = new double[getMaxLength()];
		
		ConvolutionFilter contacts = getContactsKernel();
		
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
					try {
						if (c.isDetected()) {
							logOdds += c.getParticipantState(ph.get()).getRiskModel().getDirectLogOddsInPast(i) * 
								//c.getProximityDuration() *
								density;
						}
					} catch (NullPointerException e) {
						log.error("Persistent null pointer in contact network detected");
					}
				}
			}
		}
		
		return logOdds;
	};
	
	/**
	 * Retrieve direct log-odds evidence for a specific past day.
	 * 
	 * @param i days in the past (0 = today, 1 = yesterday, etc.)
	 * @return log-odds evidence for infectiousness on that day
	 */
	default double getDirectLogOddsInPast(int i) {
		if (i<0 || i >= this.getDirectLogOdds().length) return 0;
		return this.getDirectLogOdds()[i];
	};

	/**
	 * Calculate symptom log-likelihood ratio for Bayesian updating.
	 * 
	 * <p>For symptomatic reports:
	 * \[
	 * LR_{\text{symptom}} = \log\left(\frac{\text{sensitivity}}{1 - \text{specificity}}\right) \times P_{\text{report}}
	 * \]
	 * 
	 * <p>For asymptomatic reports:
	 * \[
	 * LR_{\text{no-symptom}} = \log\left(\frac{1 - \text{sensitivity}}{\text{specificity}}\right) \times P_{\text{no-report}}
	 * \]
	 * 
	 * @param isKnownSymptomatic whether symptoms were reported
	 * @return log-likelihood ratio for infectiousness given symptom evidence
	 */
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
	
	/**
	 * Initialize a new risk model for a person with configured convolution kernels.
	 * 
	 * <p>Sets up temporal weighting functions from execution configuration:
	 * - Symptom kernel: \(K_{\text{symptom}}(t)\)
	 * - Test kernel: \(K_{\text{test}}(t)\) 
	 * - Contacts kernel: \(K_{\text{contacts}}(t) / \text{expectedContacts}\)
	 * 
	 * @param person the individual to model
	 * @return initialized risk model ready for evidence accumulation
	 */
	static RiskModel initialise(Person person) {
		ExecutionConfiguration config = ModelNav.modelParam(person);
		double meanContacts = ModelNav.modelBase(person).getExpectedContactsPerPersonPerDay();
		return ImmutableRiskModel.builder()
				.setEntity(person)
				.setTime(person.getOutbreak().getCurrentState() == null ? 0 : person.getOutbreak().getCurrentState().getTime())
				.setSymptomKernel(ConvolutionFilter.from(config.getRiskModelSymptomKernel()))
				.setTestKernel(ConvolutionFilter.from(config.getRiskModelTestKernel()))
				.setContactsKernel(ConvolutionFilter.from(config.getRiskModelContactsKernel()).scale(1/meanContacts))
				.build();
	};
	
	/**
	 * Update risk model to current day with new evidence.
	 * 
	 * Update the risk model to the current day, based on the current state of
	 * the person. (The risk model is most likely immutable so this will be a new one).
	 * This is called during the update cycle at the point that the history
	 * is updated..
	 * <p>Performs one step of temporal Bayesian filtering:
	 * \[
	 * \mathbf{evidence}_{t} = \text{update}(\mathbf{evidence}_{t-1}, \text{observations}_t)
	 * \]
	 * 
	 * <p>Called during the daily update cycle when new history becomes available.
	 * 
	 * @return updated risk model with incremented time and new evidence incorporated
	 */
	default RiskModel update() {
		Builder tmp = ImmutableRiskModel.builder().from(this);
		tmp
			.setDirectLogOdds(updateDirectLogOdds(this.getDirectLogOdds()))
			.setTime(getTime()+1);
		return tmp.build();
	};
	
}
