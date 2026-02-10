package io.github.ai4ci.abm;

import java.util.Optional;
import java.util.stream.Stream;

import io.github.ai4ci.abm.Abstraction.Entity;
import io.github.ai4ci.abm.Abstraction.TemporalState;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;

/**
 * Convenience navigation helpers for extracting model-level information from
 * temporal state objects and related entities.
 *
 * <p>
 * Main purpose: provide a compact collection of static accessors that make it
 * easy to obtain frequently used model objects (for example the current
 * {@link OutbreakState}, {@link OutbreakBaseline}, execution and setup
 * configurations) from a variety of temporal and entity wrappers.
 *
 * <p>
 * Downstream uses: used widely across builders, initialisers and update logic
 * to avoid repetitive dereferencing (for example in state updaters, analysis
 * code and calibration routines). Typical consumers include
 * {@link io.github.ai4ci.flow.mechanics.Updater}, initialisers and diagnostic
 * helpers.
 *
 * @author Rob Challen
 */
public class ModelNav {

	/**
	 * Return the current outbreak state for the supplied person temporal state.
	 *
	 * @param person the temporal person state wrapper
	 * @return the current {@link OutbreakState} for the person's outbreak
	 */
	public static OutbreakState modelState(PersonTemporalState person) {
		return person.getEntity().getOutbreak().getCurrentState();
	}

	/**
	 * Return the outbreak baseline for the outbreak containing the supplied person
	 * temporal state.
	 *
	 * @param person the temporal person state wrapper
	 * @return the {@link OutbreakBaseline} used for the person's outbreak
	 */
	public static OutbreakBaseline modelBase(PersonTemporalState person) {
		return person.getEntity().getOutbreak().getBaseline();
	}

	/**
	 * Return execution configuration parameters for the outbreak that contains the
	 * supplied person temporal state.
	 *
	 * @param person the temporal person state wrapper
	 * @return the {@link ExecutionConfiguration} for the person's outbreak
	 */
	public static ExecutionConfiguration modelParam(PersonTemporalState person) {
		return person.getEntity().getOutbreak().getExecutionConfiguration();
	}

	/**
	 * Return execution configuration parameters from an outbreak temporal state.
	 *
	 * @param outbreak the temporal outbreak state wrapper
	 * @return the {@link ExecutionConfiguration} for the outbreak
	 */
	public static ExecutionConfiguration modelParam(OutbreakTemporalState outbreak) {
		return outbreak.getEntity().getExecutionConfiguration();
	}

	/**
	 * Return the current outbreak state for a plain {@link Person}.
	 *
	 * @param person the person entity
	 * @return the current {@link OutbreakState} for the person's outbreak
	 */
	public static OutbreakState modelState(Person person) {
		return person.getOutbreak().getCurrentState();
	}

	/**
	 * Return the outbreak baseline for a plain {@link Person}.
	 *
	 * @param person the person entity
	 * @return the {@link OutbreakBaseline} used for the person's outbreak
	 */
	public static OutbreakBaseline modelBase(Person person) {
		return person.getOutbreak().getBaseline();
	}

	/**
	 * Return execution configuration parameters for a plain {@link Person}.
	 *
	 * @param person the person entity
	 * @return the {@link ExecutionConfiguration} for the person's outbreak
	 */
	public static ExecutionConfiguration modelParam(Person person) {
		return person.getOutbreak().getExecutionConfiguration();
	}

	/**
	 * Convenience accessor for a person's baseline demographic data.
	 *
	 * @param person the temporal person state wrapper
	 * @return the {@link PersonBaseline} for the person
	 */
	public static PersonBaseline baseline(PersonTemporalState person) {
		return person.getEntity().getBaseline();
	}

	/**
	 * Convenience accessor for a person's baseline demographic data.
	 *
	 * @param person the person entity
	 * @return the {@link PersonBaseline} for the person
	 */
	public static PersonBaseline baseline(Person person) {
		return person.getBaseline();
	}

