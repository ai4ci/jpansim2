package io.github.ai4ci.abm;

import io.github.ai4ci.config.ConfigMerger;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.flow.ExperimentBuilder;

public class TestUtils {

	static Outbreak instance = null;
	
	public static ModifiableOutbreak mockOutbreak() {
		if (instance == null) {
			instance = ExperimentBuilder.buildExperiment(
				ConfigMerger.INSTANCE.mergeConfiguration(
						SetupConfiguration.DEFAULT,
						PartialSetupConfiguration.create()
							.setNetworkSize(3)
							.setInitialImports(2)
				),
				ExecutionConfiguration.DEFAULT, 
				"experiment");
		}
		return (ModifiableOutbreak) instance;
	}
	
	public static ModifiablePerson mockPerson() {
		return (ModifiablePerson) mockOutbreak()
				.getPeople().get(0);
	}
	
	public static PersonState mockPersonState() {
		return mockPerson().getCurrentState();
	}
	
	public static void reset() {
		instance = null;
	}
	
}
