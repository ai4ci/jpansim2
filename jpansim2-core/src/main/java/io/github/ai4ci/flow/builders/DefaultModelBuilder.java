package io.github.ai4ci.flow.builders;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Sampler;

/**
 * Default, compositional model builder that assembles the standard set of
 * setup, baselining and initialisation behaviours from small, focused
 * interfaces.
 *
 * Extending model builders should consider whether to override behaviour by
 * subclassing or by composing alternative interfaces. For example, to change
 * the default outbreak baselining logic a builder could either implement a
 * different baseliner interface or could subclass {@code DefaultModelBuilder}
 * and override the {@code baselineOutbreak} method directly. Both approaches
 * are valid and the best choice depends on the extent of the change and the
 * desired level of modularity. The methods can coexist and the delegation to
 * the default interfaces ensures that the wiring is clear and that alternative
 * implementations can be easily swapped in without modifying the builder
 * itself.
 *
 * <p>
 * Rationale: the builder composes several default behaviour interfaces (for
 * example {@link DefaultNetworkSetup}, {@link DefaultOutbreakBaseliner} and
 * {@link DefaultPersonInitialiser}) rather than centrally implementing all
 * logic. This compositional approach has several benefits:
 * <ul>
 * <li>Separation of responsibilities — each interface encapsulates a distinct
 * concern such as network construction or person baselining.</li>
 * <li>Reusability — alternative builders can mix different interfaces to
 * produce different default behaviours without duplicating code.</li>
 * <li>Testability — small interfaces are easier to mock and test in
 * isolation.</li>
 * <li>Extensibility — callers can subclass {@code DefaultModelBuilder} and
 * selectively override only the hooks they need to change.</li>
 * </ul>
 *
 * <p>
 * Implementation detail: each public method in this class delegates to the
 * corresponding default implementation on the composed interface using
 * {@code InterfaceName.super.method(...)}. This explicit delegation makes the
 * wiring obvious and preserves the ability for subclasses to override behaviour
 * either by implementing a different interface or by overriding the method
 * directly.
 *
 * <p>
 * Composition specifics: the class composes the following default interfaces in
 * the standard build pipeline: {@link DefaultNetworkSetup},
 * {@link DefaultOutbreakBaseliner}, {@link DefaultOutbreakInitialiser},
 * {@link DefaultPersonBaseliner} and {@link DefaultPersonInitialiser}. The
 * {@link DefaultPersonInitialiser} in particular centralises the orchestration
 * that creates the in‑host model for each person (by delegating to the
 * specialised in‑host initialiser interfaces) and sets importation and basic
 * per‑person modifiers. To customise person initialisation replace the
 * {@code DefaultPersonInitialiser} implementation or subclass
 * {@code DefaultModelBuilder} and override the {@code initialisePerson}
 * delegation method.
 *
 * <p>
 * Downstream users such as examples and test harnesses can use this builder as
 * a ready‑made configuration for most simulations and replace individual
 * components by subclassing or by providing alternative interface
 * implementations.
 *
 * @author Rob Challen
 */
public class DefaultModelBuilder extends AbstractModelBuilder implements
		DefaultNetworkSetup, DefaultOutbreakBaseliner, DefaultOutbreakInitialiser,
		DefaultPersonBaseliner, DefaultPersonInitialiser {

	// TODO: look at https://github.com/jhalterman/typetools

	/**
	 * Create a default model builder with the standard set of composed
	 * interfaces.
	 */
	public DefaultModelBuilder() {
		super();
	}

	/**
	 * Delegate outbreak base-lining to the default baseliner.
	 *
	 * <p>
	 * The method forwards to the default implementation so that global outbreak
	 * baseline concerns (policy defaults, aggregated metrics) are defined in a
	 * single place. Subclasses can override to provide custom baselining
	 * behaviour or to compose alternative baseline logic.
	 */
	@Override
	public ImmutableOutbreakBaseline baselineOutbreak(
			ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak,
			Sampler sampler
	) {
		return DefaultOutbreakBaseliner.super.baselineOutbreak(
				builder, outbreak, sampler
		);
	}

	/**
	 * Delegate person baselining to the default person baseliner.
	 *
	 * <p>
	 * Person level baselining (demographics, household membership, persistent
	 * attributes) is provided by {@link DefaultPersonBaseliner}. This class
	 * keeps per person baseline logic modular and easily replaceable.
	 */
	@Override
	public ImmutablePersonBaseline baselinePerson(
			ImmutablePersonBaseline.Builder builder, Person person, Sampler rng
	) {
		return DefaultPersonBaseliner.super.baselinePerson(builder, person, rng);
	}

	/**
	 * Delegate outbreak initialisation to the default outbreak initialiser.
	 *
	 * <p>
	 * Outbreak runtime values that depend on the baselined outbreak are computed
	 * by {@link DefaultOutbreakInitialiser}. Delegating keeps this class focused
	 * on composition and makes it straightforward to swap in an alternative
	 * initialisation strategy.
	 */
	@Override
	public ImmutableOutbreakState initialiseOutbreak(
			ImmutableOutbreakState.Builder builder, Outbreak outbreak,
			Sampler sampler
	) {
		return DefaultOutbreakInitialiser.super.initialiseOutbreak(
				builder, outbreak, sampler
		);
	}

	/**
	 * Delegate person initialisation to the default initialiser.
	 *
	 * <p>
	 * The initialiser converts baseline values into runtime state. By delegating
	 * to {@link DefaultPersonInitialiser} the default conversion logic (for
	 * example seeding infection timers or initial immunity) is centralised and
	 * can be updated independently of the model builder.
	 */
	@Override
	public ImmutablePersonState initialisePerson(
			ImmutablePersonState.Builder builder, Person person, Sampler rng
	) {
		return DefaultPersonInitialiser.super.initialisePerson(
				builder, person, rng
		);
	}

	/**
	 * Delegate outbreak setup to the default network setup implementation.
	 *
	 * <p>
	 * This method intentionally delegates to
	 * {@link DefaultNetworkSetup#setupOutbreak(ModifiableOutbreak, SetupConfiguration, Sampler)}
	 * so that network construction is maintained in a focused interface. To
	 * change network construction replace the composed interface or override
	 * this method in a subclass.
	 */
	@Override
	public ModifiableOutbreak setupOutbreak(
			ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler
	) {
		return DefaultNetworkSetup.super.setupOutbreak(outbreak, config, sampler);
	}

}