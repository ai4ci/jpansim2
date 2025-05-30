package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.TestResult.Type;
import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.abm.riskmodel.RiskModel;
import io.github.ai4ci.config.TestParameters;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ModelNav;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface PersonState extends PersonTemporalState {

	/**
	 * This is the maximum exposure an individual can experience in a single day in
	 * normalised units, where 1 is an average dose, and 0 is the minimum. If this
	 * value is too high multiple exposures aggregate and dose dependent effects
	 * become important
	 */
	double MAX_EXPOSURE = 4;

	// Optional<Integer> getLastTestedTime();
	// Optional<Integer> getLastInfectedTime();
	// @Value.Default default Boolean isScreened() {return false;}
	// Double getScreeningInterval();

	/**
	 * An odds ratio of transmission that modifies the baseline probability of
	 * transmission to another person
	 */
	@Value.Default
	default double getTransmissibilityModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio of mobility that modifies the baseline contact probability
	 */
	@Value.Default
	default double getMobilityModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio of compliance that modifies the baseline compliance probability
	 * that the person is going to follow guidance.
	 */
	@Value.Default
	default double getComplianceModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio of that modifies the baseline probability of transmission from
	 * another person to this one
	 */
	@Value.Default
	default double getSusceptibilityModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio that modifies the baseline probability that the person is using
	 * a smart agent to record data, and get personalised guidance
	 */
	@Value.Default
	default double getAppUseModifier() {
		return 1.0D;
	}

	/**
	 * A viral exposure as a result of importation this may be the result of a time
	 * varying function representing external drivers.
	 */
	@Value.Default
	default double getImportationExposure() {
		return 0D;
	}

	/**
	 * An immunisation dose is a fraction of dormant immune cells that are activated
	 * by any immunising exposure in the previous day. Setting this to something non
	 * zero implies that the person was immunised at this time point. It is cleared
	 * after every timestep by the Updater.
	 */
	@Value.Default
	default double getImmunisationDose() {
		return 0D;
	}
	// TODO: Immunisation schedules and other time varying functions are not
	// implemented: {@link ModelUpdate#IMMUNISATION_PROTOCOL}

	InHostModelState<?> getInHostModel();
	
	@Value.Default default RiskModel getRiskModel() {
		return RiskModel.initialise(this.getEntity());
	}

	/**
	 * A normalised severity index where 1 is symptomatic (excepting symptom
	 * ascertainment error.)
	 */
	default double getNormalisedSeverity() {
		return getInHostModel().getNormalisedSeverity();
	}

	/** A normalised viral load index where 1 is infectious. */
	default double getNormalisedViralLoad() {
		return getInHostModel().getNormalisedViralLoad();
	}

	/**
	 * Is the persons internal viral load above the threshold for potential
	 * infectivity.
	 */
	@Value.Derived
	default boolean isInfectious() {
		if (this.isDead())
			return false;
		return getNormalisedViralLoad() > 0;
	}

	/**
	 * Is the persons in-host state above the calibrated threshold for exhibiting
	 * symptoms on this day and symptom sensitivity and specificity are taken into
	 * account. This will be non deterministic
	 */
	@Value.Derived
	default boolean isSymptomatic() {
		if (this.isDead())
			return false;
		Sampler rng = Sampler.getSampler();
		double adjSev = getNormalisedSeverity() / ModelNav.modelBase(this).getSeveritySymptomsCutoff();
		adjSev = TestParameters.applyNoise(adjSev, 
				this.getEntity().getBaseline().getSymptomSensitivity(),
				this.getEntity().getBaseline().getSymptomSpecificity(),
				this.getEntity().getOutbreak().getBaseline().getSeveritySymptomsCutoff(), // limit of detection of symptoms
				rng);
		return adjSev >= 1;
	}

	/**
	 * Is the persons in-host state above the threshold for requiring
	 * hospitalisation on this day. This is deterministic on the result of the
	 * in-host model at the moment.
	 */
	@Value.Derived
	default boolean isRequiringHospitalisation() {
		if (this.isDead())
			return false;
		// Sampler rng = Sampler.getSampler();
		return getNormalisedSeverity() >= ModelNav.modelBase(this).getSeverityHospitalisationCutoff();
	}

	/** The person is dead */
	@Value.Derived
	default boolean isDead() {
		// Sampler rng = Sampler.getSampler();
		if (this.getEntity().getStateMachine().getState().equals(NonCompliant.DEAD))
			return true;
		return getNormalisedSeverity() >= ModelNav.modelBase(this).getSeverityDeathCutoff();
	}

	/**
	 * The user has reported symptoms via the app. This means their symptom state
	 * can inform local estimates of prevalence.
	 */
	@Value.Derived
	default boolean isReportedSymptomatic() {
		// assume user is too lazy to lie about symptoms
		if (!isSymptomatic())
			return false;
		return this.isUsingAppToday();
		// TODO: Probability of test reporting for tests such as LFT
		// some form of probablility of reporting LFT and symptoms.
		// However this is a very specific problem for self administered tests
		// unless you consider that the app may not have access to the canonical
		// test results.... unlikely.
		// reporting of tests is an individual behaviour so this needs to be
		// done independent of tests. It is however result specific
		// for LFTs and reporting of negatives was not done.
	}

	/** Is a person symptomatic for a number of days in a row */
	default boolean isSymptomaticConsecutively(int days) {
		return ModelNav.history(this, days).takeWhile(h -> h.isSymptomatic()).count() >= days;
	}

	/**
	 * Probability of contact given a fully mobile partner on a per day basis. The
	 * joint probability between 2 people within a social network will define the
	 * likelihood of a contact (Plus some randomness). A high mobility means a high
	 * probability of contact.
	 */
	default double getAdjustedMobility() {
		double tmp = Conversions.scaleProbabilityByOR(this.getEntity().getBaseline().getMobilityBaseline(),
				this.getMobilityModifier());
		return tmp;
	};

	default double getAbsoluteMobilityDecrease() {
		return this.getEntity().getBaseline().getMobilityBaseline() - getAdjustedMobility();
	}
	
	/**
	 * Probability of use of an app at any given point in time on a per day basis.
	 */
	default double getAdjustedAppUseProbability() {
		double tmp = Conversions.scaleProbabilityByOR(this.getEntity().getBaseline().getAppUseProbability(),
				this.getAppUseModifier());
		return tmp;
	};

	/**
	 * Probability of transmission given a contact with a completely unprotected
	 * person. A lower value means less likely transmission. This is a combination
	 * of several factors, the R0 and structure of the social network, the exogenous
	 * factors such as weather, endogenous factors such as inherent susceptibility
	 * due to age, and time varying behavioural factors such as mask wearing.
	 */
	default double getAdjustedTransmissibility() {
		double tmp = Conversions.scaleProbabilityByOR(
				ModelNav.modelBase(this).getTransmissibilityBaseline(this.getNormalisedViralLoad()),
				ModelNav.modelState(this).getTransmissibilityModifier()
						* ModelNav.baseline(this).getTransmissibilityModifier() * this.getTransmissibilityModifier());
		return tmp;
	};

	/**
	 * A probability that a person is compliant to guidance on any give day.
	 */
	default double getAdjustedCompliance() {
		return Conversions.scaleProbabilityByOR(this.getEntity().getBaseline().getComplianceBaseline(),
				this.getComplianceModifier());
	};
	
	default double getAbsoluteComplianceDecrease() {
		return this.getEntity().getBaseline().getComplianceBaseline() - getAdjustedCompliance();
	}

	/**
	 * It determines if the most recent test regardless of whether there is a result
	 * or it is still pending. This is how it needs to be for determining whether or
	 * not to do another test and is used in the testing strategies. It is a bad
	 * choice for anything else.
	 */
	default boolean isLastTestExactly(Result result) {
		// This is looking at the day behind for test results
		// because at the point this is run in the update cycle the
		// history has not yet been updated.
		Optional<Result> o = this.getLastTest().map(tr -> tr.resultOnDay(this.getTime()));
		return o.isPresent() && o.get().equals(result);
	}

	default boolean isCompliant(Sampler rng) {
		return rng.uniform() < this.getAdjustedCompliance();
	}

	@Value.Derived
	default boolean isCompliant() {
		if (isDead())
			return false;
		Sampler rng = Sampler.getSampler();
		return isCompliant(rng);
	}

	default boolean isUsingAppToday(Sampler rng) {
		return rng.bern(this.getAdjustedAppUseProbability());
	}

	@Value.Derived
	default boolean isUsingAppToday() {
		if (isDead())
			return false;
		Sampler rng = Sampler.getSampler();
		return isUsingAppToday(rng);
	}

	@Value.Lazy
	default String getBehaviour() {
		return this.getEntity().getStateMachine().getState().getName();
	}

	/**
	 * A normalised viral load dose, this is converted into virion numbers later and
	 * it is the sum of all the exposures over the course of a day. This is always
	 * going to work one day behind as the viral load model can only pick up the
	 * change for the next day. This is derived form the contact network when that
	 * is initialised.
	 * 
	 * N.B. the dose maxes out at 20 times the infectious viral load. This prevents
	 * ridiculously large initial viral doses when uncontrolled exponential growth.
	 * I could have done this in the getEposure() method of contact to control the
	 * maximum amount one individual can expose another by.
	 * 
	 * @return
	 */
	@Value.Lazy
	default double getContactExposure() {
		return Math.min(MAX_EXPOSURE, ModelNav.history(this).stream()
				.flatMap(ph -> Arrays.stream(ph.getTodaysExposures())).mapToDouble(p -> p.getExposure()).sum());
	}

	/** Exposure due to contact and importation */
	public default double getTotalExposure() {
		return getContactExposure() + getImportationExposure();
	}

	/**
	 * Looks at whether there are any tests of specified type within the last x
	 * days.
	 */
	default boolean isRecentlyTested(Type type) {
		return getRecentRuleOutTests().filter(t -> type.params().getTestName().equals(t.getTestParams().getTestName()))
				.findFirst().isPresent();
	};
	

	default boolean isRecentlyTested(Type type, int days) {
		return ModelNav.history(this, days).flatMap(ph -> ph.getTodaysTests().stream())
			.filter(t -> type.params().getTestName().equals(t.getTestParams().getTestName()))
			.findFirst().isPresent();
	};
	
	/**
	 * Estimate the local prevalence for an individual. This is average of all this
	 * person's contacts presumed probability of being infectious. I.e. if a contact 
	 * has a positive test result their risk will be higher, and this will .
	 */
	@Value.Lazy
	default double getPresumedLocalPrevalence() {
		double prev = getContactHistory().filter(c -> c.isDetected()).map(c -> c.getParticipantState(this))
				.mapToDouble(ph -> ph.getProbabilityInfectiousToday()).average().orElse(0D);
		// The local prevalence
		return prev != 0 ? prev :
		// prev2 !=0 ? prev2 :
				0.0001;
	}
	
	/** How many people in this persons recent contacts are infectious 
	 * regardless of whether on not the contact is detected. This integrates 
	 * over approx one infectious period (0.95 quantile) so will be lagged.  
	 */
	@Value.Lazy
	default double getTrueLocalPrevalence() {
		return getContactHistory().map(c -> c.getParticipantState(this))
				.mapToDouble(ph -> ph.isInfectious() ? 1.0 : 0.0).average().orElse(0D);
	}

//	@Value.Lazy default double getProbabilityImmuneToday() {
//		
//		double pImmuneYesterday = ModelNav.history(this).map(ph -> ph.getProbabilityImmuneToday()).orElse(0D);
//		ModelNav.history(this).map(ph -> ph.isRecentlyInfectious(0))
//		
//	}

//	/**
//	 * Looks at the previous tests and identifies if there was a positive test
//	 * result within the infectious period. This assumes a prior probability of
//	 * disease = local prevalence and the likelihood of each observed test outcome.
//	 */
//	@Value.Lazy
//	default double getProbabilityInfectiousToday() {
//		return ModelNav.history(this).map(ph -> ph.getProbabilityInfectiousToday()).orElse(0D);

//		double logOdds = Conversions.logit(this.getPresumedLocalPrevalence()) +
//		// Conversions.logit(ModelNav.modelState(this).getPresumedTestPositivePrevalence())
//		// +
////				Conversions.logit((
////						this.getPresumedLocalPrevalence()+
////						ModelNav.modelState(this).getPresumedTestPositivePrevalence())/2.0) +
//				getDirectLogLikelihood();
//		// There are 2 sources of log odds. Hence the average is -log(2)
//		return Conversions.expit(logOdds - Math.log(2));
//	};

//	/**
//	 * the log likelihood of disease given the history of tests. The value of this
//	 * will change in the future as more test results become available. We can't
//	 * pre-calculate this, and there is little point anyway as it only is used
//	 * directly for this individual at one point in time.
//	 * 
//	 * @return
//	 */
//	default double getTestLogLikelihood() {
//		return this.getStillRelevantTests().mapToDouble(t -> t.logLikelihoodRatio(this.getTime(), infPeriod())).sum();
//	}
//
//	/**
//	 * The log likelihood of disease given the history of symptoms. Each day of
//	 * symptoms in the presumed infectious period increases the chance the patient
//	 * is identified as symptomatic. This quantity doesn't change over time so we
//	 * can precalculate it (unlike the test log-liklihood which changes as 
//	 * results become available).
//	 */
//	@Value.Derived
//	default double getSymptomLogLikelihood() {
//		return ModelNav.history(this, DAYS_OF_SYMPTOMS).mapToDouble(ph ->
////				Conversions.wanLogOdds(
////						sympLogLik(this, ph.isReportedSymptomatic()), 
////						this.getTime() - ph.getTime(), 
////						samples)
//		symptomLogLik(this, ph.isReportedSymptomatic())).average().orElse(0);
//		// .sum();
//	}
//
//	private static double symptomLogLik(PersonState pers, boolean isKnownSymptomatic) {
//		// At the moment this is not adjusted for the probability of app usage
//		double sens = ModelNav.modelState(pers).getPresumedSymptomSensitivity();
//		double spec = ModelNav.modelState(pers).getPresumedSymptomSpecificity();
//		// Assume that the app only knows about positive symptoms, and not all
//		// positive symptoms reported.
//		return isKnownSymptomatic ?
//		// Value of a symptom
//				Math.log(sens / (1 - spec)) :
//				// Value of a not reported symptom:
//				Math.log((1 - sens) / (spec));
//	}
//
//	/**
//	 * The maximum log likelihood of disease given exposure to contacts, the logic
//	 * here is that a contact that had some risk of being infected when the contact
//	 * was made contributes to evidence that the subject is infected. On the other
//	 * hand if no people in the contacts were infected then the probability of being
//	 * infected is lower, but it only takes 1 positive. Therefore the log-liklihood
//	 * max value for contacts is used. This is looking at all contacts over the
//	 * infectious period
//	 */
//	default double getContactLogLikelihood() {
//		return this.getContactHistory().filter(c -> c.isDetected()).mapToDouble(c ->
//		// c.getProximityDuration() *
//		c.getParticipant(this)
//			.getDirectLogLikelihood(this.getTime(), infPeriod()))
//			.max().orElse(0);
//		// .average().orElse(0);
//	}
//
//	/**
//	 * The total log likelihood of disease is assembled from test results symptoms
//	 * and inferred from the (direct) log-likelihood of contacts being
//	 * 
//	 * @return
//	 */
//	@Value.Lazy
//	default double getTotalLogLikelihood() {
//		double tests = getTestLogLikelihood();
//		double symptoms = getSymptomLogLikelihood();
//		double contacts = getContactLogLikelihood();
//		return tests + symptoms + contacts;
//	}
//
//	/**
//	 * The log likelihood of infection as inferred from features of this individual
//	 * only. Including at the moment test results and symptoms only. This does not
//	 * include any probability inferred from contacts.
//	 */
//	@Value.Lazy
//	default double getDirectLogLikelihood() {
//		double tests = getTestLogLikelihood();
//		double symptoms = getSymptomLogLikelihood();
//		return tests + symptoms;
//		// TODO: Assessment of individual direct risk of disease needs to account for known previous infection.
//	}

	/**
	 * This is the risk that this individual is infectious today. This is the
	 * risk calculated from observable data such as reported symptoms, test
	 * results and detected contacts, as calculated by the risk model.
	 * @return
	 */
	default double getProbabilityInfectiousToday() {
		return this.getRiskModel().getProbabilityInfectiousToday();
	};

	/**
	 * All the tests done in the last presumed infectious period
	 * 
	 * @return
	 */
	default Stream<TestResult> getStillRelevantTests() {
		return ModelNav.history(this, infPeriod()).flatMap(ph -> ph.getTodaysTests().stream());
	}

	/**
	 * All the tests done in the last incubation period. The results here can rule
	 * out doing another test.
	 * 
	 * @return
	 */
	default Stream<TestResult> getRecentRuleOutTests() {
		return ModelNav.history(this, incubPeriod()).flatMap(ph -> ph.getTodaysTests().stream());
	}

	/**
	 * Most recent (relevant) test whether or not there is a result. This is not
	 * particularly deterministic and weakly assumes there is only one test per day.
	 */
	@Value.Lazy
	default Optional<TestResult> getLastTest() {
		return getStillRelevantTests().findFirst();
	}

	/**
	 * Most recent test with a result, so excluding pending results.
	 */
	@Value.Lazy
	default Optional<TestResult> getLastResult() {
		return getStillRelevantTests().filter(t -> t.isResultAvailable(this.getTime())).findFirst();
	}

	

	/**
	 * Reassemble the weighted contacts from the PersonHistory contact graph within
	 * the limit of the expected infectious period. This is the contact history as
	 * the app might collect it, except it does not filter for detected contacts
	 */
	default Stream<Contact> getContactHistory() {
		return ModelNav.history(this, infPeriod()).flatMap(ph -> Arrays.stream(ph.getTodaysContacts()));
	}

	/**
	 * Reassemble the exposures from the PersonHistory exposure graph within the
	 * infectious period. This is the true exposure history as the app might see it
	 * but the app would not usually know that these are exposures and not
	 * non-infectious contacts.
	 */
	default Stream<Exposure> getExposureHistory() {
		return ModelNav.history(this, infPeriod()).flatMap(ph -> Arrays.stream(ph.getTodaysExposures()));
	}

	/**
	 * A contact count for understanding the contact degree distribution. This is
	 * the number of contacts made today
	 */
	default long getContactCount() {
		return ModelNav.history(this).map(m -> m.getTodaysContacts().length).orElse(0);
	}

	/**
	 * A count of infectious contacts made today
	 */
	default long getExposureCount() {
		return ModelNav.history(this).map(m -> m.getTodaysExposures().length).orElse(0);
	}
	
	/** use with extreme caution: prefer the Optional ModelNav version. 
	 * This is only here for the CSV mapper. */
	default PersonHistory getYesterday() {
		return ModelNav.history(this).orElse(ReflectionUtils.nullProxy(PersonHistory.class));
	}

}