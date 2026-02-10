package io.github.ai4ci.config.execution;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.config.Scale;
import io.github.ai4ci.functions.FixedValueFunction;
import io.github.ai4ci.functions.ImmutableEmpiricalFunction;
import io.github.ai4ci.functions.ImmutableMathematicalFunction;
import io.github.ai4ci.functions.LinkFunction;
import io.github.ai4ci.functions.SimpleFunction;

/**
 * Age-based adjustment values used to modify baseline model parameters.
 *
 * <p>Main purpose: provide a small set of interfaces that describe how various
 * model parameters may be adjusted by demographic strata (for example age).
 * Implementations supply distribution-like or numeric transforms that are
 * consulted by initialisers and model code to produce age-dependent values.
 *
 * <p>Downstream uses: referenced by {@link io.github.ai4ci.config.execution.ExecutionConfiguration},
 * in-host configuration types such as {@link io.github.ai4ci.config.inhost.PhenomenologicalModel}
 * and {@link io.github.ai4ci.config.inhost.MarkovStateModel}, and by initialisers in
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser}.
 *
 * @author Rob Challen
 */
public interface DemographicAdjustment {
    
    /**
     * An empty partial adjustment that applies no demographic modification.
     *
     * <p>Used as a default when no age-dependent adjustments are required.
     */
    public static PartialDemographicAdjustment EMPTY = 
            PartialDemographicAdjustment.builder().build();
    
    /**
     * A sensible default set of age-dependent adjustments used in examples and tests.
     *
     * <p>This partial provides example functions for contact probability,
     * clinical severity and immune waning that are suitable for demonstrations
     * and unit tests.
     */
    public static PartialDemographicAdjustment AGE_DEFAULT =
            PartialDemographicAdjustment.builder()
            .setContactProbability(
                    ImmutableEmpiricalFunction.builder()
                        .setX(0,5,15,25,45,75)
                        .setY(2,1,1.5,1.25,0.8,1.2)
                        .setLink(LinkFunction.LOG)
                        .build()    
                )
            .setAppUseProbability(FixedValueFunction.ofOne())
            .setComplianceProbability(FixedValueFunction.ofOne())
            .setMaximumSocialContactReduction(FixedValueFunction.ofOne())
            .setIncubationPeriod(FixedValueFunction.ofOne())
            .setPeakToRecoveryDelay(
                    ImmutableEmpiricalFunction.builder()
                        .setX(0,5,15,25,45,65)
                        .setY(1,0.5,0.5,0.75,1,2)
                        .setLink(LinkFunction.LOG)
                        .build()
            )
            .setImmuneWaningHalfLife(
                    ImmutableEmpiricalFunction.builder()
                        .setX(0,5,15,25,45,65,85)
                        .setY(1,2,2,1.5,1,0.5,0.25)
                        .setLink(LinkFunction.LOG)
                        .build()
            )
            .setCaseHospitalisationRate(
                    ImmutableMathematicalFunction.builder()
                        // baseline 45, doubles every 15 years of age
                        .setFXExpression("exp((x-45)/15*lg(2))")
                        .build()
            )
            .setCaseFatalityRate(
                    ImmutableMathematicalFunction.builder()
                        // baseline 45, doubles every 10 years of age
                        .setFXExpression("exp((x-45)/10*lg(2))")
                        .build()
            )
            .setAsymptomaticFraction(
                    ImmutableMathematicalFunction.builder()
                        // baseline 45, doubles every 20 years of age (inverted)
                        .setFXExpression("exp(-(x-45)/20*lg(2))")
                        .build()
            )
            .build();
            
    
    @Value.Immutable @Partial
    @JsonSerialize(as = PartialDemographicAdjustment.class)
    @JsonDeserialize(as = PartialDemographicAdjustment.class)
    /**
     * A combined partial adjustment usable for different configuration facets.
     *
     * This immutable partial implements the execution, phenomenological and
     * markov facets so a single object may be applied where a complete set of
     * age-dependent adjustments is required.
     */
    public interface _PartialDemographicAdjustment extends 
        DemographicAdjustment.Execution<SimpleFunction, SimpleFunction>,
        DemographicAdjustment.Phenomenological<SimpleFunction, SimpleFunction>,
        DemographicAdjustment.Markov<SimpleFunction, SimpleFunction>,
        Serializable
    {
        // Convenience self reference used by the Immutables builder API.
    	/**
    	 * Convenience self reference used by the Immutables builder API.
    	 * @return this instance
    	 */
        default _PartialDemographicAdjustment self() {return this;}
    }
    
