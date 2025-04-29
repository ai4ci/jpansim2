package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Sampler;

public interface DefaultPersonBaseliner {

	default void baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng) {
		ExecutionConfiguration configuration = person.getOutbreak().getExecutionConfiguration();
		person.getStateMachine().init(configuration.getDefaultBehaviourModel());
		
		// TODO: in the future this will have to be reconfigured to fit with 
		// different types of person e.g. age groups etc.
		// Possibly all of this can be some kind of function of the person demographics?
		
		builder
			.setMobilityBaseline(	Math.sqrt( configuration.getContactProbability().sample(rng) ) )
			.setTransmissibilityModifier(	rng.gamma(1, 0.1) )
			.setComplianceBaseline( configuration.getComplianceProbability().sample(rng))
			.setAppUseProbability( configuration.getAppUseProbability().sample(rng))
			.setDefaultBehaviourState( configuration.getDefaultBehaviourModel() )
			
			.setSymptomSensitivity( configuration.getSymptomSensitivity().sample(rng))
			.setSymptomSpecificity( configuration.getSymptomSpecificity().sample(rng))
			.setSelfIsolationDepth( configuration.getMaximumSocialContactReduction().sample(rng) )
		;
		
	}
	
}
