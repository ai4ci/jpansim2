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
import io.github.ai4ci.util.Sampler;

/**
 * The person's current state within a single simulation timestep.
 *
 * <p>This interface represents the mutable, per-timestep view of a
 * person used during the update cycle. It holds transmissibility,
 * mobility, compliance and app use modifiers, transient quantities such
 * as importation exposure and immunisation dose, and references to the
 * <code>InHostModelState</code> and <code>RiskModel</code> instances
 * associated with the person. Implementations are typically generated
 * by the Immutables library and provide derived or lazy values for
 * properties that are computed from the person's history.</p>
 *
 * <p>Behavioural decisions (for example changes to mobility or
 * compliance) are made by classes in the {@code io.github.ai4ci.abm.behaviour}
 * package and may update the modifiers exposed by this interface. State
 * update helpers live in {@link io.github.ai4ci.flow.mechanics.StateUtils}
 * and the model navigation helpers used in many methods are available via
 * {@link ModelNav}.</p>
 *
 * <p>Notes about timing: many accessor methods consult the person's
 * historical records via {@link ModelNav#history(PersonState)}; where the
 * javadoc notes that a method is "one day behind" this reflects that the
 * history available during the update pass does not yet include the
 * values being computed for the current timestep.</p>
 *
 * @author Rob Challen
 */
@Value.Immutable
public interface PersonState extends PersonTemporalState {

    /**
     * The maximum exposure an individual can experience in a single day in
     * normalised units, where 1 is an average dose and 0 is the minimum.
     * Values greater than this are clipped to avoid undue numerical
     * instability when aggregating multiple exposures.
     */
    double MAX_EXPOSURE = 4;

    // Optional<Integer> getLastTestedTime();
    // Optional<Integer> getLastInfectedTime();
    // @Value.Default default Boolean isScreened() {return false;} 
    // Double getScreeningInterval();

    /**
     * An odds ratio of transmission that modifies the baseline probability of
     * transmission from this individual to another person. Behaviour models
     * may change this to represent interventions such as mask wearing.
     *
     * @return an odds ratio of transmissibility (1.0 = no change)
     */
    @Value.Default
    default double getTransmissibilityModifier() {
        return 1.0D;
    }

    /**
     * An odds ratio of mobility that modifies the baseline contact probability.
     * Behavioural code may change this modifier to reflect reduced or
     * increased contacts.
     *
     * @return an odds ratio of mobility (1.0 = no change)
     */
    @Value.Default
    default double getMobilityModifier() {
        return 1.0D;
    }

    /**
     * An odds ratio of compliance that modifies the baseline probability that
     * the person will follow guidance. This is a target for change by the
     * behaviour model and is sampled by {@link #isCompliant()} to produce the
     * daily compliant flag.
     *
     * @return an odds ratio of compliance (1.0 = no change)
     */
    @Value.Default
    default double getComplianceModifier() {
        return 1.0D;
    }

    /**
     * An odds ratio that modifies the baseline probability of becoming
     * infected (susceptibility). This is separate from the transmissibility
     * modifier to allow asymmetric interventions (for example protection of
     * the recipient but not the infector).
     *
     * @return an odds ratio of susceptibility (1.0 = no change)
     */
    @Value.Default
    default double getSusceptibilityModifier() {
        return 1.0D;
    }

    /**
     * An odds ratio that modifies the baseline probability the person is
     * using the smartphone app. Behavioural code may modify this to reflect
     * adoption or abandonment of the app.
     *
     * @return an odds ratio of app use (1.0 = no change)
     */
    @Value.Default
    default double getAppUseModifier() {
        return 1.0D;
    }

    /**
     * A viral exposure due to importation drivers. This value is transient
     * and is cleared by the updater after it has been consumed for the
     * current timestep; methods that refer to historical exposures will
     * therefore be working one day behind.
     *
     * @return importation exposure for the current timestep
     */
    @Value.Default
    default double getImportationExposure() {
        return 0D;
    }

    /**
     * An immunisation dose is a fraction of dormant immune cells that are
     * activated by any immunising exposure in the previous day. Setting this
     * non-zero implies the person received an immunising event at this time
     * point; it is cleared by the updater after each timestep.
     *
     * @return an immunisation dose (0.0 = none)
     */
    @Value.Default
    default double getImmunisationDose() {
        return 0D;
    }
    // TODO: Immunisation schedules and other time varying functions are not
    // implemented: {@link ModelUpdate#IMMUNISATION_PROTOCOL}

