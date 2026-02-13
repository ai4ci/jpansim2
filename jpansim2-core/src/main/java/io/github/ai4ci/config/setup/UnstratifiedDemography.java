package io.github.ai4ci.config.setup;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.functions.Distribution;
import io.github.ai4ci.functions.SimpleDistribution;
import io.github.ai4ci.util.Sampler;

/**
 * Simple unstratified demographic configuration.
 *
 * <p>
 * This implementation provides a minimal demographic model where relationship
 * strengths are drawn from a single distribution and no stratification (for
 * example by age or location) is applied. It is useful for tests and simple
 * experiments where demographic detail is not required.
 *
 * <p>
 * Downstream users include setup and baselining code which may call
 * {@link #createPersonStub} to construct simple person stubs and
 * {@link #getRelationshipStrength} to obtain per‑pair contact modifiers.
 *
 * <p>
 * Extension guidance: for richer demographies prefer
 * {@link AgeStratifiedDemography} or {@link LocationAwareDemography}.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableUnstratifiedDemography.class)
@JsonDeserialize(as = ImmutableUnstratifiedDemography.class)
public interface UnstratifiedDemography extends DemographicConfiguration {

	/**
	 * A sensible default instance for tests and examples.
	 */
	ImmutableUnstratifiedDemography DEFAULT = ImmutableUnstratifiedDemography
			.builder().build();

	/**
	 * Create a minimal person stub attached to the supplied outbreak.
	 *
	 * @param outbreak the owning outbreak
	 * @return a simple modifiable person stub
	 */
	@Override @JsonIgnore
	default ModifiablePerson createPersonStub(Outbreak outbreak) {
		return Person.createPersonStub(outbreak);
	}

	/**
	 * Draw a relationship strength from the configured distribution.
	 *
	 * @param source  source person (unused in this simple implementation)
	 * @param target  target person (unused)
	 * @param sampler sampler used to draw from the distribution
	 * @return sampled relationship strength
	 */
	@Override @JsonIgnore
	default double getRelationshipStrength(
			Person source, Person target, Sampler sampler
	) {
		return this.getRelationshipStrengthDistribution().sample(sampler);
	}

	/**
	 * Default distribution used for relationship strength sampling.
	 *
	 * @return a uniform distribution by default
	 */
	@Value.Default
	default Distribution getRelationshipStrengthDistribution() {
		return SimpleDistribution.uniform();
	}

}