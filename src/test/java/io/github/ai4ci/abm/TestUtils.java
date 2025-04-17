package io.github.ai4ci.abm;

import java.util.function.Predicate;

import org.immutables.value.Value;

import io.github.ai4ci.config.ConfigMerger;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.flow.ExperimentBuilder;

@Value.Immutable
public interface TestUtils {

	@Value.Derived default Outbreak getOutbreak() {
		return ExperimentBuilder.buildExperiment(
				ConfigMerger.INSTANCE.mergeConfiguration(
						SetupConfiguration.DEFAULT,
						getSetupTweak()
				).build(),
				ConfigMerger.INSTANCE.mergeConfiguration(
						ExecutionConfiguration.DEFAULT,
						getExecutionTweak()
				).build(),
				"experiment");
	};
	
	default ModifiablePerson getPerson() {
		return (ModifiablePerson) getOutbreak()
				.getPeople().get(0);
	}
	
	default ModifiablePerson getPerson(Predicate<Person> test) {
		return (ModifiablePerson) getOutbreak()
				.getPeople().stream().filter(test)
				.findFirst().get();
	}
	
	default PersonState getPersonState() {
		return getPerson().getCurrentState();
	}
	
	@Value.Default default PartialSetupConfiguration getSetupTweak() {
		return PartialSetupConfiguration.builder()
			.setNetworkSize(100)
			.setNetworkConnectedness(30)
			.setInitialImports(2)
			.build();
	};
	
	@Value.Default default PartialExecutionConfiguration getExecutionTweak( ) {
		return PartialExecutionConfiguration.builder()
			.build();
	};
	
	public static ModifiableOutbreak instance = (ModifiableOutbreak) ImmutableTestUtils.builder().build().getOutbreak();
	
	public static ModifiableOutbreak mockOutbreak() {
		return (ModifiableOutbreak) instance;
	}
	
	public static ModifiablePerson mockPerson() {
		return (ModifiablePerson) mockOutbreak()
				.getPeople().get(0);
	}
	
	public static PersonState mockPersonState() {
		return mockPerson().getCurrentState();
	}
	
	
	
}