    /**
     * The in-host state model is updated during the update cycle by calling
     * {@link InHostModelState#update(Person, Sampler)}. The in-host model
     * provides viral load, severity and immune activity used throughout the
     * infection and behaviour logic.
     *
     * @return the in-host state model
     */
    InHostModelState<?> getInHostModel();
    
    /**
     * The risk model is updated automatically by the updater during the
     * second phase of the update cycle using {@link RiskModel#update()}. The
     * risk model estimates the probability the person is infectious today
     * from observable data (symptoms, tests, detected contacts) and is used
     * by behaviour models and testing strategies.
     *
     * @return the risk model (initialised from the person's entity).
     */
    @Value.Default default RiskModel getRiskModel() {
        return RiskModel.initialise(this.getEntity());
    }

    /**
     * A normalised severity index where 1 indicates symptom threshold (modulo
     * symptom sensitivity and specificity applied when observed).
     *
     * @return normalised severity from the in-host model
     */
    default double getNormalisedSeverity() {
        return getInHostModel().getNormalisedSeverity();
    }

    /** 
     * A normalised viral load index where 1 is the calibrated infectious
     * threshold.
     *
     * @return normalised viral load from the in-host model
     */
    default double getNormalisedViralLoad() {
        return getInHostModel().getNormalisedViralLoad();
    }

    /**
     * Is the person's internal viral load above the threshold for potential
     * infectivity today? Dead people are never infectious. The routine uses
     * the model base transmissibility baseline and returns true when that
     * baseline is positive for the current normalised viral load.
     *
     * @return true if the person is infectious today (true state)
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
     * Is this person newly exposed on this day? Defined as the first time a
     * non-zero exposure is recorded within one calibrated infectious period.
     * Uses {@link #isIncident(Predicate, int)} with the model's infective
     * duration.
     *
     * @return true if the person is newly exposed today, false otherwise
     */
    @Value.Lazy default boolean isIncidentExposure() {
        return isIncident(
            ph -> ph.getContactExposure() > 0,
            ModelNav.modelBase(this).getInfectiveDuration()
        );
    }
    
    /**
     * Is this person newly infectious on this day? Defined as the first time
     * the in-host viral load crosses the infectious threshold within one
     * calibrated infectious period. Uses {@link #isIncident(Predicate, int)}.
     *
     * @return true if newly infectious today, false otherwise
     */
    @Value.Lazy default boolean isIncidentInfection() {
        return isIncident(
            ph -> ph.isInfectious(),
            ModelNav.modelBase(this).getInfectiveDuration()
        );
    }
    
    /**
     * Is this person newly needing hospitalisation on this day or part of an
     * existing hospital episode. The window for considering a previous
     * hospitalisation is the model's symptom duration as returned by the
     * in-host calibration.
     *
     * @return true if newly needing hospitalisation today, false otherwise
     */
    @Value.Lazy default boolean isIncidentHospitalisation() {
        return isIncident(
            ph -> ph.isRequiringHospitalisation(),
            ModelNav.modelBase(this).getSymptomDuration()
        );
    }
    

    /**
     * Is the person's in-host state above the calibrated threshold for
     * exhibiting symptoms on this day. Symptom ascertainment error and app
     * reporting bias are applied to determine whether the person would be
     * observed as symptomatic when reporting is modelled.
     *
     * @return true if the person is symptomatic today (true state)
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
     * Is the person's in-host state above the threshold for requiring
     * hospitalisation on this day. The check is deterministic against the
     * in-host severity and the model's hospitalisation cutoff.
     *
     * @return true if hospitalisation is required today (true state)
     */
    @Value.Derived
    default boolean isRequiringHospitalisation() {
        if (this.isDead())
            return false;
        // Sampler rng = Sampler.getSampler();
        return getNormalisedSeverity() >= ModelNav.modelBase(this).getSeverityHospitalisationCutoff();
    }

    /** 
     * The person is dead. This checks the state machine for a terminal
     * dead state and also compares severity against the in-host death
     * threshold; either condition makes the person dead.
     *
     * @return true if the person is dead (true state)
     */
    @Value.Derived
    default boolean isDead() {
        // Sampler rng = Sampler.getSampler();
        if (this.getEntity().getStateMachine().getState().equals(NonCompliant.DEAD))
            return true;
        return getNormalisedSeverity() >= ModelNav.modelBase(this).getSeverityDeathCutoff();
    }
    
