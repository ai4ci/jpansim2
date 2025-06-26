package io.github.ai4ci.abm.policy;

import static io.github.ai4ci.abm.mechanics.StateUtils.branchPeopleTo;
import static io.github.ai4ci.abm.mechanics.StateUtils.randomlyScreen;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
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
		
		public void updateHistory(ImmutableOutbreakHistory.Builder builder, 
				OutbreakState current, StateMachineContext context, Sampler rng) {
			randomlyScreen(current,rng);
		}
		
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getLockdownTrigger().confidentlyGreaterThan( 
					ModelNav.modelParam(current).getLockdownStartTrigger(),
					0.95)
			) {
				branchPeopleTo(current, LockdownIsolation.ISOLATE);
				return LOCKDOWN;
			}
			return MONITOR;
		}
	},
	
	LOCKDOWN {
		
		public void updateHistory(ImmutableOutbreakHistory.Builder builder, 
				OutbreakState current, StateMachineContext context, Sampler rng) {
			randomlyScreen(current,rng);
		}
		
		public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getLockdownTrigger().confidentlyLessThan( 
					ModelNav.modelParam(current).getLockdownReleaseTrigger(),
					0.95)
			) {
				branchPeopleTo(current, LockdownIsolation.RELEASE);
				return MONITOR;
			}
			return LOCKDOWN;
		}
	}
	;
	
}