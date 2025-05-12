package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.mechanics.StateUtils.*;
import static io.github.ai4ci.util.ModelNav.*;

import io.github.ai4ci.abm.ImmutablePersonHistory.Builder;
import io.github.ai4ci.abm.mechanics.StateMachine;

/**
 * Called during an update cycle before any changes have been made
 * This means any references to state refers to current state, but 
 * any references to history refer to the previous state.
 */
public interface BehaviourModel extends StateMachine.BehaviourState {
	
//	public static enum SmartAgent implements BehaviourModel, DefaultNoTesting {
//		
//		DEFAULT {
//
//			@Override
//			public void updateHistory(Builder builder, PersonState current, StateMachineContext context, Sampler rng) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public BehaviourState nextState(ImmutablePersonState.Builder builder,
//					PersonState current, StateMachineContext context, Sampler rng) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//			
//		};
//		
//		public String getName() {return this.getClass().getSimpleName()+"."+this.name();}
//		
//	}
	
}
