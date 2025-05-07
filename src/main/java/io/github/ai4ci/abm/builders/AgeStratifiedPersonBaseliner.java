package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.EmpiricalFunction;
import io.github.ai4ci.util.Sampler;

public interface AgeStratifiedPersonBaseliner {
	
	default void baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng) {
		ExecutionConfiguration configuration = person.getOutbreak().getExecutionConfiguration();
		AgeStratifiedNetworkConfiguration config2 = (AgeStratifiedNetworkConfiguration) person.getOutbreak().getSetupConfiguration();
		person.getStateMachine().init(configuration.getDefaultBehaviourModel());
		
		// TODO: So far this example just looks at adjusting the mobility wrt
		// age using a custom function. At some stage we need to figure out how 
		// to express this in configuration. 
		
		builder
			.setMobilityBaseline(	
				config2.adjustedMobilityBaseline(
					Math.sqrt( configuration.getContactProbability().sample(rng) ),
					person
				)
			)
			.setTransmissibilityModifier( 
					config2.adjustedTransmissionFromAge(
							rng.gamma(1, 0.1), person )
			)
			.setComplianceBaseline( 
					config2.adjustedComplianceFromAge(
							configuration.getComplianceProbability().sample(rng), person)
			)
			.setAppUseProbability( 
					config2.adjustedAppUseFromAge(
					configuration.getAppUseProbability().sample(rng), person)
			)
			.setDefaultBehaviourState( configuration.getDefaultBehaviourModel() )
			
			.setSymptomSensitivity( configuration.getSymptomSensitivity().sample(rng))
			.setSymptomSpecificity( configuration.getSymptomSpecificity().sample(rng))
			.setSelfIsolationDepth( 
					config2.adjustedMaximumSocialContactReductionFromAge(
					configuration.getMaximumSocialContactReduction().sample(rng),person )
			)
		;
		
	}
	
}
