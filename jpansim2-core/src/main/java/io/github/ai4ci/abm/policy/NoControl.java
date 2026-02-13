package io.github.ai4ci.abm.policy;

import io.github.ai4ci.abm.ImmutableOutbreakState.Builder;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.util.Sampler;

/**
 * A no operation policy model where no central control is applied to the
 * outbreak. This is effectively a placeholder policy model for simulations
 * where no interventions are applied.
 */
public enum NoControl implements PolicyModel {

	/**
	 * No central control of the outbreak is applied; the outbreak progresses
	 * without any interventions.
	 */
	DEFAULT {
		@Override
		public State.PolicyState nextState(
				Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng
		) {
			return DEFAULT;
		}
	};

}