    /**
     * Age dependent values in the general execution configuration.
     *
     * <p>Implementations supply either distribution-like objects or numeric
     * transforms that are interpreted by execution code to produce age-specific
     * parameters such as case rates and contact odds.
     * 
     * @param <DIST> the type used to represent distribution-like adjustments (for example a function or empirical distribution)
     * @param <NUMERIC> the type used to represent numeric adjustments (for example a simple scalar or a function that produces a numeric value)
     */
    public static interface Execution<DIST,NUMERIC> {
        /**
         * Asymptomatic fraction by age group, interpreted on the scale defined
         * by the {@link io.github.ai4ci.config.Scale} annotation used on callers.
         *
         * @return a numeric-like value representing the asymptomatic fraction
         */
        @Scale(Scale.ScaleType.ODDS) NUMERIC getAsymptomaticFraction();

        /**
         * Case hospitalisation rate by age group.
         *
         * @return a numeric-like value representing the hospitalisation rate
         */
        @Scale(Scale.ScaleType.ODDS) NUMERIC getCaseHospitalisationRate();

        /**
         * Case fatality rate by age group.
         *
         * @return a numeric-like value representing the fatality rate
         */
        @Scale(Scale.ScaleType.ODDS) NUMERIC getCaseFatalityRate();

        /**
         * Contact probability modifier by age group.
         *
         * @return a distribution-like object used to modify contact odds
         */
        @Scale(Scale.ScaleType.ODDS) DIST getContactProbability();

        /**
         * Compliance probability by age group (for example adherence to isolation).
         *
         * @return a distribution-like object representing compliance probability
         */
        @Scale(Scale.ScaleType.ODDS) DIST getComplianceProbability();

        /**
         * App use probability by age group (digital contact tracing uptake).
         *
         * @return a distribution-like object representing app use probability
         */
        @Scale(Scale.ScaleType.ODDS) DIST getAppUseProbability();

        /**
         * Maximum social contact reduction achievable by this age group under
         * behavioural interventions.
         *
         * @return a distribution-like object expressing the maximum contact reduction
         */
        @Scale(Scale.ScaleType.ODDS) DIST getMaximumSocialContactReduction();
    } 
    
    /**
     * Age dependent values in the in-host phenomenological model configuration.
     *
     * <p>These entries adjust time-scales used by phenomenological in-host models
     * such as incubation and recovery delays.
     * 
     * @param <DIST> the type used to represent distribution-like adjustments (for example a function or empirical distribution)
     * @param <NUMERIC> the type used to represent numeric adjustments (for example a simple scalar or a function that produces a numeric value)
     */
    public static interface Phenomenological<DIST,NUMERIC> {
        /**
         * Incubation period distribution or modifier by age.
         *
         * @return a distribution-like object representing incubation timing
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getIncubationPeriod();

        /**
         * Delay from peak viral load to recovery by age.
         *
         * @return a distribution-like object representing peak-to-recovery delay
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getPeakToRecoveryDelay();

        /**
         * Immune waning half-life by age.
         *
         * @return a distribution-like object representing immune waning half-life
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getImmuneWaningHalfLife();
    }
    
    /**
     * Age dependent values in the in-host Markov model configuration.
     *
     * <p>These entries are used by Markov-style in-host implementations to
     * parameterise durations and time-scales.
     * @param <DIST> the type used to represent distribution-like adjustments (for example a function or empirical distribution)
     * @param <NUMERIC> the type used to represent numeric adjustments (for example a simple scalar or a function that produces a numeric value)
     */
    public static interface Markov<DIST,NUMERIC> {
        /**
         * Incubation period distribution or modifier by age.
         *
         * @return a distribution-like object representing incubation timing
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getIncubationPeriod();

        /**
         * Immune waning half-life by age.
         *
         * @return a distribution-like object representing immune waning half-life
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getImmuneWaningHalfLife();

        /**
         * Infectious duration by age.
         *
         * @return a distribution-like object representing infectious duration
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getInfectiousDuration();

        /**
         * Symptom duration by age.
         *
         * @return a distribution-like object representing symptom duration
         */
        @Scale(Scale.ScaleType.FACTOR) DIST getSymptomDuration();
        
    }
    
}