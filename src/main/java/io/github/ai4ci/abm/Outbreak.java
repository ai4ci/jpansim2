package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.Data;
import io.github.ai4ci.abm.Abstraction.Entity;
import io.github.ai4ci.abm.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.flow.mechanics.StateMachine;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * The central mutable container for an entire simulation instance.
 *
 * <p>
 * An {@code Outbreak} holds all immutable or semi-immutable components of a
 * single simulation run, including:
 * <ul>
 * <li>Configuration ({@link SetupConfiguration},
 * {@link ExecutionConfiguration})</li>
 * <li>Population of {@link Person} agents</li>
 * <li>Social contact network ({@link SocialRelationship})</li>
 * <li>Baseline parameters ({@link OutbreakBaseline})</li>
 * <li>Current state ({@link OutbreakState}) and history
 * ({@link OutbreakHistory})</li>
 * <li>System-wide policy state machine ({@link StateMachine})</li>
 * </ul>
 *
 * <p>
 * This class is <b>mutable by design</b> to support efficient in-place updates
 * during the simulation loop. However, all contained data (e.g., person states,
 * config) are immutable or thread-safe, enabling safe parallel execution.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 * <li><b>Setup</b>: Created via {@link #createOutbreakStub()}, then populated
 * with people and social network by
 * {@link io.github.ai4ci.flow.builders.DefaultNetworkSetup}</li>
 * <li><b>Baselining</b>: Immutable baseline parameters (e.g., R₀-calibrated
 * transmission) are computed once via
 * {@link io.github.ai4ci.flow.builders.DefaultOutbreakBaseliner}</li>
 * <li><b>Initialization</b>: Initial state (time=0) is set via
 * {@link io.github.ai4ci.flow.builders.DefaultOutbreakInitialiser}</li>
 * <li><b>Execution</b>: Updated daily by
 * {@link io.github.ai4ci.flow.mechanics.Updater}, which advances time,
 * processes contacts/exposures, and updates states/history</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 * <li>Reads from multiple threads are safe (e.g., during contact network
 * traversal)</li>
 * <li>Writes occur only during the sequential update cycle (one thread per
 * simulation)</li>
 * <li>Ephemeral fields ({@link #getNextState()}, {@link #getNextHistory()}) are
 * used exclusively during the update cycle to stage next-day values</li>
 * </ul>
 */
@Value.Modifiable
@Data.Mutable
public interface Outbreak
		extends Entity, HistoricalStateProvider<OutbreakHistory>, Cloneable {

	/**
	 * Factory method to create a minimal, unconfigured outbreak stub.
	 *
	 * <p>
	 * Used internally during simulation setup. Callers should use
	 * {@link io.github.ai4ci.flow.ExecutionBuilder} instead.
	 *
	 * @return a new Outbreak instance with empty state/history and stubbed state
	 *         machine
	 */
	public static ModifiableOutbreak createOutbreakStub() {
		var tmp = new ModifiableOutbreak();
		tmp.setNextHistory(Ephemeral.empty());
		tmp.setNextState(Ephemeral.empty());
		tmp.setStateMachine(StateMachine.stub());
		return tmp;
	}

	/**
	 * underride equality to compare outbreak instances by their hash codes,
	 * which are random and unique per instance
	 *
	 * @param another the object to compare with this outbreak
	 * @return true if the other object is an Outbreak with the same hash code,
	 *         false otherwise
	 */
	default boolean equality(Object another) {
		if (another != null && another instanceof Outbreak)
			return this.hash() == ((Outbreak) another).hash();
		return false;
	}

	/**
	 * Average network degree across all agents accounting for undirected edges.
	 *
	 * @return average degree
	 */
	default double getAverageNetworkDegree() {
		return this.getSocialNetwork().size() * 2
				/ (double) this.getPopulationSize();
	}

	/**
	 * Calibrated baseline parameters computed once at simulation start.
	 *
	 * <p>
	 * Includes R₀-derived transmission probability, severity cutoffs, and
	 * infectivity profile. Never changes during simulation.
	 *
	 * @return outbreak baseline parameters
	 */
	OutbreakBaseline getBaseline();

	/**
	 * Returns the most recent historical state (same as current state during
	 * update cycle).
	 *
	 * @return an Optional containing the most recent OutbreakHistory, or empty
	 *         if history is empty
	 */
	default Optional<OutbreakHistory> getCurrentHistory() {
		return this.getHistory().stream().findFirst();
	}

	/**
	 * Current state of the outbreak at this simulation time.
	 *
	 * <p>
	 * Immutable snapshot containing prevalence, hospitalisations, mobility, and
	 * other aggregate metrics. Replaced entirely each timestep.
	 *
	 * @return current outbreak state
	 */
	OutbreakState getCurrentState();

	/**
	 * Configuration defining disease dynamics, behaviour, and policy.
	 *
	 * <p>
	 * Immutable after initialization. Includes in-host model, R₀, testing
	 * strategy, and policy triggers.
	 *
	 * @return execution configuration
	 */
	ExecutionConfiguration getExecutionConfiguration();

	/**
	 * Reverse-time-ordered list of historical outbreak states.
	 *
	 * <p>
	 * Most recent entry (index 0) corresponds to {@link #getCurrentState()}.
	 * Length is capped to ~2× infectious period to limit memory use.
	 *
	 * @return list of outbreak history entries, ordered from most recent to
	 *         oldest
	 */
	@Override
	List<OutbreakHistory> getHistory();

	/**
	 * Ephemeral builder for the next day's outbreak history.
	 *
	 * <p>
	 * Only non-null during the update cycle. Used to accumulate daily summaries
	 * (e.g., test counts, incidence) before finalizing into
	 * {@link #getHistory()}.
	 *
	 * @return ephemeral builder for next outbreak history
	 */
	Ephemeral<ImmutableOutbreakHistory.Builder> getNextHistory();

	/**
	 * Ephemeral builder for the next day's outbreak state.
	 *
	 * <p>
	 * Only non-null during the update cycle (between {@code prepareUpdate()} and
	 * {@code switchState()}). Cleared after each timestep. Used to stage the
	 * next outbreak state before finalizing into {@link #getCurrentState()} and
	 * appending to {@link #getHistory()}.
	 *
	 * @return ephemeral builder for next outbreak state
	 */
	Ephemeral<ImmutableOutbreakState.Builder> getNextState();

	/**
	 * Thread-safe array of all agents in the simulation.
	 *
	 * <p>
	 * Size is fixed at setup time and equals {@link #getPopulationSize()}.
	 * Indexed by agent ID (0-based).
	 *
	 * @return thread-safe array of Person agents
	 */
	ThreadSafeArray<Person> getPeople();

	/**
	 * Retrieve a person by their ID.
	 *
	 * @param id the person's ID
	 * @return an Optional containing the Person if found, or empty if not found
	 */
	default Optional<Person> getPersonById(int id) {
		if (id >= this.getPeople().size()) return Optional.empty();
		return Optional.ofNullable(this.getPeople().get(id));
	}

	/**
	 * Retrieves a specific agent's state at a given simulation time.
	 *
	 * @param id   agent ID
	 * @param time simulation day
	 * @return the agent's history record for that day, or
	 *         {@code Optional.empty()} if not found
	 */
	default Optional<PersonHistory> getPersonHistoryByIdAndTime(
			int id, int time
	) {
		return this.getPersonById(id).flatMap(p -> p.getHistoryEntry(time));
	}

	/**
	 * Total number of agents in the simulation.
	 *
	 * <p>
	 * Derived from {@code setupConfig.getNetwork().getNetworkSize()}.
	 *
	 * @return population size
	 */
	default int getPopulationSize() {
		return this.getSetupConfiguration().getNetwork().getNetworkSize();
	}

	/**
	 * Configuration defining population structure and contact network.
	 *
	 * <p>
	 * Immutable after setup. Includes demographic model (e.g., age-stratified)
	 * and network generator (e.g., Erdős–Rényi).
	 *
	 * @return setup configuration
	 */
	SetupConfiguration getSetupConfiguration();

	/**
	 * Social contact network as a thread-safe array of weighted edges.
	 *
	 * <p>
	 * Each {@link SocialRelationship} defines a potential contact between two
	 * agents, with strength modulated by demographics (e.g., age, proximity).
	 *
	 * @return thread-safe array of social relationships representing the contact
	 *         network this will have been finalised at setup time and will not
	 *         change during the simulation, but is thread-safe to allow
	 *         concurrent reads during contact processing
	 */
	ThreadSafeArray<SocialRelationship> getSocialNetwork();

	/**
	 * System-wide policy state machine.
	 *
	 * <p>
	 * Manages transitions between policy states (e.g., {@code MONITOR} ↔
	 * {@code LOCKDOWN} in {@link io.github.ai4ci.abm.policy.ReactiveLockdown}).
	 *
	 * @return policy state machine
	 */
	StateMachine getStateMachine();

	/**
	 * Unique identifier for this simulation run.
	 *
	 * <p>
	 * Format: {@code urnBase:setupName:setupReplica:execName:execReplica}. Used
	 * for logging, output filenames, and random seed derivation.
	 *
	 * @return unique URN string
	 */
	@Override
	String getUrn();

	/**
	 * underride hash code
	 *
	 * @return a random hash code for this outbreak instance
	 */
	@JsonIgnore @Value.Derived
	default int hash() {
		return UUID.randomUUID().hashCode();
	}

	/**
	 * underride toString to return the outbreak's URN for easier identification
	 * in logs and outputs
	 *
	 * @return the URN of this outbreak instance
	 */
	default String print() {
		return this.getUrn();
	}

}
