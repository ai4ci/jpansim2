package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.util.Sampler;

public interface State<BUILDER,HISTORY,STATE,X extends State<BUILDER,HISTORY,STATE,X>> extends Serializable {
	
	default boolean filter(STATE person, StateMachineContext context, Sampler rng) {
		return true;
	}
	
	/**
	 * Depending on the type this updated the outbreak or person history, and
	 * this will include testing in the person history.
	 * @param builder
	 * @param current
	 * @param context
	 * @param rng
	 */
	void updateHistory(HISTORY builder, STATE current, StateMachineContext context, Sampler rng); 
	
	X nextState(BUILDER builder, STATE current, StateMachineContext context, Sampler rng);

	String getName();
}