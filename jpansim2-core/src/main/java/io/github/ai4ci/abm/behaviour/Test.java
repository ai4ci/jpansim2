package io.github.ai4ci.abm.behaviour;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.flow.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

/**
 * A test behaviour model that does nothing.
 *
 * @author Rob Challen
 */
public enum Test implements BehaviourModel, DefaultNoTesting {

	/** A test behaviour model that does nothing. */
	NONE {
		@Override
		public State.BehaviourState nextState(
				ImmutablePersonState.Builder builder, PersonState person,
				StateMachineContext context, Sampler rng
		) {
			return Test.NONE;
		}
	};

}