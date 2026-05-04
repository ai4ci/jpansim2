package io.github.ai4ci.config.inhost;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface InHostModelProfile {

	@Value.Immutable
	public interface InHostModelProfileIndividual {

		int getPersonId();
		List<InHostModelState<?>> getStates();
	}

	@JsonIgnore
	List<InHostModelProfileIndividual> getStates();

	static ImmutableInHostModelProfile from(
			ExecutionConfiguration execConfig, double viralChallenge,
			double immunisationChallenge
	) {
		return from(execConfig, 100, 100, viralChallenge, immunisationChallenge);
	}

	static ImmutableInHostModelProfile from(
			ExecutionConfiguration execConfig, int samples, int duration,
			double viralChallenge, double immunisationChallenge
	) {
		var config = execConfig.getInHostConfiguration();
		var rng = Sampler.getSampler();
		var builder = ImmutableInHostModelProfile.builder();
		for (var n = 0; n < samples; n++) {
			var inner = new ArrayList<InHostModelState<?>>();
			InHostModelState<?> state = InHostModelState
				.test(config, execConfig, rng);
			state = state.update(rng, viralChallenge, immunisationChallenge);
			for (var i = 0; i < duration; i++) {
				inner.add(state);
				state = state.update(rng, 0, 0);
			}
			builder.addState(
				ImmutableInHostModelProfileIndividual.builder()
					.setPersonId(n)
					.setStates(inner)
					.build()
			);
		}
		return builder.build();
	}

	@JsonIgnore @Value.Lazy
	default double[][] getViralLoadProfile() {
		return this.getStates()
			.stream()
			.map(
				ls -> ls.getStates()
					.stream()
					.mapToDouble(InHostModelState::getNormalisedViralLoad)
					.toArray()
			)
			.toArray(i -> new double[i][]);
	}

	@JsonIgnore
	default Stream<InHostModelProfileIndividual> stream() {
		return this.getStates()
			.stream();
	}

}
