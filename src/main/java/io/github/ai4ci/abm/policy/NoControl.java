package io.github.ai4ci.abm.policy;

import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.ImmutableOutbreakState.Builder;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;
import io.github.ai4ci.util.Sampler;

public enum NoControl implements PolicyModel {
	
	DEFAULT {
		@Override
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			return DEFAULT;
		}
	};
	
}