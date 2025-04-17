package io.github.ai4ci.abm;

import static io.github.ai4ci.util.StateUtils.branchTo;

import io.github.ai4ci.abm.BehaviourModel.LockdownIsolation;
import io.github.ai4ci.abm.ImmutableOutbreakState.Builder;
import io.github.ai4ci.abm.StateMachine.PolicyState;
import io.github.ai4ci.util.Sampler;

public interface PolicyModel extends StateMachine.PolicyState {
	
	default public void updateHistory(ImmutableOutbreakHistory.Builder builder, 
			OutbreakState person, StateMachineContext context, Sampler rng) {}
	
	public static enum NoControl implements PolicyModel {
		
		DEFAULT {
			@Override
			public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
					Sampler rng) {
				return DEFAULT;
			}
		};
		
		public String getName() {return NoControl.class.getSimpleName()+"."+this.name();}
	}
	
	
	public static enum ReactiveLockdown implements PolicyModel {
		
		MONITOR {
			public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
					Sampler rng) {
				if (current.getPresumedTestPositivePrevalence() > 0.05) {
					branchTo(current, LockdownIsolation.ISOLATE);
					return LOCKDOWN;
				}
				return MONITOR;
			}
		},
		
		LOCKDOWN {
			public PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
					Sampler rng) {
				if (current.getPresumedTestPositivePrevalence() < 0.01) {
					branchTo(current, LockdownIsolation.RELEASE);
					return MONITOR;
				}
				return LOCKDOWN;
			}
		}
		;
		public String getName() {return ReactiveLockdown.class.getSimpleName()+"."+this.name();}
	}

}
