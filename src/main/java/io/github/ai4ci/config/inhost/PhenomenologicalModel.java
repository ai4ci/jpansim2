package io.github.ai4ci.config.inhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.config.execution.DemographicAdjustment;
import io.github.ai4ci.functions.Distribution;
import io.github.ai4ci.functions.SimpleDistribution;

/**
 * Configuration for an in‑host phenomenological model.
 *
 * <p>This interface captures the distributions and thresholds used by the
 * phenomenological in‑host model. It includes parameters for incubation,
 * timing of peak viral load and recovery, approximate peak viral load and
 * immune response, and immune waning.
 *
 * <p>Several entries are subject to demographic adjustment via
 * {@link io.github.ai4ci.config.execution.DemographicAdjustment.Phenomenological} and
 * may be modified at runtime by the execution configuration. In particular
 * the following getters are adjustable by demographic factors:
 * <ul>
 *   <li>{@link #getIncubationPeriod()}</li>
 *   <li>{@link #getPeakToRecoveryDelay()}</li>
 *   <li>{@link #getImmuneWaningHalfLife()}</li>
 * </ul>
 *
 * <p>Downstream uses include the initialiser
 * {@link io.github.ai4ci.flow.builders.DefaultInHostPhenomenologicalStateInitialiser}
 * which samples these distributions to build an
 * {@link io.github.ai4ci.abm.inhost.InHostPhenomenologicalState}.
 * It is also referenced by example code and tests.
 *
 * @author Rob Challen
 */
@Value.Immutable
//@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.FALSE, defaultImpl = ImmutablePhenomenologicalModel.class)
//@JsonSubTypes({
//  @Type(value=ImmutablePhenomenologicalModel.class, name="complete"), 
//  @Type(value=PartialPhenomenologicalModel.class, name="partial")
//})
@JsonSerialize(as = ImmutablePhenomenologicalModel.class)
@JsonDeserialize(as = ImmutablePhenomenologicalModel.class)
public interface PhenomenologicalModel extends InHostConfiguration,  DemographicAdjustment.Phenomenological<Distribution, Double> {
    
    

    /**
     * A sensible default parameterisation of the phenomenological model.
     *
     * <p>The default values provide a compact set of distributions used in
     * examples and tests. Downstream callers that rely on this simple default
     * include the default in‑host phenomenological initialiser and example
     * experiments.
     *
     * @see io.github.ai4ci.flow.builders.DefaultInHostPhenomenologicalStateInitialiser
     */
    public static ImmutablePhenomenologicalModel DEFAULT = ImmutablePhenomenologicalModel.builder()
            
            //.setSymptomCutoff(0.5)
            .setInfectiousnessCutoff(0.2)
            
            .setIncubationPeriod(SimpleDistribution.logNorm(4D, 2D))
            .setApproxPeakViralLoad( SimpleDistribution.unimodalBeta(0.5, 0.1))
            .setIncubationToPeakViralLoadDelay(SimpleDistribution.logNorm(2D, 1D))
            .setPeakToRecoveryDelay(SimpleDistribution.logNorm(6D,3D))
            
            .setApproxPeakImmuneResponse( SimpleDistribution.unimodalBeta(0.5, 0.1))
            .setImmuneWaningHalfLife(SimpleDistribution.logNorm(300D, 10D))
            .setPeakImmuneResponseDelay( SimpleDistribution.logNorm(20D, 4D) )
            
            .build();
    
    /**
     * Cutoff of the viral load or proxy used to determine infectiousness.
     *
     * <p>Value is used by initialisers and state code to determine whether an
     * individual is infectious at a given point on their viral load curve.
     *
     * @return infectiousness cutoff in the unit interval
     */
    Double getInfectiousnessCutoff();
    
    /**
     * Distribution for the incubation period (time from infection to onset).
     *
     * <p>This distribution is sampled by the default initialiser and by the
     * viral load calibration code. It is adjustable by
     * {@link DemographicAdjustment.Phenomenological}
     * implementations, allowing age dependent scaling of incubation timing.
     *
     * @return the incubation periods
     */
    Distribution getIncubationPeriod();
    
    /**
     * Approximate distribution for peak viral load or its proxy.
     *
     * <p>This value is used to calibrate the shape and amplitude of the viral
     * load trajectory. It is not directly adjusted by the demographic
     * adjustment component in the current design.
     *
     * @return the peak viral load
     */
    Distribution getApproxPeakViralLoad();
    
    /**
     * Delay from incubation/onset to peak viral load.
     *
     * <p>This distribution is sampled when constructing the viral load model
     * for an infected individual. It is not listed as adjustable in the
     * {@link DemographicAdjustment.Phenomenological}
     * interface and so remains constant across demographic groups unless
     * modified elsewhere.
     *
     * @return the delay from onset to peak viral load
     */
    Distribution getIncubationToPeakViralLoadDelay();
    
    /**
     * Delay from peak viral load to recovery.
     *
     * <p>The {@link DemographicAdjustment.Phenomenological}
     * may adjust this value via the {@code getPeakToRecoveryDelay} entry in
     * certain demographic configurations; callers should consult the
     * execution configuration's demographic adjustment before sampling if
     * age dependent values are required.
     *
     * @return the delay from peak to recovery
     */
    Distribution getPeakToRecoveryDelay();
    
    /**
     * Approximate distribution for peak immune response.
     *
     * <p>This is used in the phenomenological immune model to determine the
     * amplitude of the host immune response and its effect on viral decay.
     * It is not adjusted via the demographic adjustment interface in the
     * standard configuration.
     *
     * @return the peak immune response
     */
    Distribution getApproxPeakImmuneResponse();
    
    /**
     * Delay from infection/onset to peak immune response.
     *
     * <p>This distribution is sampled when creating an individual's immune
     * response trajectory and is used by the viral load/immune interaction
     * routines.
     *
     * @return the delay to peak immune response
     */
    Distribution getPeakImmuneResponseDelay();
    
    /**
     * Distribution for the half life of waning immunity.
     *
     * <p>This parameter may be adjusted by
     * {@link DemographicAdjustment.Phenomenological}
     * implementations to represent age dependent differences in immune waning.
     * Callers such as initialisers and immune decay updaters sample this
     * distribution when computing immunity loss over time.
     *
     * @return the immune waning half life
     */
    Distribution getImmuneWaningHalfLife();
    
}