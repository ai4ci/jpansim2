package io.github.ai4ci.abm.builders;

import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.ImmutableInHostPhenomenologicalState;
import io.github.ai4ci.abm.inhost.InHostPhenomenologicalState;
import io.github.ai4ci.abm.inhost.InHostPhenomenologicalState.BiPhasicLogistic;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

public interface DefaultInHostPhenomenologicalStateInitialiser {

	default InHostPhenomenologicalState initialiseInHostModel(PhenomenologicalModel configuration, ExecutionConfiguration execConfig, Optional<PersonDemographic> person, Sampler rng, int time) {
		
	if (person.isPresent()) {
		configuration = ReflectionUtils.modify(
				configuration,
				execConfig.getDemographicAdjustment(),
				person.get()
		);
	}
		double incubation = configuration.getIncubationPeriod().sample(rng);
		return ImmutableInHostPhenomenologicalState.builder()
				.setTime(time)
				.setInfectiousnessCutoff(configuration.getInfectiousnessCutoff())
				.setViralLoadModel(
						BiPhasicLogistic.calibrateViralLoad(
								incubation, //onsetTime
								configuration.getIncubationToPeakViralLoadDelay().sample(rng), // peakTime, 
								configuration.getPeakToRecoveryDelay().sample(rng), // double duration, 
								configuration.getInfectiousnessCutoff(),// double thresholdLevel, 
								configuration.getApproxPeakViralLoad().sample(rng)// double peakLevel
						)
				)
				.setImmunityModel(
						BiPhasicLogistic.calibrateImmuneActivity(
								configuration.getPeakImmuneResponseDelay().sample(rng), // peakTime, 
								configuration.getApproxPeakImmuneResponse().sample(rng),// peakLevel, 
								configuration.getImmuneWaningHalfLife().sample(rng) // halfLife
						)
				)
				.build();
		
	}
	
}
