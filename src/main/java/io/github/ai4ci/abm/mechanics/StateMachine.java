package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.util.Sampler;

public class StateMachine implements Serializable {

	

	public static interface BehaviourState extends State<ImmutablePersonState.Builder, ImmutablePersonHistory.Builder, PersonState, BehaviourState> {}
	
	public static interface PolicyState extends State<ImmutableOutbreakState.Builder, ImmutableOutbreakHistory.Builder, OutbreakState, PolicyState> {}
	
	public void performHistoryUpdate(ImmutablePersonHistory.Builder builder, PersonState person, Sampler rng) {
		if (this.getState() instanceof BehaviourState) {
			BehaviourState state = (BehaviourState) this.getState();
			if (state.filter(person, context, rng))
				state.updateHistory(builder, person, context, rng);
		}
	}
	
	public void performHistoryUpdate(ImmutableOutbreakHistory.Builder builder, OutbreakState outbreak, Sampler rng) {
		if (this.getState() instanceof PolicyState) {
			PolicyState state = (PolicyState) this.getState();
			if (state.filter(outbreak, context, rng))
				state.updateHistory(builder, outbreak, context, rng);
		}
	}
	
	/** 
	 * UPdate the state machine and store the new state
	 * @param builder
	 * @param person
	 * @param rng
	 */
	public synchronized void performStateUpdate(ImmutablePersonState.Builder builder, PersonState person, Sampler rng) {
		if (this.getState() instanceof BehaviourState) {
			BehaviourState state = (BehaviourState) this.getState();
			if (state.filter(person, context, rng)) {
				BehaviourState next = state.nextState(builder, person, context, rng);
				currentState = next;
			}
		}
	}
	
	public synchronized void performStateUpdate(ImmutableOutbreakState.Builder builder, OutbreakState outbreak, Sampler rng) {
		if (this.getState() instanceof PolicyState) {
			PolicyState state = (PolicyState) this.getState();
			if (state.filter(outbreak, context, rng)) {
				PolicyState next = state.nextState(builder, outbreak, context, rng);
				currentState = next;
			}
		}
	}
	
	public StateMachine() {}
	
	public StateMachine(State<?,?,?,?> defaultStateMachineState, StateMachineContext context) {
		this.context = ModifiableStateMachineContext.create().from(context);
		this.currentState = defaultStateMachineState;
	}

//	public static State noop() {
//		return new State() {
//			@Override
//			public State nextState(ImmutablePersonState.Builder builder, PersonState person, StateMachineContext context, Sampler rng) {
//				return StateMachine.noop();
//			}
//		};
//	}
//	
//	public static State noopThenDefault() {
//		return new State() {
//			@Override
//			public State nextState(ImmutablePersonState.Builder builder, PersonState person, StateMachineContext context, Sampler rng) {
//				return context.getBaselineState();
//			}
//		};
//	}
//	
//	public static State delayThen(int delay, State nextState) {
//		return new State() {
//			int counter = delay;
//			@Override
//			public State nextState(ImmutablePersonState.Builder builder, PersonState person, StateMachineContext context, Sampler rng) {
//				if (counter == 0) return nextState;
//				counter -= 1;
//				return this;
//			}
//		};
//	}
	

	
	private ModifiableStateMachineContext context;
	private State<?,?,?,?> currentState;
	public State<?,?,?,?> getState() {return currentState;}
	
	public void init(PolicyState policy) {
		this.currentState = policy;
		this.context = ModifiableStateMachineContext.create()
				.setBaselineState(policy);
	}
	
	public void init(BehaviourState policy) {
		this.currentState = policy;
		this.context = ModifiableStateMachineContext.create()
				.setBaselineState(policy);
	}
	
	public static StateMachine stub() {
		return new StateMachine();
	}
	
	public void forceTo(BehaviourState state) {
		if (this.getState() instanceof BehaviourState) {
			synchronized(this) {
				branchToState(state);
				currentState = state;
			}
		} else {
			throw new RuntimeException("Cannot update a policy with a behaviour");
		}
	}
	
	public void forceTo(PolicyState state) {
		if (this.getState() instanceof PolicyState) {
			synchronized(this) {
				// If switching to a different behaviour model
				branchToState(state);
				currentState = state;
			}
		} else {
			throw new RuntimeException("Cannot update a behaviour with a policy");
		}
	}
	
	public void branchToState(State<?,?,?,?> state) {
		if (!state.getClass().equals(state.getClass()))
			context.pushState(currentState);
	}
	
	public String toString() {
		return this.getState().getName();
	}

	
	
}
