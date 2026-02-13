package io.github.ai4ci.config.setup;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

/**
 * Location aware demographic configuration.
 *
 * <p>
 * This interface provides simple helpers for demographies that incorporate
 * spatial information. It defines a proximity based modifier and utility
 * methods used during setup and relationship strength calculation.
 * Implementations are used by the model builder during the setup stage to place
 * people and to inform contact probabilities between agents.
 *
 * <p>
 * Downstream uses include
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak}
 * during outbreak construction and the per‑person baseliner which may consult
 * proximity when assigning household or workplace links.
 *
 * <p>
 * Extension guidance: subclasses may override {@link #getRelationshipStrength}
 * to combine alternative spatial kernels or to use data driven lookup tables.
 * Keep heavy data loads in the setup stage rather than per‑person methods.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableLocationAwareDemography.class)
@JsonDeserialize(as = ImmutableLocationAwareDemography.class)
public interface LocationAwareDemography extends DemographicConfiguration {

	/**
	 * Sensible default configuration for location aware demography used in
	 * examples and tests.
	 */
	public static ImmutableLocationAwareDemography DEFAULT = ImmutableLocationAwareDemography
			.builder().setContactProximityBias(2.0).build();

	/**
	 * Create a minimal person stub attached to the supplied outbreak.
	 *
	 * <p>
	 * Used by setup helpers that need a quick person object for sampling or
	 * placement. Implementations may extend the returned stub further in later
	 * stages.
	 *
	 * @param outbreak owning outbreak used to attach the created person
	 * @return a modifiable person stub
	 */
	@Override
	default ModifiablePerson createPersonStub(Outbreak outbreak) {
		ModifiablePerson tmp = Person.createPersonStub(outbreak);
		return tmp;
	}

	/**
	 * Calibrates the strength of association between distance and contact
	 * probability. Values above 1 compress contact radius; values below 1 expand
	 * it.
	 *
	 * @return proximity bias applied to base contact odds
	 */
	Double getContactProximityBias();

	/**
	 * Compute a relationship strength between two people based on spatial
	 * proximity.
	 *
	 * <p>
	 * The returned value is an odds/probability modifier used by higher level
	 * code when composing demographic and location based contact modifiers.
	 * Implementations that need alternative kernels should override this method.
	 *
	 * @param source  the source person
	 * @param target  the target person
	 * @param sampler a sampler for stochastic sampling when required
	 * @return a relationship strength used as an odds modifier
	 */
	@Override
	default public double getRelationshipStrength(
			Person source, Person target, Sampler sampler
	) {
		return Conversions.scaleProbabilityByOR(
				this.getProximity(source, target),
				1 / this.getContactProximityBias()
		);
	}

}