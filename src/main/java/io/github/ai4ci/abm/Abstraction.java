package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base interface defining abstraction layers for modeling different levels of
 * detail in agent behavior and mathematical operations within the agent-based
 * modeling framework.
 *
 * <p>
 * This interface serves as a container for various nested interfaces that
 * define different types of abstractions used throughout the modeling system,
 * including modifications, functions, distributions, entities, and state
 * management utilities.
 *
 * <p>
 * These abstractions enable consistent handling of mathematical operations,
 * probability distributions, and object identification across the modeling
 * framework.
 */
public interface Abstraction {

	/**
	 * Base interface for identifiable entities within the modeling framework.
	 *
	 * <p>
	 * Provides consistent identification and metadata access for all entities in
	 * the modeling system, including model and experiment identification.
	 */
	public interface Entity extends Serializable {

		/**
		 * Unique resource name (URN) for this entity, used for consistent
		 * identification
		 *
		 * @return the unique resource name (URN) identifier for this entity
		 */
		@Value.NonAttribute
		String getUrn();

		/**
		 * Model name for this entity, derived from the model configuration it belongs
		 * to.
		 *
		 * @return the name of the model configuration this entity belongs to
		 */
		default String getModelName() {
			return ModelNav.modelSetup(this).getName();
		}

		/**
		 * Model replica number for this entity, derived from the model configuration it
		 * belongs to.
		 *
		 * @return the replica number of the model configuration this entity belongs to
		 */
		default Integer getModelReplica() {
			return ModelNav.modelSetup(this).getReplicate();
		}

		/**
		 * Experiment name for this entity, derived from the experiment configuration it
		 * belongs to.
		 *
		 * @return the name of the experiment this entity belongs to
		 */
		default String getExperimentName() {
			return ModelNav.modelParam(this).getName();
		}

		/**
		 * Experiment replica number for this entity, derived from the experiment
		 * configuration it belongs to.
		 *
		 * @return the replica number of the experiment this entity belongs to
		 */
		default Integer getExperimentReplica() {
			return ModelNav.modelParam(this).getReplicate();
		}
	}

	/**
	 * Interface for temporal states that track entity evolution over time.
	 *
	 * <p>
	 * Represents the state of an entity at a specific time step, providing metadata
	 * and identification inherited from the associated entity.
	 *
	 * @param <E> the type of entity this state represents
	 */
	public interface TemporalState<E extends Entity> extends Serializable {

		/**
		 * The entity this state represents, providing access to its metadata and
		 * identifiers.
		 *
		 * @return the entity this state represents
		 */
		E getEntity();

		/**
		 * The simulation time step for this state, indicating the temporal point at
		 * which this state is valid.
		 *
		 * @return the simulation time step for this state
		 */
		Integer getTime();

		/**
		 * Unique resource name (URN) for this temporal state, constructed from the
		 * entity's URN and the time step.
		 *
		 * @return the URN identifier for this temporal state
		 */
		@Value.Derived
		default String getUrn() {
			return getEntity().getUrn() + ":step:" + getTime();
		}

		/**
		 * Model name inherited from the associated entity, providing context for this
		 * state within the model configuration.
		 *
		 * @return the model name inherited from the entity
		 */
		default String getModelName() {
			return getEntity().getModelName();
		}

		/**
		 * Model replica number inherited from the associated entity, providing context
		 * for this state within the model configuration.
		 *
		 * @return the model replica number inherited from the entity
		 */
		default Integer getModelReplica() {
			return getEntity().getModelReplica();
		}

		/**
		 * Experiment name inherited from the associated entity, providing context for
		 * this state within the experiment configuration.
		 *
		 * @return the experiment name inherited from the entity
		 */
		default String getExperimentName() {
			return getEntity().getExperimentName();
		}

		/**
		 * Experiment replica number inherited from the associated entity, providing
		 * context for this state within the experiment configuration.
		 *
		 * @return the experiment replica number inherited from the entity
		 */
		default Integer getExperimentReplica() {
			return getEntity().getExperimentReplica();
		}
	}

	/**
	 * Interface for objects that have a name identifier.
	 */
	public interface Named {
		/**
		 * The name identifier for this object, used for labeling and identification
		 * purposes.
		 *
		 * @return the name identifier
		 */
		String getName();
	}

	/**
	 * Interface for objects that support replica/simulation run tracking.
	 */
	public interface Replica {
		/**
		 * The replica number for this object, used to track different simulation runs
		 * or replicates.
		 *
		 * @return the replica number (defaults to 0)
		 */
		@JsonIgnore
		@Value.Default
		default Integer getReplicate() {
			return 0;
		}
	}

	/**
	 * Interface for providing access to historical state data.
	 *
	 * <p>
	 * Used by objects that maintain a history of temporal states, enabling
	 * retrieval of past states for analysis and modeling purposes.
	 *
	 * @param <H> the type of historical state being provided
	 */
	public interface HistoricalStateProvider<H extends TemporalState<?>> {

		/**
		 * Gets the complete history of temporal states, ordered from most recent to
		 * oldest.
		 *
		 * @return the complete history of temporal states
		 */
		public List<H> getHistory();

		/**
		 * Gets a history entry for N days in the past if such an entry exists.
		 *
		 * @param delay the number of days in the past
		 * @return an optional containing the historical state if available
		 */
		public default Optional<H> getHistory(int delay) {
			if (delay < 0) {
				return Optional.empty();
			}
			if (this.getHistory().size() <= delay) {
				return Optional.empty();
			}
			return Optional.of(this.getHistory().get(delay));
		}

		/**
		 * Gets a history entry for simulation day N if such an entry exists.
		 *
		 * @param time the absolute simulation time
		 * @return an optional containing the historical state if available
		 */
		public default Optional<H> getHistoryEntry(int time) {
			if (getHistory().size() == 0) {
				return Optional.empty();
			}
			int currentTime = getHistory().get(0).getTime();
			var delay = currentTime - time;
			return getHistory(delay);
		}

	}
}