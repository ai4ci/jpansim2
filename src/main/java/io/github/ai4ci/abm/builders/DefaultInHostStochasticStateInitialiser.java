package io.github.ai4ci.abm.builders;

import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.ImmutableInHostStochasticState;
import io.github.ai4ci.abm.inhost.InHostStochasticState;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

public interface DefaultInHostStochasticStateInitialiser {

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
				.setConfig(configuration)
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
