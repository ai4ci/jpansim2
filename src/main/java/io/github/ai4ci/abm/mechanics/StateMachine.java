package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.util.Sampler;

public class StateMachine implements Serializable {

	

	public static interface BehaviourState extends State<ImmutablePersonState.Builder, ImmutablePersonHistory.Builder, PersonState, BehaviourState> {
		public default String name() {return "Behaviour";}
	}
	
	public static interface PolicyState extends State<ImmutableOutbreakState.Builder, ImmutableOutbreakHistory.Builder, OutbreakState, PolicyState> {
		public default String name() {return "Policy";}
	}
	
	public void prepareUpdate() {
		this.context.resetFlags();
	}
	
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
	 * Update the state machine and store the new state
	 */
	public synchronized void performStateUpdate(ImmutablePersonState.Builder builder, PersonState person, Sampler rng) {
		if (person.isDead()) {
			currentState = NonCompliant.DEAD.nextState(builder, person, context, rng);
		} else {
			if (this.getState() instanceof BehaviourState) {
				BehaviourState state = (BehaviourState) this.getState();
				if (state.filter(person, context, rng)) {
					BehaviourState next = state.nextState(builder, person, context, rng);
					currentState = next;
				}
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
		if (state.equals(NonCompliant.DEAD)) return;
		if (this.getState() instanceof BehaviourState) {
			synchronized(this) {
				rememberCurrentState(state);
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
				rememberCurrentState(state);
				currentState = state;
			}
		} else {
			throw new RuntimeException("Cannot update a behaviour with a policy");
		}
	}
	
	/**
	 * Remembers the current state if the behaviour is switching from one type
	 * to another (each behaviour model is an enum so the class of a group of
	 * behaviour states is the same). This way we only remember state branches
	 * if it directs people to a new state within a different model of behaviour.
	 * @param state
	 */
	public void rememberCurrentState(State<?,?,?,?> state) {
		if (state instanceof Enum && getState() instanceof Enum) {
			//Don't remember if this is a different state from same behaviour model
			if (((Enum<?>) state).getDeclaringClass().equals(((Enum<?>) getState()).getDeclaringClass())) return;
		}
		context.pushState(currentState);
	}
	
	public void returnFromBranch() {
		if (this.getState() instanceof BehaviourState) 
			currentState = context.pullBehaviour();
		if (this.getState() instanceof PolicyState) {
			currentState = context.pullPolicy();
		}
	}
	
	public String toString() {
		return this.getState().getName();
	}

	
	
}
