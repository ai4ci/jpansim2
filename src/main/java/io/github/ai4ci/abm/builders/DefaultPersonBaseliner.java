package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

public interface DefaultPersonBaseliner {

	default ImmutablePersonBaseline baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng) {
		ExecutionConfiguration configuration = 
				ReflectionUtils.modify(
						person.getOutbreak().getExecutionConfiguration(),
						person.getOutbreak().getExecutionConfiguration().getDemographicAdjustment(),
						person.getDemographic()
				);
		person.getStateMachine().init(configuration.getDefaultBehaviourModel());
		
		builder
			.setMobilityBaseline(
					// Uniform mobility
					rng.nextDouble()
					// Math.sqrt( configuration.getContactProbability().sample(rng) ) 
			)
			.setTransmissibilityModifier(	rng.gamma(1, 0.1) )
			.setComplianceBaseline( configuration.getComplianceProbability().sample(rng))
			.setAppUseProbability( configuration.getAppUseProbability().sample(rng))
			.setDefaultBehaviourState( configuration.getDefaultBehaviourModel() )
			
			.setSymptomSensitivity( configuration.getSymptomSensitivity().sample(rng))
			.setSymptomSpecificity( configuration.getSymptomSpecificity().sample(rng))
			.setSelfIsolationDepth( configuration.getMaximumSocialContactReduction().sample(rng) )
		;
		
		return builder.build();
	}
	
}
