package io.github.ai4ci.abm.builders;

import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;

public class BuilderFactory {

	public static AbstractModelBuilder builderFrom(SetupConfiguration setupConfig) {
		if (setupConfig instanceof WattsStrogatzConfiguration) return new DefaultModelBuilder();
		if (setupConfig instanceof AgeStratifiedNetworkConfiguration) return new AgeStratifiedModelBuilder();
		throw new RuntimeException("Unknown config type");
	}
	
}