	/**
	 * Return the current history entry for the supplied temporal person state.
	 *
	 * @param person a {@link PersonState} wrapper
	 * @return an optional current {@link PersonHistory} if present
	 */
	public static Optional<PersonHistory> history(PersonState person) {
		return person.getEntity().getCurrentHistory();
	}

	/**
	 * Stream historical person entries up to the supplied number of days.
	 *
	 * @param person a {@link PersonState} wrapper
	 * @param days   number of history days to return (most recent first)
	 * @return a stream of {@link PersonHistory} entries limited to {@code days}
	 */
	public static Stream<PersonHistory> history(PersonState person, int days) {
		return person.getEntity().getHistory().stream().limit(days);
	}

	/**
	 * Return the current {@link PersonState} for the supplied history entry.
	 *
	 * @param person a {@link PersonHistory} entry
	 * @return the corresponding current {@link PersonState}
	 */
	public static PersonState current(PersonHistory person) {
		return person.getEntity().getCurrentState();
	}

	/**
	 * Stream the current state of every person in the supplied outbreak state.
	 *
	 * @param current the current {@link OutbreakState}
	 * @return a parallel stream of {@link PersonState} for each person
	 */
	public static Stream<PersonState> peopleState(OutbreakState current) {
		return current.getEntity().getPeople().parallelStream().map(p -> p.getCurrentState());
	}

	/**
	 * Stream the current history entry for every person in the supplied outbreak
	 * state.
	 *
	 * @param current the current {@link OutbreakState}
	 * @return a parallel stream of current {@link PersonHistory} entries
	 */
	public static Stream<PersonHistory> peopleCurrentHistory(OutbreakState current) {
		return current.getEntity().getPeople().parallelStream().flatMap(p -> p.getCurrentHistory().stream());
	}

	/**
	 * Stream the person entities for the supplied outbreak state.
	 *
	 * @param current the current {@link OutbreakState}
	 * @return a parallel stream of {@link Person} entities
	 */
	public static Stream<Person> people(OutbreakState current) {
		return current.getEntity().getPeople().parallelStream();
	}

	/**
	 * Return the optional current {@link OutbreakHistory} for the supplied outbreak
	 * state.
	 *
	 * @param outbreakState the outbreak state wrapper
	 * @return an optional current {@link OutbreakHistory}
	 */
	public static Optional<OutbreakHistory> history(OutbreakState outbreakState) {
		return outbreakState.getEntity().getCurrentHistory();
	}

	/**
	 * Stream past outbreak history entries limited to the supplied period.
	 *
	 * @param outbreakState the outbreak state wrapper
	 * @param period        number of historical entries to return (most recent
	 *                      first)
	 * @return a stream of {@link OutbreakHistory} limited to {@code period}
	 */
	public static Stream<OutbreakHistory> history(OutbreakState outbreakState, int period) {
		return outbreakState.getEntity().getHistory().stream().limit(period);
	}

	/**
	 * Retrieve execution parameters from a generic temporal state instance.
	 *
	 * @param temporalState a temporal state (person or outbreak)
	 * @return the corresponding {@link ExecutionConfiguration}
	 * @throws RuntimeException if the temporal state type is not recognised
	 */
	public static ExecutionConfiguration modelParam(TemporalState<?> temporalState) {
		if (temporalState instanceof OutbreakTemporalState) {
			return modelParam((OutbreakTemporalState) temporalState);
		}
		if (temporalState instanceof PersonTemporalState) {
			return modelParam((PersonTemporalState) temporalState);
		}
		throw new RuntimeException("Unknown temporal state type");
	}

	/**
	 * Retrieve setup configuration from a generic temporal state instance.
	 *
	 * @param temporalState a temporal state (person or outbreak)
	 * @return the corresponding {@link SetupConfiguration}
	 * @throws RuntimeException if the temporal state type is not recognised
	 */
	public static SetupConfiguration modelSetup(TemporalState<?> temporalState) {
		if (temporalState instanceof OutbreakTemporalState) {
			return modelSetup((OutbreakTemporalState) temporalState);
		}
		if (temporalState instanceof PersonTemporalState) {
			return modelSetup((PersonTemporalState) temporalState);
		}
		throw new RuntimeException("Unknown temporal state type");
	}

