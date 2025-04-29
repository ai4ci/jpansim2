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
			);
		outbreak.getStateMachine().init( configuration.getDefaultPolicyModel() );
	}
	
}
