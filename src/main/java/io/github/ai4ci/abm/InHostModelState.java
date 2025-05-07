package io.github.ai4ci.abm;

import java.io.Serializable;

import io.github.ai4ci.abm.builders.DefaultPersonInitialiser;
import io.github.ai4ci.config.InHostConfiguration;
import io.github.ai4ci.util.Sampler;

public interface InHostModelState<CFG extends InHostConfiguration> extends Serializable {

	CFG getConfig();
	double getNormalisedViralLoad();
	double getNormalisedSeverity();
	double getImmuneActivity();
	boolean isInfected();
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
	
	public static <CFG extends InHostConfiguration> InHostModelState<CFG> test(CFG config, Sampler rng) {
		return (new DefaultPersonInitialiser() {}).initialiseInHostModel(config, null, rng);
	}
	
}
