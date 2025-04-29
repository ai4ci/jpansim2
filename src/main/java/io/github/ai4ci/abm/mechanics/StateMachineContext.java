package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value.Modifiable;

import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;

@Modifiable
public interface StateMachineContext extends Serializable {
	public State<?,?,?,?> getBaselineState();
	public List<State<?,?,?,?>> getLastState();
	public List<Boolean> getFlags();
	
	public default void pushState(State<?,?,?,?> state) {
		this.getLastState().add(0, state);
	}
	
	public default BehaviourState pullBehaviour() {
		State<?,?,?,?> out;
		if (this.getLastState().size() > 0) {
			out = this.getLastState().get(0);
			this.getLastState().remove(0);
			
		} else {
			out = this.getBaselineState();
		}
		if (out instanceof BehaviourState) return (BehaviourState) out;
		throw new RuntimeException("Wrong type of state");
	}
	
	public default PolicyState pullPolicy() {
		State<?,?,?,?> out;
		if (this.getLastState().size() > 0) {
			out = this.getLastState().get(0);
			this.getLastState().remove(0);
			
		} else {
			out = this.getBaselineState();
		}
		if (out instanceof PolicyState) return (PolicyState) out;
		throw new RuntimeException("Wrong type of state");
	}
	
	public default boolean pullFlag() {
		if (this.getFlags().isEmpty()) return false;
		boolean tmp = this.getFlags().get(0);
		this.getFlags().remove(0);
		return tmp;
	}
	
	public default void pushFlag(boolean flag) {
		this.getFlags().add(0, flag);
	}
	
}