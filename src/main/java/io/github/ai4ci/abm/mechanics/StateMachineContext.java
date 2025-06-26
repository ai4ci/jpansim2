package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;
import java.util.List;

import org.immutables.value.Value.Modifiable;

import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;

@Modifiable
public interface StateMachineContext extends Serializable {
	public State<?,?,?,?> getBaselineState();
	public List<State<?,?,?,?>> getBranchState();
	public boolean isReactivelyTestedToday();
	
	public default void pushState(State<?,?,?,?> state) {
		this.getBranchState().add(0, state);
	}
	
	public default BehaviourState pullBehaviour() {
		State<?,?,?,?> out;
		if (this.getBranchState().size() > 0) {
			out = this.getBranchState().get(0);
			this.getBranchState().remove(0);
			
		} else {
			out = this.getBaselineState();
		}
		if (out instanceof BehaviourState) return (BehaviourState) out;
		throw new RuntimeException("Wrong type of state");
	}
	
	public default PolicyState pullPolicy() {
		State<?,?,?,?> out;
		if (this.getBranchState().size() > 0) {
			out = this.getBranchState().get(0);
			this.getBranchState().remove(0);
			
		} else {
			out = this.getBaselineState();
		}
		if (out instanceof PolicyState) return (PolicyState) out;
		throw new RuntimeException("Wrong type of state");
	}
	
	public default void resetFlags() {
		this.setReactivelyTestedToday(false);
	}
	
	StateMachineContext setReactivelyTestedToday(boolean flag);
	
}