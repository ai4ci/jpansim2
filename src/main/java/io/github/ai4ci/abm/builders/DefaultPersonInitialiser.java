package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutableInHostPhenomenologicalState;
import io.github.ai4ci.abm.ImmutableInHostStochasticState;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.InHostModelState;
import io.github.ai4ci.abm.InHostPhenomenologicalState;
import io.github.ai4ci.abm.InHostStochasticState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.InHostConfiguration;
import io.github.ai4ci.config.PhenomenologicalModel;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.StochasticModel;
import io.github.ai4ci.config.ExposureModel.BiPhasicLogistic;
import io.github.ai4ci.util.Sampler;

public interface DefaultPersonInitialiser {

	default void initialisePerson(ImmutablePersonState.Builder builder, Person person,
			Sampler rng) {
		ExecutionConfiguration params = person.getOutbreak().getExecutionConfiguration();
		SetupConfiguration configuration = person.getOutbreak().getSetupConfiguration();
		// PersonBaseline baseline = person.getBaseline();
		double limit = ((double) configuration.getInitialImports())/configuration.getNetworkSize();
		
		builder
			.setTransmissibilityModifier(1.0)
			.setMobilityModifier(1.0)
			.setImmuneModifier(1.0)
			.setComplianceModifier(1.0)
			.setSusceptibilityModifier(1.0)
			.setAppUseModifier(1.0)
			
			.setInHostModel(
				initialiseInHostModel(params.getInHostConfiguration(),person, rng)
			)
			.setImportationExposure(
					rng.uniform() < limit ? 2.0 : 0
			)
			.setImmunisationDose(0D);
		
	}
	
	@SuppressWarnings("unchecked")
	default <CFG extends InHostConfiguration> InHostModelState<CFG> initialiseInHostModel(CFG config, Person person, Sampler rng) {
		if (config instanceof PhenomenologicalModel) return (InHostModelState<CFG>) initialiseInHostModel((PhenomenologicalModel) config, person, rng);
		if (config instanceof StochasticModel) return (InHostModelState<CFG>) initialiseInHostModel((StochasticModel) config, person, rng);
		throw new RuntimeException("Unknown in host configuration type");
	}
	
	default InHostPhenomenologicalState initialiseInHostModel(PhenomenologicalModel config, Person person, Sampler rng) {
		int time = person == null ? 0 : person.getOutbreak().getCurrentState().getTime();
		
		double incubation = config.getIncubationPeriod().sample(rng);
		return ImmutableInHostPhenomenologicalState.builder()
				.setTime(time)
				.setConfig(config)
				.setViralLoadModel(
						BiPhasicLogistic.calibrateViralLoad(
								incubation, //onsetTime
								config.getIncubationToPeakViralLoadDelay().sample(rng), // peakTime, 
								config.getPeakToRecoveryDelay().sample(rng), // double duration, 
								config.getInfectiousnessCutoff(),// double thresholdLevel, 
								config.getApproxPeakViralLoad().sample(rng)// double peakLevel
						)
				)
				.setImmunityModel(
						BiPhasicLogistic.calibrateImmuneActivity(
								config.getPeakImmuneResponseDelay().sample(rng), // peakTime, 
								config.getApproxPeakImmuneResponse().sample(rng),// peakLevel, 
								config.getImmuneWaningHalfLife().sample(rng) // halfLife
						)
				)
				.build();
		
	}
	
	default InHostStochasticState initialiseInHostModel(StochasticModel configuration, Person person, Sampler rng) {
		int time = person == null ? 0 : person.getOutbreak().getCurrentState().getTime();
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
