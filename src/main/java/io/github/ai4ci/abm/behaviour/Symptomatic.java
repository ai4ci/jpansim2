package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.mechanics.StateUtils.complianceRestoreSlowly;
import static io.github.ai4ci.abm.mechanics.StateUtils.decreaseSociabilitySlowlyIfSymptomatic;
import static io.github.ai4ci.abm.mechanics.StateUtils.restoreSociabilitySlowlyIfAsymptomatic;
import static io.github.ai4ci.abm.mechanics.StateUtils.toLastBranchPoint;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

public enum Symptomatic implements BehaviourModel, DefaultNoTesting {
	
	DEFAULT {
		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			decreaseSociabilitySlowlyIfSymptomatic(builder,person);
			restoreSociabilitySlowlyIfAsymptomatic(builder,person);
			complianceRestoreSlowly(builder,person);
			// N.B.: this could lead to frequent flip-flopping from 
			// compliance to non compliance:
			if (person.isCompliant()) return toLastBranchPoint(context);
			return Symptomatic.DEFAULT;
		}
	};
	
}