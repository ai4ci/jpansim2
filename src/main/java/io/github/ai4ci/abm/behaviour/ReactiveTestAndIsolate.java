package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.mechanics.StateUtils.complianceFatigue;
import static io.github.ai4ci.abm.mechanics.StateUtils.decreaseSociabilityIfCompliant;
import static io.github.ai4ci.abm.mechanics.StateUtils.isTestedToday;
import static io.github.ai4ci.abm.mechanics.StateUtils.resetBehaviour;
import static io.github.ai4ci.abm.mechanics.StateUtils.seekPcrIfSymptomatic;
import static io.github.ai4ci.util.ModelNav.modelState;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

public enum ReactiveTestAndIsolate implements BehaviourModel, DefaultNoTesting {

	/**
	 * Patient will probably test if they have symptoms, then wait for the
	 * result. They will self isolate if they 
	 */
	REACTIVE_PCR {

		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			seekPcrIfSymptomatic(builder, person, 2);
		}

		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( isTestedToday(person) ) { 
				decreaseSociabilityIfCompliant(builder,person);
				return AWAIT_PCR;
			} else {
				return REACTIVE_PCR;
			}
		}
		
	},
	
	AWAIT_PCR {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (person.isLastTestExactly(Result.PENDING)) return AWAIT_PCR;
			if (person.isLastTestExactly(Result.POSITIVE)) return SELF_ISOLATE;
			resetBehaviour(builder,person);
			return REACTIVE_PCR;
		}
	},
	
	SELF_ISOLATE {
		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (!person.isCompliant()) {
				resetBehaviour(builder,person);
				return NonCompliant.DEFAULT;
			}
			if (!person.isSymptomatic()) {
				if (rng.periodTrigger(modelState(person).getPresumedInfectiousPeriod(), 0.95)) {
					resetBehaviour(builder,person);
					return REACTIVE_PCR;
				}
			}
			complianceFatigue(builder,person);
			return SELF_ISOLATE;
		}		
	};
	
	
}