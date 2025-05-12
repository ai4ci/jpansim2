package io.github.ai4ci.abm.policy;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.mechanics.StateMachine;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.util.Sampler;

public interface PolicyModel extends StateMachine.PolicyState {
	
	default public void updateHistory(ImmutableOutbreakHistory.Builder builder, 
			OutbreakState person, StateMachineContext context, Sampler rng) {}

}
