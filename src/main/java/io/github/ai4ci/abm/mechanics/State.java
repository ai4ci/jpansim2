package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;

import io.github.ai4ci.util.Sampler;

/**
 * This abstraction lets us have policy and behaviour models sharing the same
 * state model framework, so outbreak and person updates can follow the same 
 * pattern. This is the supertype for StateMachine.PolicyState and 
 * StateMachine.BehaviourState.
 * @param <BUILDER> builder of either outbreak state (policy) or person state (behaviour). 
 * @param <HISTORY> builder of either outbreak history (policy) or person history (behaviour). 
 * @param <STATE> the current state of the entity (e.g. OutbreakState (policy) or behaviour state)
 * @param <X> the super-type of allowable state models.
 */
public interface State<BUILDER,HISTORY,STATE,X extends State<BUILDER,HISTORY,STATE,X>> extends Serializable {
	
	/**
	 * 
	 * @param person
	 * @param context
	 * @param rng
	 * @return
	 */
	default boolean filter(STATE person, StateMachineContext context, Sampler rng) {
		return true;
	}
	
	/**
	 * Depending on the type this updated the outbreak or person history, and
	 * this is the hook for changing the testing, or adding in contacts / exposures
	 * if we are looking at people. Use case is less clear for outbreak/policy, as there 
	 * is less stored here. The implementation for outbreak (PolicyModel) makes this a no-op.
	 * This is called during the history stage of the update cycle.
	 * 
	 * @param builder the next PersonHistory builder (usually)
	 * @param current the current PersonHistory
	 * @param context
	 * @param rng the sampler
	 */
	void updateHistory(HISTORY builder, STATE current, StateMachineContext context, Sampler rng); 
	
	/**
	 * This is the hook for changing the behaviour in the model in terms of mobility, 
	 * compliance etc in person/behavioural models, or changing system wide 
	 * properties in outbreak/policy models. The implementation of the next
	 * state in policy models also can influence PersonState but should do so 
	 * using StateUtils.branchPeopleTo which will force a change in individuals
	 * behaviour models. or iterating through the agents and calling 
	 * ps.getStateMachine().forceTo(behaviour) on each of them; Other changes
	 * are possible by interacting with ps.getNextState (the next person state
	 * builder) but changes here may be changed back if dictated by the person's
	 * behaviour state so less reliable that creating a behaviour that 
	 * updates the persons state in response to a policy state. This is 
	 * done this way so that the behaviour state of an agent is pushed to a 
	 * stack when behaviour is forced and allow it to revert after
	 * 
	 * @param builder the next PersonState builder.
	 * @param current
	 * @param context
	 * @param rng
	 * @return
	 */
	X nextState(BUILDER builder, STATE current, StateMachineContext context, Sampler rng);

	String name();
	
	default String getName() {
		return this.getEnumClass().getSimpleName()+"."+name();
	}
	
	@SuppressWarnings("unchecked")
	default Class<State<BUILDER,HISTORY,STATE,X>> getEnumClass() {
		return (Class<State<BUILDER, HISTORY, STATE, X>>) ((Enum<?>) this).getDeclaringClass();
	}
}