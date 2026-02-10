package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.ModelNav.modelState;
import static io.github.ai4ci.flow.mechanics.StateUtils.*;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModelNav;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.flow.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.Sampler;

/**
 * A behaviour model for people who will test reactively with LFTs, and then
 * reflexively with PCRs. This is a more compliant behaviour than the default
 * symptomatic testing, but less compliant than the reactive PCR testing model.
 * 
 * @author Rob Challen
 *
 */
public enum SmartAgentLFTTesting implements BehaviourModel, DefaultNoTesting {
	
	/**
	 * Patient will probably test using a LFT if they have symptoms. Results are
	 * available immediately so they will isolate immediately if they have a
	 * positive test, but will not isolate if they have symptoms but a negative
	 * test. If they have a positive LFT, they will then get a PCR test to confirm,
	 * and isolate while awaiting the result.
	 */
	REACTIVE_LFT {

		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			
			if (isSymptomaticAndCompliant(person, 2) || isHighRiskOfInfectionAndCompliant(person, 
						ModelNav.modelParam(person).getSmartAppRiskTrigger()
					)  ) {
				if (isLFTTestingAllowed(person)) doLFT(builder,person, context);
			}
			
		}

		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( isPositiveTestToday(person) ) { 
				decreaseSociabilityIfCompliant(builder,person);
				return REFLEX_PCR;
			} else {
				return REACTIVE_LFT;
			}
		}
		
	},
	/** Patient will test with PCR if they have symptoms and a positive LFT, then wait for the result. */
	REFLEX_PCR {
		
		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			
			// Maybe put a delay in here otherwise it will be next day PCR.
			if (isPCRTestingAllowed(person)) doPCR(builder,person, context);
			
		}

		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( context.isReactivelyTestedToday() ) { 
				decreaseSociabilityIfCompliant(builder,person);
				return AWAIT_PCR;
			} else {
				if (!isPCRTestingAllowed(person)) {
					resetBehaviour(builder,person);
					return REACTIVE_LFT;
				}
				return REFLEX_PCR;
			}
		}
	},
	
	/** Patient will isolate if they have a positive PCR, but will wait for the result. */
	AWAIT_PCR {
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (person.isLastTestExactly(Result.PENDING)) return AWAIT_PCR;
			if (person.isLastTestExactly(Result.POSITIVE)) return SELF_ISOLATE;
			resetBehaviour(builder,person);
			return REACTIVE_LFT;
		}
	},
	/** Patient will isolate if they have a positive PCR for an average duration
	 * of the presumed disease infectious period. */
	SELF_ISOLATE {
		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (!person.isCompliant()) {
				resetBehaviour(builder,person);
				return Symptomatic.DEFAULT;
			}
			if (!person.isSymptomatic()) {
				if (rng.periodTrigger(modelState(person).getPresumedInfectiousPeriod(),0.95)) {
					resetBehaviour(builder,person);
					return REACTIVE_LFT;
				}
			}
			complianceFatigue(builder,person);
			return SELF_ISOLATE;
		}		
	};
	
	
}