    /** 
     * Did this person die on this time step. This returns true when the
     * current derived dead state is true and there is no record of death in
     * yesterday's history.
     *
     * @return true if newly dead today, false otherwise
     */
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
     * The user has reported symptoms via the app. This combines the true
     * symptomatic state and app use for the day and therefore represents an
     * observed report; it is used in prevalence estimates and contact
     * notification.
     *
     * @return true if the person has reported symptoms today (observed)
     */
    @Value.Derived
    default boolean isReportedSymptomatic() {
        // assume user is not going to lie about symptoms
        if (!isSymptomatic()) return false;
        return this.isUsingAppToday();
    }

    /**
     * Is a person symptomatic for a number of days in a row? This consults the
     * person's history to determine whether they have been symptomatic for the
     * specified number of days in a row, including today. If days <= 0 this
     * returns false. When used during the update cycle the history is lagged by
     * one day.
     *
     * @param days the number of days (including today)
     * @return true if symptomatic for at least the specified consecutive days
     */
    default boolean isSymptomaticConsecutively(int days) {
        if (days <=0 ) return false;
        return isContinuation(h -> h.isSymptomatic(), days-1);
    }

    /**
     * Probability of contact given a fully mobile partner on a per day basis.
     * This combines baseline mobility with the individual's mobility modifier
     * and is used by the contact generation process. Behavioural models may
     * update the modifier; see {@code io.github.ai4ci.abm.behaviour}.
     *
     * @return probability of contact with a fully mobile partner
     */
    default double getAdjustedMobility() {
        double tmp = Conversions.scaleProbabilityByOR(
                this.getEntity().getBaseline().getMobilityBaseline(),
                this.getMobilityModifier()
        );
        return tmp;
    };

    /** 
     * A derived value of the decrease in mobility from the baseline. This is a
     * probability difference (baseline - current).
     *
     * @return the absolute mobility decrease
     */
    default double getAbsoluteMobilityDecrease() {
        return this.getEntity().getBaseline().getMobilityBaseline() - getAdjustedMobility();
    }
    
    /**
     * A derived value of the probability of app use. This combines the
     * baseline adoption probability and the individual's app use modifier.
     * Behavioural adjustments to app use should modify the app use modifier.
     *
     * @return probability of using the app today
     */
    default double getAdjustedAppUseProbability() {
        double tmp = Conversions.scaleProbabilityByOR(this.getEntity().getBaseline().getAppUseProbability(),
                this.getAppUseModifier());
        return tmp;
    };

    /**
     * A derived value of the probability of transmission given an encounter
     * with a completely unprotected person. This combines the model's
     * transmissibility baseline (which depends on viral load) with global and
     * individual transmissibility modifiers.
     *
     * @return probability of transmission in an unprotected contact
     */
    default double getAdjustedTransmissibility() {
        double tmp = Conversions.scaleProbabilityByOR(
                ModelNav.modelBase(this).getTransmissibilityBaseline(this.getNormalisedViralLoad()),
                ModelNav.modelState(this).getTransmissibilityModifier() // sim wide
                        * this.getTransmissibilityModifier()); // individual
        return tmp;
    };
    
    /**
     * A derived value of the probability of compliance to guidance. This is a
     * combination of the baseline probability and the individual's compliance
     * modifier. Behaviour models may modify the modifier; the daily observed
     * compliance is sampled from this value by {@link #isCompliant()}.
     *
     * @return probability of compliance today
     */
    default double getAdjustedCompliance() {
        return Conversions.scaleProbabilityByOR(this.getEntity().getBaseline().getComplianceBaseline(),
                this.getComplianceModifier());
    };
    
    /**
     * A derived value of the decrease in compliance from the baseline.
     *
     * @return the absolute compliance decrease (baseline - current)
     */
    default double getAbsoluteComplianceDecrease() {
        return this.getEntity().getBaseline().getComplianceBaseline() - getAdjustedCompliance();
    }

