package io.github.ai4ci.flow.mechanics;

import java.io.Serializable;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.util.Sampler;

/**
 * This abstraction lets us have policy and behaviour models sharing the same
 * state model framework, so outbreak and person updates can follow the same 
 * pattern. This is the super-type for {@link State.PolicyState} and 
 * {@link State.BehaviourState}.
 * 
 * <br><br> Each state machine state has 2 methods in it that alter the behaviour of the 
 * outbreak or agent. firstly the {@link #updateHistory(Object, Object, StateMachineContext, Sampler)}
 * method which is run at the end of each day and allows the population of 
 * data points that are specific to  {@link io.github.ai4ci.abm.PersonHistory} 
 * or {@link io.github.ai4ci.abm.OutbreakHistory}. Usually this means 
 * 
 * @param <BUILDER> builder of either outbreak state (policy) or person state (behaviour). 
 * @param <HISTORY> builder of either outbreak history (policy) or person history (behaviour). 
 * @param <STATE> the current state of the entity (e.g. OutbreakState (policy) or behaviour state)
 * @param <X> the super-type of allowable state models.
 */
public interface State<BUILDER,HISTORY,STATE,X extends State<BUILDER,HISTORY,STATE,X>> extends Serializable {
	
	/**
	 * Marks a state model as being a component of a behaviour relevant to people
	 * within the simulation 
	 */
	interface BehaviourState extends State<ImmutablePersonState.Builder, ImmutablePersonHistory.Builder, PersonState, BehaviourState> {
		public default String name() {return "Behaviour";}
	}

	/**
	 * Marks a state model state as being a component of a policy for managing 
	 * to the outbreak simulation as a whole 
	 */
	interface PolicyState extends State<ImmutableOutbreakState.Builder, ImmutableOutbreakHistory.Builder, OutbreakState, PolicyState> {
		public default String name() {return "Policy";}
	}

	/**
	 * Depending on the type this updated the outbreak or person history, and
	 * this is the hook for changing the testing, or adding in contacts / exposures
	 * if we are looking at people. Use case is less clear for outbreak/policy, as there 
	 * is less stored here. The implementation for outbreak (PolicyModel) makes this a no-op.
	 * This is called during the history stage of the update cycle, and effectively
	 * performs actions that are done at the end of the day.
	 * 
	 * @param builder the next PersonHistory builder (usually)
	 * @param current the current PersonHistory
	 * @param context the previous states and other context for the state model
	 * @param rng the sampler
	 */
	void updateHistory(HISTORY builder, STATE current, StateMachineContext context, Sampler rng); 
	
	/**
	 * This is the hook for changing the behaviour in the model in terms of mobility, 
	 * compliance etc in person/behavioural models, or changing system wide 
	 * properties in outbreak/policy models. These changes can be thought of
	 * as happening at the start of a day in relation to the information 
	 * available from the day before. Because of this limitation it is difficult
	 * for the model to react to things that happen on the same day. To some 
	 * extent reflex testing is possible as the result of a test can be known 
	 * instantly.  
	 * 
	 * <br><br> This method both decides what the future behaviour or policy 
	 * will be based on the current state, but can also change the future state
	 * by calling the builder parameter's methods. This is the main 
	 * place in which future state is affected by behaviour, and a lot of the
	 * effects are implemented as static methods in {@link StateUtils}. 
	 * 
	 * <br><br> The implementation of the nextState method in policy models 
	 * can influence both the next state of the Outbreak (via the builder) but 
	 * also can influence the next state of people within the outbreak indirectly
	 * by iterating through the agents and calling
	 * {@link StateUtils#branchPeopleTo(io.github.ai4ci.abm.OutbreakState, io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState)} 
	 * which will force an immediate change in the individuals persons
	 * behaviour model state, {@link io.github.ai4ci.abm.Person#getStateMachine()} 
	 * on each of them. 
	 * 
	 * <br><br> Other changes
	 * are possible by interacting with {@link io.github.ai4ci.abm.Person#getNextState()} 
	 * (the next person state builder, which will be populated by the time this
	 * method is called). Changes made in this way by a policy model could be changed 
	 * back if dictated by the person's behaviour model so this is less reliable 
	 * than authoring a behaviour that specifically updates the persons state in 
	 * response to a policy state change. 
	 * 
	 * @param builder the builder for the future state of the agent or outbreak.
	 * @param current the current state of the agent or outbreak.
	 * @param context the previous states and other context for the state model
	 * @param rng a random number generator 
	 * @return The behaviour or policy state for the next time step, but side 
	 *   effects on the state of the builder are expected. 
	 */
	X nextState(BUILDER builder, STATE current, StateMachineContext context, Sampler rng);

	String name();
	
	default String getName() {
		return this.getEnumClass().getSimpleName()+"."+name();
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * The states in the state model are all enum fields. This provides a hook 
	 * back into the definition so we can tell if it is a policy or behaviour 
	 * model state. This is largely used for displaying the name of the policy
	 * or behaviour model from the individual state it is in.
	 * @return the class of the Enum of this policy or behaviour state.
	 */
	default Class<State<BUILDER,HISTORY,STATE,X>> getEnumClass() {
		return (Class<State<BUILDER, HISTORY, STATE, X>>) ((Enum<?>) this).getDeclaringClass();
	}
}