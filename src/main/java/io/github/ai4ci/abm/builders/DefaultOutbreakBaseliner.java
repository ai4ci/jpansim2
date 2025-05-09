package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Sampler;

public interface DefaultOutbreakBaseliner {

	
	default void baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak,
			Sampler sampler) {
		ExecutionConfiguration configuration = outbreak.getExecutionConfiguration();
	
		//N.B. happens after people are baselined.., I think
		
		builder
			.setDefaultPolicyState( configuration.getDefaultPolicyModel() )
			.setViralLoadTransmissibilityProbabilityFactor( 
					Calibration.inferViralLoadTransmissionProbabilityFactor(outbreak,
							configuration.getRO()
					) 
			)
			.setSeveritySymptomsCutoff(
					Calibration.inferSeverityCutoff(outbreak, configuration.getAsymptomaticFraction())
			)
			.setSeverityHospitalisationCutoff(
					Calibration.inferSeverityCutoff(outbreak, 
							1-(1-configuration.getAsymptomaticFraction())*configuration.getCaseHospitalisationRate())
							// lets say 40% asymptomatic & case hosp rate of 10%. The IHR overall is 10% of the 60% symptomatic, so 6%
							// The cutoff is the people that don;t get hospitalised so 94% quantile.
					)
			.setSeverityDeathCutoff(
					Calibration.inferSeverityCutoff(outbreak, 
							1-(1-configuration.getAsymptomaticFraction())*configuration.getCaseFatalityRate())
					)		
			
			;
		outbreak.getStateMachine().init( configuration.getDefaultPolicyModel() );
	}
	
}
