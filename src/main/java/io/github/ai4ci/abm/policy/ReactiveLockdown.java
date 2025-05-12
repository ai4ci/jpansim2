package io.github.ai4ci.abm.policy;

import static io.github.ai4ci.abm.mechanics.StateUtils.branchPeopleTo;

import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.behaviour.LockdownIsolation;
import io.github.ai4ci.abm.ImmutableOutbreakState.Builder;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;
import io.github.ai4ci.util.Sampler;

public enum ReactiveLockdown implements PolicyModel {
	
	MONITOR {
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getPresumedTestPositivePrevalence() > 0.05) {
				branchPeopleTo(current, LockdownIsolation.ISOLATE);
				return LOCKDOWN;
			}
			return MONITOR;
		}
	},
	
	LOCKDOWN {
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getPresumedTestPositivePrevalence() < 0.01) {
				branchPeopleTo(current, LockdownIsolation.RELEASE);
				return MONITOR;
			}
			return LOCKDOWN;
		}
	}
	;
	
}