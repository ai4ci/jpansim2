package io.github.ai4ci.config;

import io.github.ai4ci.abm.mechanics.Abstraction;

public interface ExperimentOverride {

	Abstraction.SimpleFunction getContactProbability();
	
}
