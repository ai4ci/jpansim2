package io.github.ai4ci.abm.policy;

import static io.github.ai4ci.abm.mechanics.StateUtils.branchPeopleTo;

import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.behaviour.LockdownIsolation;
import io.github.ai4ci.abm.ImmutableOutbreakState.Builder;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.PolicyState;
import io.github.ai4ci.util.ModelNav;
import io.github.ai4ci.util.Sampler;

public enum ReactiveLockdown implements PolicyModel {
	
	/**
	 * Monitor the outbreak using test positive rate as a proxy for prevalence.
	 * I.e. test positives over the last presumed infectious perion divided by 
	 * population size at any given time point. This is test positives by result
	 * date and not sample date
	 * When test 
	 */
	MONITOR {
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getPresumedTestPositivePrevalence() > 
				ModelNav.modelParam(current).getLockdownStartPrevalenceTrigger()
			) {
				branchPeopleTo(current, LockdownIsolation.ISOLATE);
				return LOCKDOWN;
			}
			return MONITOR;
		}
	},
	
	LOCKDOWN {
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getPresumedTestPositivePrevalence() < 
					ModelNav.modelParam(current).getLockdownReleasePrevalenceTrigger()
					) {
				branchPeopleTo(current, LockdownIsolation.RELEASE);
				return MONITOR;
			}
			return LOCKDOWN;
		}
	}
	;
	
}