    /**
     * Determine whether the most recent test is of a particular result. The
     * check consults the most recent relevant tests window and compares the
     * test result from the previous day (history is lagged during updates).
     *
     * @param result the result to compare against
     * @return true if the most recent test result equals the specified result
     */
    default boolean isLastTestExactly(Result result) {
        // This is looking at the day behind for test results
        // because at the point this is run in the update cycle the
        // history has not yet been updated.
        Optional<Result> o = this.getLastTest().map(tr -> tr.resultOnDay(this.getTime()));
        return o.isPresent() && o.get().equals(result);
    }

    /**
     * Is the person compliant to guidance on this day? This is sampled from
     * {@link #getAdjustedCompliance()} and cached for the day. Dead people
     * cannot be compliant.
     *
     * @return true if compliant today
     */
    @Value.Derived
    default boolean isCompliant() {
        if (isDead())
            return false;
        Sampler rng = Sampler.getSampler();
        return rng.bern(this.getAdjustedCompliance());
    }

    /**
     * Is the person using the app on this day? Sampled from
     * {@link #getAdjustedAppUseProbability()} and cached for the day. Dead
     * people cannot use the app.
     *
     * @return true if using the app today
     */
    @Value.Derived
    default boolean isUsingAppToday() {
        if (isDead()) return false;
        Sampler rng = Sampler.getSampler();
        return rng.bern(this.getAdjustedAppUseProbability());
    }

    @Value.Lazy
    /**
     * A simple textual representation of the person's current behavioural
     * state. Derived from the entity's state machine state name and intended
     * primarily for export or logging.
     *
     * @return the behaviour state name
     */
    default String getBehaviour() {
        return this.getEntity().getStateMachine().getState().getName();
    }

    /**
     * A normalised viral load dose computed as the sum of all exposures over
     * the course of a day. The value is clipped at {@link #MAX_EXPOSURE} to
     * avoid extreme values from uncontrolled growth. This value is derived
     * from the contact network and the in-host models and is computed from
     * the person's history (thus it is one day behind during the update
     * pass).
     *
     * @return normalised contact exposure for the day
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

    /**
     * A derived value of the total exposure for the day, equal to the sum of
     * contact and importation exposures. This reflects the total viral dose
     * that the in-host model will observe for the next timestep.
     *
     * @return total exposure for the day
     */
    public default double getTotalExposure() {
        return getContactExposure() + getImportationExposure();
    }

    /**
     * Has the person had a test of a specific type within the incubation
     * period. This is used by testing strategies to avoid redundant tests.
     * The search is performed over the lagged history window and therefore
     * may not include tests sampled during the current update pass.
     *
     * @param type the test type
     * @return true if a test of the specified type exists in the incubation window
     */
    default boolean isRecentlyTested(Type type) {
        return isRecentlyTested(type,incubPeriod());
    };
    
    /**
     * Look back for tests of the specified type within the given number of
     * days. This function is used by testing strategies to determine if a
     * new test is required.
     *
     * @param type the test type
     * @param days the number of days to look back
     * @return true if a matching test exists within the lookback window
     */
    default boolean isRecentlyTested(Type type, int days) {
        return ModelNav.history(this, days).flatMap(ph -> ph.getTodaysTests().stream())
            .filter(t -> type.params().getTestName().equals(t.getTestParams().getTestName()))
            .findFirst().isPresent();
    };
    
    /**
     * Estimate local prevalence for an individual as the average presumed
     * probability infectious across detected contacts. If there are no
     * detected contacts a small non-zero floor is returned to avoid zero
     * prevalence issues in downstream calculations.
     *
     * @return presumed local prevalence (observed view)
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
    
    /** 
     * How many people in this person's recent contacts are infectious 
     * regardless of whether the contact is detected. This integrates over
     * approximately one infectious period and therefore is lagged.
     *
     * @return true local prevalence measured as fraction infectious among contacts
     */
    @Value.Lazy
    default double getTrueLocalPrevalence() {
        return getContactHistory().map(c -> c.getParticipantState(this))
                .mapToDouble(ph -> ph.isInfectious() ? 1.0 : 0.0).average().orElse(0D);
    }

    /**
     * The risk this individual is infectious today as estimated by the risk
     * model. This value comes from {@link RiskModel#getProbabilityInfectiousToday()}.
     *
     * @return probability the person is infectious today (model estimate)
     */
    default double getProbabilityInfectiousToday() {
        return this.getRiskModel().getProbabilityInfectiousToday();
    };

