package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
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
import io.github.ai4ci.util.Sampler;

/**
 * The person's current state is updated in each round of the simulation. It 
 * holds data on transmission, mobility, compliance, app use, and transient 
 * values such as exposure to virus and immunisation doses. It also holds the 
 * in host viral load model which is also updated on each round of simulation.
 * The current state also includes a risk model which analyses current observed
 * data about the patient's history.
 */
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
	 * transmission from this individual to another person. This is a target for
	 * change by the behaviour model.
	 */
	@Value.Default
	default double getTransmissibilityModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio of mobility that modifies the baseline contact probability. 
	 * This is a target for change by the behaviour model.
	 */
	@Value.Default
	default double getMobilityModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio of compliance that modifies the baseline compliance probability
	 * that the person is going to follow guidance. This is a target for change by the 
	 * behaviour model.
	 */
	@Value.Default
	default double getComplianceModifier() {
		return 1.0D;
	}

	/**
	 * An odds ratio of that modifies the baseline probability of transmission from
	 * another person to this one. This is a target for change by the behaviour
	 * model. This does not include permanent changes due to immunity (which is
	 * handled by the in host state model). Is this really different from 
	 * the transmissibility modifier? It allows asymmetry for protection if infector
	 * is wearing a mask and infectee is not for example, or for intervention 
	 * that blocks transmission onwards but not infection by others. 
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
	 * varying function representing external drivers. This is automatically zeroed
	 * during update to make sure that it is not inherited from previous state. It 
	 * is then 
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

	/** The in-host state model is automatically updated during the update cycle
	 * (after contacts and tests updated in history) by calling the 
	 * InHostModelState.update() method
	 */
	InHostModelState<?> getInHostModel();
	
	/** The risk model is updated automatically by the Updater during the 
	 * second phase, after tests and contacts have been updated (using the 
	 * RiskModel.update()
	 */
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
	 * infectivity today? This is defined as having a non zero baseline for 
	 * transmission. 
	 */
	@Value.Derived
	default boolean isInfectious() {
		if (this.isDead())
			return false;
		return 
				ModelNav.modelBase(this).getTransmissibilityBaseline(
					getNormalisedViralLoad()
				) > 0;
	}
	
	/**
	 * Is this person newly exposed on this day? defined as the first time a non zero
	 * exposure is recorded within one calibrated infectious period.
	 */
	@Value.Lazy default boolean isIncidentExposure() {
		return isIncident(
			ph -> ph.getContactExposure() > 0,
			ModelNav.modelBase(this).getInfectiveDuration()
		);
	}
	
	/**
	 * Is this person newly infectious on this day? This is truth and may not
	 * be observed.
	 */
	@Value.Lazy default boolean isIncidentInfection() {
		return isIncident(
			ph -> ph.isInfectious(),
			ModelNav.modelBase(this).getInfectiveDuration()
		);
	}
	
	/**
	 * Is this person newly needing hospitalisation on this day, or is this part of 
	 * the same hospitalisation episode (as defined by recent hospitalisation
	 * within the average duration of symptoms). N.B. this duration should probably be
	 * an estimated quantity but it is actually calibrated from the in host model. 
	 */
	@Value.Lazy default boolean isIncidentHospitalisation() {
		return isIncident(
			ph -> ph.isRequiringHospitalisation(),
			ModelNav.modelBase(this).getSymptomDuration()
		);
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
	
	/** Did this person die on this time step. */
	@Value.Lazy default boolean isIncidentDeath() {
		if (!this.isDead()) return false;
		if (ModelNav.history(this).map(ph -> ph.isDead()).orElse(Boolean.FALSE)) {
			//was dead yesterday:
			return false;
		} else {
			// was not dead yesterday
			return true;
		}
	}

	/**
	 * The user has reported symptoms via the app. This means their symptom state
	 * can inform local estimates of prevalence.
	 */
	@Value.Derived
	default boolean isReportedSymptomatic() {
		// assume user is not going to lie about symptoms
		if (!isSymptomatic()) return false;
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
		if (days <=0 ) return false;
		return isContinuation(h -> h.isSymptomatic(), days-1);
	}

	/**
	 * Probability of contact given a fully mobile partner on a per day basis. The
	 * joint probability between 2 people within a social network will define the
	 * likelihood of a contact (Plus some randomness). A high mobility means a high
	 * probability of contact.
	 */
	default double getAdjustedMobility() {
		double tmp = Conversions.scaleProbabilityByOR(
				this.getEntity().getBaseline().getMobilityBaseline(),
				this.getMobilityModifier()
		);
		return tmp;
	};

	/** Derived value for export.*/
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
	 * Plus and most importantly internal viral load
	 */
	default double getAdjustedTransmissibility() {
		double tmp = Conversions.scaleProbabilityByOR(
				ModelNav.modelBase(this).getTransmissibilityBaseline(this.getNormalisedViralLoad()),
				ModelNav.modelState(this).getTransmissibilityModifier() // sim wide
						* this.getTransmissibilityModifier()); // individual
		return tmp;
	};
	
	/**
	 * A probability that a person is compliant to guidance on any give day.
	 */
	default double getAdjustedCompliance() {
		return Conversions.scaleProbabilityByOR(this.getEntity().getBaseline().getComplianceBaseline(),
				this.getComplianceModifier());
	};
	
	/** Derived value for export */
	default double getAbsoluteComplianceDecrease() {
		return this.getEntity().getBaseline().getComplianceBaseline() - getAdjustedCompliance();
	}

	/**
	 * It determines if the most recent test regardless of whether there is a result
	 * or it is still pending. This is how it needs to be for determining whether or
	 * not to do another test and is used in the testing strategies. It is a bad
	 * choice for anything else. If multiple tests were done recently this is
	 * not particularly determininstic.
	 */
	default boolean isLastTestExactly(Result result) {
		// This is looking at the day behind for test results
		// because at the point this is run in the update cycle the
		// history has not yet been updated.
		Optional<Result> o = this.getLastTest().map(tr -> tr.resultOnDay(this.getTime()));
		return o.isPresent() && o.get().equals(result);
	}

	@Value.Derived
	default boolean isCompliant() {
		if (isDead())
			return false;
		Sampler rng = Sampler.getSampler();
		return rng.bern(this.getAdjustedCompliance());
	}

	@Value.Derived
	default boolean isUsingAppToday() {
		if (isDead()) return false;
		Sampler rng = Sampler.getSampler();
		return rng.bern(this.getAdjustedAppUseProbability());
	}

	@Value.Lazy
	/** This persons current behaviour as a String. Mostly for export. */
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
	 */
	@Value.Lazy
	default double getContactExposure() {
		return Math.min(MAX_EXPOSURE, 
				ModelNav.history(this).stream()
				.flatMap(ph -> 
					Arrays.stream(ph.getTodaysExposures()))
						.mapToDouble(p -> p.getExposure())
						.sum());
	}

	/** Exposure due to contact and importation */
	public default double getTotalExposure() {
		return getContactExposure() + getImportationExposure();
	}

	/**
	 * Has the person had a test of type X in the (presumed) incubation period
	 * and therefore another test may be unnecessary.
	 */
	default boolean isRecentlyTested(Type type) {
		return isRecentlyTested(type,incubPeriod());
	};
	
	/**
	 * Looks at whether there are any tests of specified type within the last x
	 * days.
	 */
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
		double prev = getContactHistory()
				.filter(c -> c.isDetected())
				.map(c -> c.getParticipantState(this))
				.mapToDouble(ph -> ph.getProbabilityInfectiousToday()).average().orElse(0D);
		// The local prevalence will be problematic if it is zero
		return prev != 0 ? prev : 0.0001;		
	}
	
	/** How many people in this person's recent contacts are infectious 
	 * regardless of whether on not the contact is detected. This integrates 
	 * over approx one infectious period (0.95 quantile) so will be lagged.  
	 */
	@Value.Lazy
	default double getTrueLocalPrevalence() {
		return getContactHistory().map(c -> c.getParticipantState(this))
				.mapToDouble(ph -> ph.isInfectious() ? 1.0 : 0.0).average().orElse(0D);
	}

	/**
	 * This is the risk that this individual is infectious today. This is the
	 * risk calculated from observable data such as reported symptoms, test
	 * results and detected contacts, as calculated by the risk model.
	 */
	default double getProbabilityInfectiousToday() {
		return this.getRiskModel().getProbabilityInfectiousToday();
	};

	/**
	 * All the tests done in the last presumed infectious period (including
	 * those with pending results). These tests inform our view as to whether 
	 * this person is currently infected, in a simplistic
	 * way. E.g. if one of these is positive for example. This is a sequential
	 * list reversed in history, with most recent first.
	 */
	default Stream<TestResult> getStillRelevantTests() {
		return ModelNav.history(this, infPeriod()).flatMap(ph -> ph.getTodaysTests().stream());
	}
	
	

	/**
	 * All the tests done in the last incubation period. The results here can rule
	 * out doing another test.
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
	 * Most a single most recent test with a result, so excludes pending results.
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
	 * the true number of contacts made today, not observed.
	 */
	default long getContactCount() {
		return ModelNav.history(this).map(m -> m.getTodaysContacts().length).orElse(0);
	}

	/**
	 * A count of the true number infectious contacts made today
	 */
	default long getExposureCount() {
		return ModelNav.history(this).map(m -> m.getTodaysExposures().length).orElse(0);
	}
	
	/** 
	 * Has an event happened for the first time within an exclusion period of 
	 * X days (left censored at the beginning of the timeseries)? The event 
	 * must be recorded in both PersonState and copied to 
	 * PersonHistory (via being declared in PersonTemporalState) by the HistoryMapper.
	 */
	default boolean isIncident(Predicate<PersonTemporalState> test, int limit) {
		if (limit <= 0) return test.test(this);
		if (!test.test(this)) return false;
		// This assumes that a positive at the start of the simulation is new. 
		if (!this.getEntity().getCurrentHistory().isPresent()) return true;
		if (this.getEntity().getCurrentHistory().get().isRecently(test, limit-1)) return false;
		return true;
	}
	
	/** Has the event happened continuously for at least X days. If there is
	 * not enough time at the beginning of the time series this will be false
	 */
	default boolean isContinuation(Predicate<PersonTemporalState> test, int limit) {
		if (limit < 0) return false;
		if (limit == 0) return test.test(this);
		if (!test.test(this)) return false;
		if (!this.getEntity().getCurrentHistory().isPresent()) return false;
		if (this.getEntity().getCurrentHistory().get().isRecently(test, limit-1)) return false;
		return true;
	}

	default double getImmuneActivity() {
		return this.getInHostModel().getImmuneActivity();
	};

}