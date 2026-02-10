package io.github.ai4ci.config.inhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.flow.builders.DefaultInHostStochasticStateInitialiser;
import io.github.ai4ci.functions.Distribution;
import io.github.ai4ci.functions.SimpleDistribution;

/**
 * Parameters for a stochastic in‑host viral dynamics model.
 *
 * <p>This interface defines the numerical inputs and distributions used to
 * initialise the discrete stochastic in‑host model implemented by
 * {@link io.github.ai4ci.abm.inhost.InHostStochasticState}. Values here are
 * sampled by the default initialiser
 * {@link DefaultInHostStochasticStateInitialiser}
 * to construct an initial {@code InHostStochasticState} for each infected
 * individual. The parameters control initial pool sizes, immune capacity and
 * activation dynamics, rates of viral replication and infection, and the
 * calibration cutoff for what constitutes a disease‑level viral load.
 *
 * <p>Keep descriptions concise: the initialiser samples distributional
 * entries (see methods returning {@link Distribution}) and copies scalar
 * parameters directly into the initial in‑host state. Downstream updates are
 * performed by {@link io.github.ai4ci.abm.inhost.InHostStochasticState#update(Sampler,double,double)}
 * which uses these initial values to govern stochastic transitions.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableStochasticModel.class)
@JsonDeserialize(as = ImmutableStochasticModel.class)
public interface StochasticModel extends InHostConfiguration {
    
    /**
     * A sensible default configuration for the stochastic in‑host model.
     *
     * <p>This instance provides default distributions and scalars used in
     * examples and tests. The {@link DefaultInHostStochasticStateInitialiser}
     * will sample the distributional entries from this object when creating a
     * fresh {@link io.github.ai4ci.abm.inhost.InHostStochasticState}.
     */
    public static ImmutableStochasticModel DEFAULT = ImmutableStochasticModel.builder()
            .setTargetCellCount(10000)
            .setImmuneTargetRatio( SimpleDistribution.logNorm(1D, 0.1))
            .setImmuneActivationRate( SimpleDistribution.logNorm(1D, 0.1))
            .setImmuneWaningRate( SimpleDistribution.logNorm(1D/150, 0.01))
            .setInfectionCarrierProbability( SimpleDistribution.point(0D))
            .setTargetRecoveryRate( SimpleDistribution.logNorm( 1D/7, 0.1) )
            .setBaselineViralInfectionRate( 1D )
            .setBaselineViralReplicationRate( 4D )
            .setVirionsDiseaseCutoff( 1000 )
            // .setTargetSymptomsCutoff( 0.2 )
            .build();
    
    /**
     * Total number of target cells modelled for a single host.
     *
     * <p>The initialiser places this value into the created
     * {@link io.github.ai4ci.abm.inhost.InHostStochasticState} as both the
     * total target pool and the initial susceptible pool. It therefore sets
     * the scale of the infection: larger pools reduce per‑cell exposure for a
     * given number of virions and change the saturation behaviour in the
     * coverage calculations used during infection updates.
     *
     * @return the initial total target cell count
     */
    Integer getTargetCellCount();
    
    /**
     * Distribution for the immune to target cell ratio.
     *
     * <p>The initialiser samples this distribution to set the starting total
     * immune count via floor(targets * ratio). This controls immune capacity
     * relative to target cells and influences neutralisation and clearance
     * probabilities in subsequent stochastic updates.
     *
     * @return a distribution producing immune:target ratios used at initialisation
     */
    Distribution getImmuneTargetRatio();
    
    /**
     * Distribution for the immune activation (priming) rate.
     *
     * <p>This distribution is sampled by the initialiser to set the host's
     * propensity to prime immune cells in response to exposed or infected
     * targets. A higher sampled value leads to faster priming and earlier
     * immune activity during {@code InHostStochasticState.update}.
     *
     * @return a distribution for immune priming rates
     */
    Distribution getImmuneActivationRate();
    
    /**
     * Distribution for the waning rate of active immune cells.
     *
     * <p>The initialiser samples this entry to determine how quickly active
     * effectors transition to senescent states. Faster waning reduces long‑
     * term immune activity and can increase susceptibility to reinfection in
     * the simulation.
     *
     * @return a distribution for immune waning rates
     */
    Distribution getImmuneWaningRate();
    
    /**
     * Distribution giving the probability an exposed cell becomes a chronic carrier.
     *
     * <p>Sampled at initialisation, this controls the per‑cell probability that
     * an exposed target will evade clearance and enter a persistent reservoir.
     * Even if the default is zero, non‑zero values alter long‑term infection
     * dynamics and the pool of targets available for future replication.
     *
     * @return a distribution for chronic infection carrier probability
     */
    Distribution getInfectionCarrierProbability();
    
    /**
     * Distribution for the target cell recovery (regeneration) rate.
     *
     * <p>The initialiser samples this distribution to set the baseline rate at
     * which removed or damaged targets are replaced. Higher recovery rates
     * replenish susceptible targets more quickly and can shorten the period of
     * high severity in the in‑host simulation.
     *
     * @return a distribution for target recovery rates used at initialisation
     */
    Distribution getTargetRecoveryRate();
    
    /**
     * Baseline per‑virion infection rate used to calibrate the infection probability.
     *
     * <p>The scalar is copied into the initial in‑host state. It is converted
     * into a per‑time‑step probability during updates and therefore directly
     * affects how readily virions convert susceptible targets into exposed
     * cells in {@link io.github.ai4ci.abm.inhost.InHostStochasticState#update}.
     *
     * @return baseline viral infection rate (scalar)
     */
    Double getBaselineViralInfectionRate();
    
    /**
     * Baseline viral replication rate used to determine virion production.
     *
     * <p>This scalar sets the expected production per infected cell and is
     * sampled/copied by the initialiser into the host state. Larger values
     * increase the expected Poisson rate of new virions produced each step,
     * driving faster viral load growth.
     *
     * @return baseline viral replication rate (scalar)
     */
    Double getBaselineViralReplicationRate();
    
    /**
     * Calibration parameter defining the unit of virions considered disease‑level.
     *
     * <p>The initialiser stores this cutoff on the host state; it is used to
     * normalise virion counts into a unitless viral load proxy and to scale
     * external virion doses. Changing this value moves the threshold for when
     * an individual's viral load is treated as clinically relevant in the
     * simulation.
     *
     * @return the virion disease cutoff used for normalisation and dosing
     */
    Integer getVirionsDiseaseCutoff();
    
}