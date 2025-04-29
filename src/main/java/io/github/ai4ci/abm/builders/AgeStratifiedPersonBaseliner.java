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
	
	/**
	 * An odds ratio making age dependence baked into a mobility baseline 
	 * @return
	 */
	private static double ageDependentMobility(PersonDemographic p, EmpiricalFunction mobilityFromAge) {
//		if (p.getAge() < 5) return 1;
//		if (p.getAge() < 15) return 1.5;
//		if (p.getAge() < 25) return 1.25;
//		if (p.getAge() < 75) return 0.8;
		return mobilityFromAge.interpolate(p.getAge());
	}
	
	
	default void baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng) {
		ExecutionConfiguration configuration = person.getOutbreak().getExecutionConfiguration();
		AgeStratifiedNetworkConfiguration config2 = (AgeStratifiedNetworkConfiguration) person.getOutbreak().getSetupConfiguration();
		person.getStateMachine().init(configuration.getDefaultBehaviourModel());
		
		// TODO: So far this example just looks at adjusting the mobility wrt
		// age using a custom function. At some stage we need to figure out how 
		// to express this in configuration. 
		
		builder
			.setMobilityBaseline(	
				Conversions.scaleProbabilityByOR(
					Math.sqrt( configuration.getContactProbability().sample(rng) ),
					ageDependentMobility(person.getDemographic(), config2.getOddsMobilityFromAge())
				)
			)
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
