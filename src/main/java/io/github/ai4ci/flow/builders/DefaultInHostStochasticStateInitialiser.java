package io.github.ai4ci.flow.builders;

import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.ImmutableInHostStochasticState;
import io.github.ai4ci.abm.inhost.InHostStochasticState;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

/**
 * Default initialiser for the stochastic in‑host model.
 *
 * <p>This interface constructs an {@link InHostStochasticState} from a
 * {@link StochasticModel} configuration and execution parameters. The
 * implementation samples initial counts and rates (targets, immune pools,
 * priming and waning rates) and populates the initial state containers.
 *
 * <p>Role in composition: the initialiser is consumed by
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} and is
 * composed into {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}.
 * The model builder delegates to this interface's default implementation
 * using {@code DefaultInHostStochasticStateInitialiser.super.initialiseInHostModel(...)}.
 * Keeping this logic isolated makes it trivial to swap alternative
 * stochastic initialisers.
 *
 * <p>Demographic adjustment: if a {@link PersonDemographic} is provided the
 * configuration is adjusted using
 * {@link io.github.ai4ci.util.ReflectionUtils#modify} before sampling so
 * demographic specific scalings from
 * {@link io.github.ai4ci.config.execution.DemographicAdjustment.Phenomenological}
 * can be applied.
 *
 * <p>Extension guidance: implementers should focus this component on creating
 * the immutable in‑host stochastic state and not on external IO; expensive
 * per‑person computation should be performed during outbreak baselining if
 * possible.
 *
 * @author Rob Challen
 */
public interface DefaultInHostStochasticStateInitialiser {

	/**
	 * Create an {@link InHostStochasticState} from a stochastic configuration.
	 *
	 * <p>The default implementation applies demographic adjustment (if a
	 * demographic is present), copies scalar rates and samples distributional
	 * entries to populate initial target and immune pools used by the
	 * stochastic update mechanics.
	 *
	 * @param configuration the stochastic in‑host configuration
	 * @param execConfig execution configuration providing contextual rates
	 * @param person optional demographic used to apply demographic adjustments
	 * @param rng sampler used for stochastic draws
	 * @param time current simulation time used to timestamp the state
	 * @return an initial {@link InHostStochasticState}
	 */
	default InHostStochasticState initialiseInHostModel(StochasticModel configuration, ExecutionConfiguration execConfig, Optional<PersonDemographic> person, Sampler rng, int time) {
			
		if (person.isPresent()) {
			configuration = ReflectionUtils.modify(
					configuration,
					execConfig.getDemographicAdjustment(),
					person.get()
			);
		}
		
		return ImmutableInHostStochasticState.builder()
				.setTime(time)
				.setBaselineViralInfectionRate(configuration.getBaselineViralInfectionRate())
				.setBaselineViralReplicationRate(configuration.getBaselineViralReplicationRate())
				.setVirionsDiseaseCutoff(configuration.getVirionsDiseaseCutoff())
				// .setConfig(configuration)
				.setTargets(configuration.getTargetCellCount())
				.setTargetSusceptible(configuration.getTargetCellCount())
				.setTargetExposed(0)
				.setTargetInfected(0)
				.setVirions(0)
				.setVirionsProduced(0)
				.setImmunePriming(0)
				.setImmuneActive(0)
				.setImmuneTargetRatio(configuration.getImmuneTargetRatio().sample(rng))
				.setImmuneActivationRate(configuration.getImmuneActivationRate().sample(rng))
				.setImmuneWaningRate(configuration.getImmuneWaningRate().sample(rng))
				.setTargetRecoveryRate(configuration.getTargetRecoveryRate().sample(rng))
				.setInfectionCarrierProbability(configuration.getInfectionCarrierProbability().sample(rng))
				.build();
	}
}