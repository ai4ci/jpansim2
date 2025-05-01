package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.InHostPhenomenologicalState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.SimpleDistribution;

@Value.Immutable
@JsonSerialize(as = ImmutablePhenomenologicalModel.class)
@JsonDeserialize(as = ImmutablePhenomenologicalModel.class)
public interface PhenomenologicalModel extends InHostConfiguration {
	
	public static ImmutablePhenomenologicalModel DEFAULT = ImmutablePhenomenologicalModel.builder()
			
			.setSymptomCutoff(0.5)
			.setInfectiousnessCutoff(0.2)
			
			.setIncubationPeriod(SimpleDistribution.logNorm(5D, 2D))
			.setApproxPeakViralLoad( SimpleDistribution.unimodalBeta(0.5, 0.1))
			.setIncubationToPeakViralLoadDelay(SimpleDistribution.logNorm(2D, 1D))
			.setPeakToRecoveryDelay(SimpleDistribution.logNorm(8D,3D))
			
			.setApproxPeakImmuneResponse( SimpleDistribution.unimodalBeta(0.5, 0.1))
			.setImmuneWaningHalfLife(SimpleDistribution.logNorm(300D, 10D))
			.setPeakImmuneResponseDelay( SimpleDistribution.logNorm(20D, 4D) )
			
			.build();
	
	double getSymptomCutoff();
	double getInfectiousnessCutoff();
	
	SimpleDistribution getIncubationPeriod();
	SimpleDistribution getApproxPeakViralLoad();
	SimpleDistribution getIncubationToPeakViralLoadDelay();
	SimpleDistribution getPeakToRecoveryDelay();
	
	SimpleDistribution getApproxPeakImmuneResponse();
	SimpleDistribution getPeakImmuneResponseDelay();
	SimpleDistribution getImmuneWaningHalfLife();
	
	public default InHostPhenomenologicalState initialise(Person person, Sampler rng) {
		InHostConfiguration config = person.getOutbreak().getExecutionConfiguration().getInHostConfiguration();
		int time = person.getOutbreak().getCurrentState().getTime();
		return InHostPhenomenologicalState.initialise((PhenomenologicalModel) config, rng, time);
	}
	
}