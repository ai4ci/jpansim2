package io.github.ai4ci.flow.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.util.Sampler;

/**
 * A generic state machine implementation for managing transitions between
 * different behavioral and policy states in agent-based models.
 *
 * Supports both individual behavior patterns (BehaviorState) and policy
 * patterns (PolicyState). State transitions are implemented using enumerated
 * states that define transition logic through history updates and state
 * updates.
 *
 * <h2>Transition Cycle</h2>
 * <p>
 * The state machine follows a two-phase update cycle:
 * <ul>
 * <li><b>History Update Phase</b>: Occurs early in the update cycle, handling
 * actions like testing that need to be recorded</li>
 * <li><b>State Update Phase</b>: Occurs at the end of the update cycle,
 * determining the next state based on current conditions</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 * <ul>
 * <li>For individual agents: manages behavior state transitions</li>
 * <li>For outbreaks/policies: manages policy state transitions</li>
 * <li>Supports forced state transitions and branching state management</li>
 * </ul>
 *
 * @see State.BehaviourState
 * @see State.PolicyState
 */
public class StateMachine implements Serializable {

	/**
	 * Creates a stub state machine without initial state or context.
	 *
	 * @return a new stub StateMachine instance
	 */
	public static StateMachine stub() {
		return new StateMachine();
	}

	private ModifiableStateMachineContext context;

	private State<?, ?, ?, ?> currentState;

	/** Default constructor for creating an uninitialized state machine. */
	public StateMachine() {}

	/**
	 * Constructs a state machine with a default state and context.
	 *
	 * @param defaultStateMachineState the initial state for the state machine
	 * @param context                  the initial context for the state machine
	 */
	public StateMachine(
			State<?, ?, ?, ?> defaultStateMachineState, StateMachineContext context
	) {
		this.context = ModifiableStateMachineContext.create().from(context);
		this.currentState = defaultStateMachineState;
	}

	/**
	 * Forces a transition to a specific behavior state (except DEAD state).
	 * Maintains state branching history when switching between different
	 * behavior models.
	 *
	 * @param state the target behavior state
	 * @throws RuntimeException if attempting to update a policy state machine
	 *                          with a behavior state
	 */
	public void forceTo(State.BehaviourState state) {
		if (state.equals(NonCompliant.DEAD)) return;
		if (this.getState() instanceof State.BehaviourState) {
			synchronized (this) {
				this.rememberCurrentState(state);
				this.currentState = state;
			}
		} else
			throw new RuntimeException("Cannot update a policy with a behaviour");
	}

	/**
	 * Forces a transition to a specific policy state. Maintains state branching
	 * history when switching between different policy models.
	 *
	 * @param state the target policy state
	 * @throws RuntimeException if attempting to update a behavior state machine
	 *                          with a policy state
	 */
	public void forceTo(State.PolicyState state) {
		if (this.getState() instanceof State.PolicyState) {
			synchronized (this) {
				// If switching to a different behaviour model
				this.rememberCurrentState(state);
				this.currentState = state;
			}
		} else
			throw new RuntimeException("Cannot update a behaviour with a policy");
	}

	/**
	 * Gets the current state of the state machine.
	 *
	 * @return the current state object
	 */
	public State<?, ?, ?, ?> getState() { return this.currentState; }

	/**
	 * Initializes the state machine with a behavior state.
	 *
	 * @param policy the initial behavior state
	 */
	public void init(State.BehaviourState policy) {
		this.currentState = policy;
		this.context = ModifiableStateMachineContext.create()
				.setBaselineState(policy);
	}

	/**
	 * Initializes the state machine with a policy state.
	 *
	 * @param policy the initial policy state
	 */
	public void init(State.PolicyState policy) {
		this.currentState = policy;
		this.context = ModifiableStateMachineContext.create()
				.setBaselineState(policy);
	}

	/**
	 * Performs history update for outbreak states.
	 *
	 * @param builder  the builder for constructing immutable outbreak history
	 * @param outbreak the current outbreak state
	 * @param rng      the random number generator for stochastic operations
	 */
	public void performHistoryUpdate(
			ImmutableOutbreakHistory.Builder builder, OutbreakState outbreak,
			Sampler rng
	) {
		if (this.getState() instanceof State.PolicyState) {
			State.PolicyState state = (State.PolicyState) this.getState();
			state.updateHistory(builder, outbreak, this.context, rng);
		}
	}

	/**
	 * Performs history update for person states.
	 *
	 * @param builder the builder for constructing immutable person history
	 * @param person  the current person state
	 * @param rng     the random number generator for stochastic operations
	 */
	public void performHistoryUpdate(
			ImmutablePersonHistory.Builder builder, PersonState person, Sampler rng
	) {
		if (this.getState() instanceof State.BehaviourState) {
			State.BehaviourState state = (State.BehaviourState) this.getState();
			state.updateHistory(builder, person, this.context, rng);
		}
	}

	/**
	 * Updates the state machine and stores the new outbreak state.
	 *
	 * @param builder  the builder for constructing immutable outbreak state
	 * @param outbreak the current outbreak state
	 * @param rng      the random number generator for stochastic operations
	 */
	public synchronized void performStateUpdate(
			ImmutableOutbreakState.Builder builder, OutbreakState outbreak,
			Sampler rng
	) {
		if (this.getState() instanceof State.PolicyState) {
			State.PolicyState state = (State.PolicyState) this.getState();
			State.PolicyState next = state
					.nextState(builder, outbreak, this.context, rng);
			this.currentState = next;
		}
	}

	/**
	 * Updates the state machine and stores the new person state. Handles death
	 * state transitions automatically.
	 *
	 * @param builder the builder for constructing immutable person state
	 * @param person  the current person state
	 * @param rng     the random number generator for stochastic operations
	 */
	public synchronized void performStateUpdate(
			ImmutablePersonState.Builder builder, PersonState person, Sampler rng
	) {
		if (person.isDead()) {
			this.currentState = NonCompliant.DEAD
					.nextState(builder, person, this.context, rng);
		} else {
			if (this.getState() instanceof State.BehaviourState) {
				State.BehaviourState state = (State.BehaviourState) this.getState();
				State.BehaviourState next = state
						.nextState(builder, person, this.context, rng);
				this.currentState = next;
			}
		}
	}

	/**
	 * Prepares the state machine for an update cycle by resetting context flags.
	 */
	public void prepareUpdate() {
		this.context.resetFlags();
	}

	/**
	 * Remembers the current state if switching between different behavior/policy
	 * models. Only remembers state branches when switching to a different model
	 * (different enum type).
	 *
	 * @param state the target state to compare against current state
	 */
	public void rememberCurrentState(State<?, ?, ?, ?> state) {
		if (state instanceof Enum && this.getState() instanceof Enum) {
			// Don't remember if this is a different state from same behaviour
			// model
			if (((Enum<?>) state).getDeclaringClass()
					.equals(((Enum<?>) this.getState()).getDeclaringClass()))
				return;
		}
		this.context.pushState(this.currentState);
	}

	/**
	 * Returns to a previously remembered state from a branch. Appropriately
	 * handles both behavior and policy state transitions.
	 */
	public void returnFromBranch() {
		if (this.getState() instanceof State.BehaviourState) {
			this.currentState = this.context.pullBehaviour();
		}
		if (this.getState() instanceof State.PolicyState) {
			this.currentState = this.context.pullPolicy();
		}
	}

	/**
	 * Returns a string representation of the state machine's current state.
	 *
	 * @return the name of the current state
	 */
	@Override
	public String toString() {
		return this.getState().getName();
	}

}
