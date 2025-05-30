package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Sampler;

public interface DefaultOutbreakInitialiser {
	
	
	default ImmutableOutbreakState initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak,
			Sampler sampler) {
		ExecutionConfiguration config = outbreak.getExecutionConfiguration();
		builder
			.setTransmissibilityModifier( 1.0 )
			.setContactDetectedProbability( config.getContactDetectedProbability() )
			.setPresumedInfectiousPeriod( config.getInitialEstimateInfectionDuration().intValue() )
			.setPresumedIncubationPeriod( config.getInitialEstimateIncubationPeriod().intValue() )
			.setPresumedSymptomSensitivity( config.getInitialEstimateSymptomSensitivity() ) 
			.setPresumedSymptomSpecificity( config.getInitialEstimateSymptomSpecificity() );
			
			
		return builder.build();
	}
}
