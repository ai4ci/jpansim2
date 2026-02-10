package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.flow.mechanics.StateUtils.toLastBranchPoint;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.flow.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

/**
 * Non compliant behaviour model. This is the behaviour of a person who does not
 * comply with any policies, and is used as a baseline for comparison with
 * compliant behaviour models, and also as a branch target for models where
 * people either die or become compliant.
 * 
 * @author Rob Challen
 */
public enum NonCompliant implements BehaviourModel, DefaultNoTesting {
	
	/** Non compliant but alive */
	ALIVE {
		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			builder.setAppUseModifier(1D);
			builder.setMobilityModifier(1D);
			builder.setTransmissibilityModifier(1D);
			builder.setComplianceModifier(1D);
			builder.setSusceptibilityModifier(1D);
			// Spontaneously start complying again after 30 days
			if (rng.periodTrigger(30)) return toLastBranchPoint(context);
			return NonCompliant.ALIVE;
		}
	},
	/** Non compliant and dead. This is the state in which all dead people end up in */
	DEAD {
		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			builder.setAppUseModifier(0D);
			builder.setMobilityModifier(0D);
			builder.setTransmissibilityModifier(0D);
			builder.setSusceptibilityModifier(0D);
			return NonCompliant.DEAD;
		}
	};
	
	
	
	
	
}