    /**
     * All tests in the last presumed infectious period (including pending
     * results). These inform simple rules used by strategies to infer
     * current infection status.
     *
     * @return stream of recent tests (most recent first)
     */
    default Stream<TestResult> getStillRelevantTests() {
        return ModelNav.history(this, infPeriod()).flatMap(ph -> ph.getTodaysTests().stream());
    }
    
    
    
    /**
     * All the tests done in the last incubation period. These results can be
     * used to rule out further testing where recent negatives exist.
     *
     * @return stream of tests within the incubation window
     */
    default Stream<TestResult> getRecentRuleOutTests() {
        return ModelNav.history(this, incubPeriod()).flatMap(ph -> ph.getTodaysTests().stream());
    }

    /**
     * Most recent (relevant) test whether or not there is a result. This
     * returns the most recently sampled test even if its result is pending.
     *
     * @return most recent test (if any)
     */
    @Value.Lazy
    default Optional<TestResult> getLastTest() {
        return getStillRelevantTests().findFirst();
    }

    /**
     * The single most recent test with an available result, excluding
     * pending results.
     *
     * @return most recent test result if available
     */
    @Value.Lazy
    default Optional<TestResult> getLastResult() {
        return getStillRelevantTests().filter(t -> t.isResultAvailable(this.getTime())).findFirst();
    }

    

    /**
     * Reassemble the weighted contacts from the history within the expected
     * infectious period. This returns the true contact history (not filtered
     * for detection) and is therefore not the app's observed contact list.
     *
     * @return stream of past contacts within the infectious window
     */
    default Stream<Contact> getContactHistory() {
        return ModelNav.history(this, infPeriod()).flatMap(ph -> Arrays.stream(ph.getTodaysContacts()));
    }

    /**
     * Reassemble the exposures from the history within the infectious
     * period. This is the true exposure history and not necessarily what the
     * app would observe.
     *
     * @return stream of exposures within the infectious window
     */
    default Stream<Exposure> getExposureHistory() {
        return ModelNav.history(this, infPeriod()).flatMap(ph -> Arrays.stream(ph.getTodaysExposures()));
    }

    /**
     * A contact count for the current day derived from the person's history.
     * This is the true number of contacts made today, not an observed count.
     *
     * @return true contact count for today
     */
    default long getContactCount() {
        return ModelNav.history(this).map(m -> m.getTodaysContacts().length).orElse(0);
    }

    /**
     * A count of true infectious contacts made today (not filtered by
     * detection). Uses the day's exposures list length.
     *
     * @return count of infectious exposures today
     */
    default long getExposureCount() {
        return ModelNav.history(this).map(m -> m.getTodaysExposures().length).orElse(0);
    }
    
    /**
     * Abstract detection of an incident event. This routine determines if an
     * event recorded in the current state is the first occurrence within a
     * left-censored window of length {@code limit} days. It relies on the
     * presence of a copied record in the entity's history.
     *
     * @param test predicate testing the event on a temporal state
     * @param limit lookback window in days (0 => today only)
     * @return true if the event is incident within the window
     */
    default boolean isIncident(Predicate<PersonTemporalState> test, int limit) {
        if (limit <= 0) return test.test(this);
        if (!test.test(this)) return false;
        // This assumes that a positive at the start of the simulation is new. 
        if (!this.getEntity().getCurrentHistory().isPresent()) return true;
        if (this.getEntity().getCurrentHistory().get().isRecently(test, limit-1)) return false;
        return true;
    }
    
    /** 
     * Has the event happened continuously for at least {@code limit} days.
     *
     * @param test predicate testing the event on a temporal state
     * @param limit the number of previous days to require continuity
     * @return true if the event has continued for at least {@code limit} days
     */
    default boolean isContinuation(Predicate<PersonTemporalState> test, int limit) {
        if (limit < 0) return false;
        if (limit == 0) return test.test(this);
        if (!test.test(this)) return false;
        if (!this.getEntity().getCurrentHistory().isPresent()) return false;
        if (this.getEntity().getCurrentHistory().get().isRecently(test, limit-1)) return false;
        return true;
    }

    /**
     * A derived value of immune activity from the in-host model. Range is
     * typically between 0 and 1.
     *
     * @return immune activity derived from the in-host model
     */
    default double getImmuneActivity() {
        return this.getInHostModel().getImmuneActivity();
    };

}