	/**
	 * Retrieve setup configuration for an outbreak temporal state.
	 *
	 * @param outbreak the outbreak temporal state wrapper
	 * @return the {@link SetupConfiguration} for the outbreak
	 */
	public static SetupConfiguration modelSetup(OutbreakTemporalState outbreak) {
		return outbreak.getEntity().getSetupConfiguration();
	}

	/**
	 * Retrieve setup configuration for a person temporal state by delegating to the
	 * person's outbreak.
	 *
	 * @param outbreak the person temporal state wrapper
	 * @return the {@link SetupConfiguration} for the person's outbreak
	 */
	public static SetupConfiguration modelSetup(PersonTemporalState outbreak) {
		return outbreak.getEntity().getOutbreak().getSetupConfiguration();
	}

	/**
	 * The corresponding history entries for this point in time in the outbreak i.e.
	 * a stream of individuals' history entries at the same time point.
	 *
	 * @param outbreakHistory the outbreak history entry to reference
	 * @return a stream of {@link PersonHistory} entries for the same time
	 */
	public static Stream<PersonHistory> peopleHistory(OutbreakHistory outbreakHistory) {
		int time = outbreakHistory.getTime();
		return outbreakHistory.getEntity().getPeople().stream().flatMap(p -> p.getHistoryEntry(time).stream());
	}

	/**
	 * Find the outbreak history entry that corresponds to the supplied person
	 * history time point.
	 *
	 * @param personHistory a {@link PersonHistory} entry
	 * @return the matching {@link OutbreakHistory}
	 */
	public static OutbreakHistory outbreakHistory(PersonHistory personHistory) {
		int time = personHistory.getTime();
		return personHistory.getEntity().getOutbreak().getHistoryEntry(time).get();
	}

	/**
	 * Retrieve the setup configuration from an entity (person or outbreak).
	 *
	 * @param entity an {@link Entity} instance
	 * @return the corresponding {@link SetupConfiguration}
	 * @throws RuntimeException if the entity type is not supported
	 */
	public static SetupConfiguration modelSetup(Entity entity) {
		if (entity instanceof Outbreak) {
			return ((Outbreak) entity).getSetupConfiguration();
		}
		if (entity instanceof Person) {
			return ((Person) entity).getOutbreak().getSetupConfiguration();
		}
		throw new RuntimeException();
	}

	/**
	 * Retrieve the execution configuration from an entity (person or outbreak).
	 *
	 * @param entity an {@link Entity} instance
	 * @return the corresponding {@link ExecutionConfiguration}
	 * @throws RuntimeException if the entity type is not supported
	 */
	public static ExecutionConfiguration modelParam(Entity entity) {
		if (entity instanceof Outbreak) {
			return ((Outbreak) entity).getExecutionConfiguration();
		}
		if (entity instanceof Person) {
			return ((Person) entity).getOutbreak().getExecutionConfiguration();
		}
		throw new RuntimeException();
	}

	/**
	 * Return the outbreak baseline from an outbreak temporal state.
	 *
	 * @param outbreakState the outbreak temporal state wrapper
	 * @return the {@link OutbreakBaseline} for the outbreak
	 */
	public static OutbreakBaseline modelBase(OutbreakTemporalState outbreakState) {
		return outbreakState.getEntity().getBaseline();
	}

	/**
	 * Get the current outbreak state for the supplied person temporal history.
	 *
	 * @param hist a {@link PersonTemporalState}
	 * @return the corresponding current {@link OutbreakState}
	 */
	public static OutbreakState outbreakState(PersonTemporalState hist) {
		return hist.getEntity().getOutbreak().getCurrentState();
	}

}
