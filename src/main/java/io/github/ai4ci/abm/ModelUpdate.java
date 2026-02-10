package io.github.ai4ci.abm;

import static io.github.ai4ci.flow.mechanics.ModelOperation.updateOutbreakState;
import static io.github.ai4ci.flow.mechanics.ModelOperation.updatePersonState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.flow.mechanics.ModelOperation;
import io.github.ai4ci.flow.mechanics.ModelOperation.OutbreakStateUpdater;
import io.github.ai4ci.flow.mechanics.ModelOperation.PersonStateUpdater;

/**
 * Collection of pre-packaged model update functions (hooks) that are applied
 * during the simulation update cycle. These are intended to be small, reusable
 * processing units that can be combined by the
 * {@link io.github.ai4ci.flow.mechanics.Updater Updater} to produce the final
 * per-day changes to both {@link OutbreakState} and {@link PersonState}.
 *
 * <p>
 * Where these are used in the update cycle:
 * <ul>
 * <li>The {@link io.github.ai4ci.flow.mechanics.Updater Updater} builds a list
 * of outbreak and person processors from the values declared in the enums
 * below. Those processors are applied in the {@code updateState} phase of the
 * update cycle (see
 * {@link io.github.ai4ci.flow.mechanics.Updater#updateState(Outbreak)}).</li>
 * <li>Order and timing: outbreak processors are applied as pre- and
 * post-processors around the per-agent updates (see
 * {@link io.github.ai4ci.flow.mechanics.Updater#updateState(Outbreak)}), while
 * person processors run for each agent during the agent-level
 * {@code updateState} (see
 * {@link io.github.ai4ci.flow.mechanics.Updater#updateState(Person)}). The
 * {@link io.github.ai4ci.flow.mechanics.Updater} constructor collects the enum
 * values into the processor lists so adding a new enum value automatically
 * exposes it to the update loop.</li>
 * <li>These functions execute after behaviour and policy state transitions have
 * been determined for the upcoming day. That means they can see the new
 * behaviour/policy choices (the state machine will already have computed the
 * next behaviour/policy), but they execute while the rest of the model
 * (states/histories) is still in the transitional phase described in
 * {@link io.github.ai4ci.flow.mechanics.Updater Updater}'s class-level
 * documentation.</li>
 * </ul>
 *
 * <p>
 * Design notes and extension points:
 * <ul>
 * <li>Each enum value below wraps a {@link ModelOperation.OutbreakStateUpdater}
 * or {@link ModelOperation.PersonStateUpdater} — small lambdas that accept a
 * selector and a mutator/consumer. Use
 * {@link io.github.ai4ci.flow.mechanics.ModelOperation#updatePersonState(java.util.function.Predicate, ModelOperation.TriConsumer)}
 * and
 * {@link io.github.ai4ci.flow.mechanics.ModelOperation#updateOutbreakState(java.util.function.Predicate, ModelOperation.TriConsumer)}
 * to create new updaters in the same style.</li>
 * <li>Person processors run after in-host and risk models have been advanced to
 * (t+1) but before the newly built state is committed to the agent. This allows
 * processors to observe computed infection/risk signals and modify the next
 * state builder accordingly (e.g., set immunisation dose, schedule importation
 * exposures, or set other next-day modifiers).</li>
 * <li>Because these updaters are invoked late in the state construction phase,
 * they should avoid mutating long-lived shared structures; prefer modifying the
 * provided builders.</li>
 * </ul>
 *
 * <p>
 * Work-in-progress / Vaccination note:
 * <ul>
 * <li>The {@link PersonUpdaterFn#IMMUNISATION_PROTOCOL} is provided as a
 * placeholder to show where vaccination/immunisation logic can be implemented.
 * The current implementation merely sets {@code immunisationDose} to 0 on the
 * next builder and should be considered incomplete. Full vaccination routines
 * (eligibility, prioritisation, multi-dose scheduling, waning immunity, and
 * interaction with the in-host model) are not yet implemented in this helper
 * and should be authored either as a new person processor (via
 * {@link io.github.ai4ci.flow.mechanics.Updater#withPersonProcessor}) or by
 * extending this enum with a complete implementation that consults the
 * outbreak's {@link io.github.ai4ci.config.execution.ExecutionConfiguration
 * ExecutionConfiguration} and the agent's demographic/state.</li>
 * </ul>
 */
