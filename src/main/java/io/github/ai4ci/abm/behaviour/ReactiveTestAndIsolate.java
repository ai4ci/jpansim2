package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.ModelNav.modelState;
import static io.github.ai4ci.flow.mechanics.StateUtils.complianceFatigue;
import static io.github.ai4ci.flow.mechanics.StateUtils.decreaseSociabilityIfCompliant;
import static io.github.ai4ci.flow.mechanics.StateUtils.resetBehaviour;
import static io.github.ai4ci.flow.mechanics.StateUtils.seekPcrIfSymptomatic;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.flow.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

/**
 * Reactive test and isolate behaviour model. This is the behaviour of a person who
 * will seek a test if they have symptoms, and then self isolate if they are
 * positive. This is a common policy for many diseases, and is used as a baseline
 * for comparison with other compliant behaviour models.
 * 
 * @author Rob Challen
 */
public enum ReactiveTestAndIsolate implements BehaviourModel, DefaultNoTesting {

	/**
	 * Patient will probably test if they have symptoms, then wait for the
	 * result. They will self isolate if they are positive.
	 */
	REACTIVE_PCR {

		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			seekPcrIfSymptomatic(builder, person, context, 2);
		}

		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( context.isReactivelyTestedToday() ) { 
				decreaseSociabilityIfCompliant(builder, person);
				return AWAIT_PCR;
			} else {
				return REACTIVE_PCR;
			}
		}
		
	},
	/** Awaiting PCR result. If positive then self isolate, if negative then return to normal behaviour */
	AWAIT_PCR {
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (person.isLastTestExactly(Result.PENDING)) return AWAIT_PCR;
			if (person.isLastTestExactly(Result.POSITIVE)) return SELF_ISOLATE;
			resetBehaviour(builder,person);
			return REACTIVE_PCR;
		}
	},
	/** Self isolating. Will stay in this state for the duration of the infectious period, then return to normal behaviour. */
	SELF_ISOLATE {
		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (!person.isCompliant()) {
				resetBehaviour(builder,person);
				return Symptomatic.DEFAULT;
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