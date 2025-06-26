package io.github.ai4ci.abm.behaviour;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

public enum NonCompliant implements BehaviourModel, DefaultNoTesting {
	
	ALIVE {
		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			builder.setAppUseModifier(1D);
			builder.setMobilityModifier(1D);
			builder.setTransmissibilityModifier(1D);
			builder.setComplianceModifier(1D);
			builder.setSusceptibilityModifier(1D);
			return NonCompliant.ALIVE;
		}
	},
	
	DEAD {
		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			builder.setAppUseModifier(0D);
			builder.setMobilityModifier(0D);
			builder.setTransmissibilityModifier(0D);
			builder.setSusceptibilityModifier(0D);
			return NonCompliant.DEAD;
		}
	};
	
	
	
	
	
}