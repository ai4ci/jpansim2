package io.github.ai4ci.abm.behaviour;

import io.github.ai4ci.abm.mechanics.StateMachine;

/**
 * Called during an update cycle before any changes have been made
 * This means any references to state refers to current state, but 
 * any references to history refer to the previous state.
 */
public interface BehaviourModel extends StateMachine.BehaviourState {
	
}
