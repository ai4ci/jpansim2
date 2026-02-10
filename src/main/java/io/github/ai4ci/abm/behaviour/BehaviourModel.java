package io.github.ai4ci.abm.behaviour;

import io.github.ai4ci.flow.mechanics.State;

/**
 * Called during an update cycle before any changes have been made
 * This means any references to state refers to current state, but 
 * any references to history refer to the previous state. This does
 * not override any BehaviourState functions but is here because
 * {@link io.github.ai4ci.abm.policy.PolicyModel the policy model} does
 * override the first phase
 */
public interface BehaviourModel extends State.BehaviourState {
	
}
