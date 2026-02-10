package io.github.ai4ci.abm.policy;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.util.Sampler;

/**
 * Called during an update cycle before any changes have been made This means
 * any references to state refers to current state, but any references to
 * history refer to the previous state. This overrrides the history update stage
 * with a no-op as there is not really any testing or similar activity that
 * takes place in the policy models.
 */
public interface PolicyModel extends State.PolicyState {

	@Override
	default public void updateHistory(ImmutableOutbreakHistory.Builder builder, OutbreakState person,
			StateMachineContext context, Sampler rng) {
	}

}