public class ModelUpdate {

	static Logger log = LoggerFactory.getLogger(ModelUpdate.class);

	public static enum OutbreakUpdaterFn {
		/**
		 * Default no-op outbreak updater. Kept so the Updater has at least one outbreak
		 * processor by default. Outbreak-level processors are run once per day during
		 * {@code updateState} on the outbreak builder and can modify global parameters
		 * (e.g. available tests, contact detection probability).
		 */
		DEFAULT(updateOutbreakState((outbreak) -> true, (builder, outbreak, rng) -> {
			// OutbreakBaseline baseline = outbreak.getBaseline();
			// OutbreakState current = outbreak.getCurrentState();
		})),;

		OutbreakStateUpdater fn;

		OutbreakUpdaterFn(ModelOperation.OutbreakStateUpdater fn) {
			this.fn = fn;
		}

		public OutbreakStateUpdater fn() {
			return this.fn;
		}
	}

	/**
	 * Person-level updaters: run for every person during the {@code updateState}
	 * phase (see
	 * {@link io.github.ai4ci.flow.mechanics.Updater#updateState(Person)}). These
	 * get access to the person's next-state builder and the current person object
	 * so they can make deterministic or probabilistic modifications to the next
	 * state before it is finalised.
	 */
	public static enum PersonUpdaterFn {
		/**
		 * Default no-op person updater. This exists so the Updater has at least one
		 * person processor by default and demonstrates the expected lambda shape.
		 */
		DEFAULT(updatePersonState((person) -> true, (builder, person, rng) -> {
			// PersonBaseline baseline = person.getBaseline();
			// PersonState current = person.getCurrentState();

		})),

		/**
		 * Immunisation (vaccination) protocol placeholder.
		 *
		 * <p>
		 * Current behaviour: sets the next state's immunisation dose to 0.0 as a
		 * placeholder. Intended use:
		 * <ol>
		 * <li>Be extended to consult outbreak-level vaccine supply and
		 * prioritisation.</li>
		 * <li>Apply doses to eligible individuals by mutating the next-state builder
		 * (e.g. {@code builder.setImmunisationDose(...)}) and schedule follow-up
		 * doses.</li>
		 * <li>Interact with the in-host model and risk model so that vaccination
		 * affects susceptibility, severity and waning parameters.</li>
		 * </ol>
		 *
		 * Important: the full vaccination workflow (eligibility, queueing, multi-dose
		 * schedules and waning immunity) is not implemented here. Treat this enum value
		 * as a scaffolding point for productionising vaccine logic; prefer implementing
		 * a dedicated person processor or service that can be added via
		 * {@link io.github.ai4ci.flow.mechanics.Updater#withPersonProcessor(java.util.function.Predicate, io.github.ai4ci.flow.mechanics.ModelOperation.TriConsumer)}
		 * when building an experiment.
		 */
		IMMUNISATION_PROTOCOL(updatePersonState((person) -> true, (builder, person, rng) -> {
			// PersonBaseline baseline = person.getBaseline();
			// PersonState current = person.getCurrentState();
			builder.setImmunisationDose(0D);
		})),

		/**
		 * Importation protocol: demonstrates how one might seed exposure on the next
		 * day using a per-person probability scaled by model parameters and the agent's
		 * mobility. This adds an importation exposure to the next state builder if the
		 * random draw succeeds; otherwise it leaves the importation exposure as zero.
		 */
		IMPORTATION_PROTOCOL(updatePersonState((person) -> true, (builder, person, rng) -> {
			// PersonBaseline baseline = person.getBaseline();
			// PersonState current = person.getCurrentState();
			if (rng.uniform() < ModelNav.modelParam(person).getImportationProbability()
					* person.getCurrentState().getAdjustedMobility()) {
				builder.setImportationExposure(2D);
			} else {
				builder.setImportationExposure(0D);
			}
		}))

		;

		private PersonStateUpdater fn;

		private PersonUpdaterFn(ModelOperation.PersonStateUpdater fn) {
			this.fn = fn;
		}

		/**
		 * The function that performs the person-level update. This is a lambda that
		 * accepts a selector (predicate) and a mutator (tri-consumer) and returns a
		 *
		 * @return a {@link PersonStateUpdater} that can be applied to a person's next
		 *         state builder during the update cycle.
		 */
		public PersonStateUpdater fn() {
			return this.fn;
		}
	}

}