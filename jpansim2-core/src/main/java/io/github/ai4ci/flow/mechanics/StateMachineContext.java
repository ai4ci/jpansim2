package io.github.ai4ci.flow.mechanics;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value.Modifiable;

import io.github.ai4ci.flow.mechanics.State.BehaviourState;
import io.github.ai4ci.flow.mechanics.State.PolicyState;

/**
 * Context tracking for state machine operations, providing state management and
 * branching support.
 *
 * <p>
 * This interface manages the context for state transitions in the agent-based
 * modeling framework:
 * <ul>
 * <li>Tracks the baseline (default) state for the state machine</li>
 * <li>Maintains a stack of branch states for temporary state transitions</li>
 * <li>Captures reactive testing flags that influence state behavior</li>
 * <li>Provides operations for managing state branching and recovery</li>
 * </ul>
 *
 * <h2>Branching Mechanism</h2>
 * <p>
 * The branching mechanism allows state machines to temporarily enter
 * alternative states (e.g., testing states, non-compliance behaviors) and then
 * return to the baseline. Branches are managed as a stack structure (LIFO)
 * through {@link #pushState(State)} and corresponding pull methods.
 *
 * <h2>Integration with StateMachine</h2>
 * <p>
 * This context is primarily used by {@link StateMachine} to manage state
 * transitions: {@link StateMachine#forceTo(BehaviourState)} and
 * {@link StateMachine#forceTo(PolicyState)} push the current state to branches
 * before switching, while {@link StateMachine#returnFromBranch()} retrieves
 * states using the pull methods.
 *
 * <h2>Reactive Testing</h2>
 * <p>
 * The {@code #isReactivelyTestedToday()} flag indicates whether a reactive
 * testing event occurred today, which may influence state transitions (e.g.,
 * entering isolation after positive test results). The flag is reset daily
 * through {@link #resetFlags()}.
 *
 * @see StateMachine
 * @see State
 * @see BehaviourState
 * @see PolicyState
 */
@Modifiable
public interface StateMachineContext extends Serializable {

	/**
	 * Gets the baseline (default) state for the state machine. This is the state
	 * to which the machine returns after all branches are exhausted.
	 *
	 * @return the baseline state object
	 */
	public State<?, ?, ?, ?> getBaselineState();

	/**
	 * Gets the current stack of branch states. States are maintained in order
	 * from most recent to oldest.
	 *
	 * @return an ordered list of branch states (most recent first)
	 */
	public List<State<?, ?, ?, ?>> getBranchState();

	/**
	 * Checks if a reactive testing event occurred today. Reactive testing
	 * typically refers to testing triggered by symptoms or contact tracing.
	 *
	 * @return true if reactive testing occurred today, false otherwise
	 */
	public boolean isReactivelyTestedToday();

	/**
	 * Pulls and removes the most recent behavior state from the branch stack. If
	 * no branches exist, returns to the baseline state.
	 *
	 * @return the next behavior state to transition to
	 * @throws RuntimeException if the retrieved state is not a behavior state
	 */
	public default State.BehaviourState pullBehaviour() {
		State<?, ?, ?, ?> out;
		if (this.getBranchState().size() > 0) {
			out = this.getBranchState().get(0);
			this.getBranchState().remove(0);

		} else {
			out = this.getBaselineState();
		}
		if (out instanceof State.BehaviourState)
			return (State.BehaviourState) out;
		throw new RuntimeException("Wrong type of state");
	}

	/**
	 * Pulls and removes the most recent policy state from the branch stack. If
	 * no branches exist, returns to the baseline state.
	 *
	 * @return the next policy state to transition to
	 * @throws RuntimeException if the retrieved state is not a policy state
	 */
	public default State.PolicyState pullPolicy() {
		State<?, ?, ?, ?> out;
		if (this.getBranchState().size() > 0) {
			out = this.getBranchState().get(0);
			this.getBranchState().remove(0);

		} else {
			out = this.getBaselineState();
		}
		if (out instanceof State.PolicyState) return (State.PolicyState) out;
		throw new RuntimeException("Wrong type of state");
	}

	/**
	 * Pushes the current state to the branch stack. This operation is typically
	 * called before forcing a state transition to preserve the current state for
	 * later recovery.
	 *
	 * @param state the state to push onto the branch stack
	 */
	public default void pushState(State<?, ?, ?, ?> state) {
		this.getBranchState().add(0, state);
	}

	/**
	 * Resets daily flags including the reactive testing flag. Called at the
	 * beginning of each simulation day to clear transient state.
	 */
	public default void resetFlags() {
		this.setReactivelyTestedToday(false);
	}

	/**
	 * Sets the reactive testing flag to indicate testing occurred today.
	 *
	 * @param flag true if reactive testing occurred, false otherwise
	 * @return the updated context with the new flag value
	 */
	StateMachineContext setReactivelyTestedToday(boolean flag);

}