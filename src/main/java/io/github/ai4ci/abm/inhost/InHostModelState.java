package io.github.ai4ci.abm.inhost;

import java.io.Serializable;
import java.util.Optional;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.builders.DefaultPersonInitialiser;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.util.Sampler;

public interface InHostModelState<CFG extends InHostConfiguration> extends Serializable {

	CFG getConfig();
	@Value.NonAttribute double getNormalisedViralLoad();
	@Value.NonAttribute double getNormalisedSeverity();
	@Value.NonAttribute double getImmuneActivity();
	@Value.NonAttribute boolean isInfected();
	Integer getTime();
	
	InHostModelState<CFG> update(Sampler sampler, double virionExposure, double immunisationDose); // , double viralActivityModifier, double immuneModifier);
	
	default InHostModelState<CFG> update(Person person, Sampler sampler) {
		
		return update(
			sampler,
			person.getCurrentState().getTotalExposure(),
			person.getCurrentState().getImmunisationDose()
		);
		
//				person.getOutbreak().getCurrentState().getViralActivityModifier(),
//				person.getCurrentState().getImmuneModifier()
//		);
		
	}
	
	public static <CFG extends InHostConfiguration> InHostModelState<CFG> test(CFG config, ExecutionConfiguration execConfig, Sampler rng) {
		return (new DefaultPersonInitialiser() {}).initialiseInHostModel(config, execConfig, Optional.empty(), rng, 0);
	}
	
}
