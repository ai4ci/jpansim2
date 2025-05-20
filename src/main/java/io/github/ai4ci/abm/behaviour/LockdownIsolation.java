package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.mechanics.StateUtils.complianceFatigue;
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
	
	/**
	 * Everyone enters minimal mobility state immediately in instruction to
	 * lock down (@see {@link WAIT}). This is triggered by 
	 * {@link io.github.ai4ci.abm.policy.ReactiveLockdown#MONITOR}. This is 
	 * centrally enforced and results in the person branching from their previous
	 * behaviour pausing that.
	 */
	ISOLATE {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			decreaseSociabilityStrictly(builder,person);
			return WAIT;
		}
	},
	
	/**
	 * In the locked down waiting state people's compliance levels will decrease
	 * linearly. If the person's compliance level falls too low the person will
	 * randomly become non-compliant to lockdown.
	 * <!-- (@see {@link NonCompliant#DEFAULT}). --> 
	 * In the case of becoming non compliant to lockdown the person will return 
	 * to whatever they were doing before lockdown (but with reduced compliance)
	 */
	WAIT {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (!person.isCompliant()) {
				// return branchTo(person, NonCompliant.DEFAULT);
				return toLastBranchPoint(context);
			} else {	
				complianceFatigue(builder,person);
				return WAIT;
			}
		}
	},
	
	/**
	 * After the lockdown is released, triggered by {@link io.github.ai4ci.abm.policy.ReactiveLockdown#LOCKDOWN},
	 * people return to a mobility state larger than their baseline,  
	 * gradually return to their default mobility over the next 10 days, and 
	 * return to whatever state they were in before the lockdown was triggered.
	 */
	RELEASE {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			builder.setMobilityModifier(1.25);
			return graduallyRestoreBehaviour(10, toLastBranchPoint(context));
		}
	},
	
//	/**
//	 * Between lockdowns peoples compliance levels will slowly return to normal
//	 * unless they are symptomatic.
//	 */
//	RELAX {
//		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
//				PersonState person, StateMachineContext context, Sampler rng) {
//			if (person.isSymptomatic()) return toLastBranchPoint(context);
//			complianceRestoreSlowly(builder,person);
//			return RELAX;
//		}
//	}
	
	;
	
	
	
}