package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.mechanics.StateUtils.branchTo;
import static io.github.ai4ci.abm.mechanics.StateUtils.complianceFatigue;
import static io.github.ai4ci.abm.mechanics.StateUtils.complianceRestoreSlowly;
import static io.github.ai4ci.abm.mechanics.StateUtils.decreaseSociabilityStrictly;
import static io.github.ai4ci.abm.mechanics.StateUtils.graduallyRestoreBehaviour;
import static io.github.ai4ci.abm.mechanics.StateUtils.toLastBranchPoint;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateUtils.DoesPCRIfSymptomatic;
import io.github.ai4ci.util.Sampler;

public enum LockdownIsolation implements BehaviourModel, DoesPCRIfSymptomatic {
	
	ISOLATE {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			decreaseSociabilityStrictly(builder,person);
			return WAIT;
		}
	},
	
	WAIT {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (!person.isCompliant()) {
				return branchTo(person, NonCompliant.DEFAULT);
			} else {	
				complianceFatigue(builder,person);
				return WAIT;
			}
		}
	},
	
	RELEASE {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			builder.setMobilityModifier(1.25);
			return graduallyRestoreBehaviour(10,RELAX);
		}
	},
	
	RELAX {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (person.isSymptomatic()) return toLastBranchPoint(context);
			complianceRestoreSlowly(builder,person);
			return RELAX;
		}
	}
	
	;
	
	
	
}