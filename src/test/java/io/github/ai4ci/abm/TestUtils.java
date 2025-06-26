package io.github.ai4ci.abm;

import java.util.function.Predicate;

import org.immutables.value.Value;

import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.setup.PartialSetupConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.flow.ExecutionBuilder;
import io.github.ai4ci.util.ReflectionUtils;

@Value.Immutable
public interface TestUtils {

	@Value.Derived default Outbreak getOutbreak() {
		return ExecutionBuilder.buildExperiment(
				//ConfigMerger.INSTANCE.mergeConfiguration(
				ReflectionUtils.merge(
						SetupConfiguration.DEFAULT,
						getSetupTweak()
				),
				//ConfigMerger.INSTANCE.mergeConfiguration(
				ReflectionUtils.merge(
						ExecutionConfiguration.DEFAULT,
						getExecutionTweak()
				),
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
			.setInitialImports(50)
			.setNetwork(
				WattsStrogatzConfiguration.DEFAULT
					.withNetworkSize(1000)
					.withNetworkDegree(100)
					.withNetworkRandomness(0.15)
			)
			.build();
	};
	
	@Value.Default default PartialExecutionConfiguration getExecutionTweak( ) {
		return PartialExecutionConfiguration.builder()
			.build();
	};
	
	// public static ModifiableOutbreak instance = (ModifiableOutbreak) ImmutableTestUtils.builder().build().getOutbreak();
	public static ImmutableTestUtils.Builder builder = ImmutableTestUtils.builder();
	
	public static ModifiableOutbreak mockOutbreak() {
		return (ModifiableOutbreak) ImmutableTestUtils.builder().build().getOutbreak();
	}
	
	public static ModifiablePerson mockPerson() {
		return (ModifiablePerson) mockOutbreak()
				.getPeople().get(0);
	}
	
	public static PersonState mockPersonState() {
		return mockPerson().getCurrentState();
	}
	
	
	
}
