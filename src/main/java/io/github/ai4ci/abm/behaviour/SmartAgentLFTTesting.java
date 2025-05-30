package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.mechanics.StateUtils.*;
import static io.github.ai4ci.util.ModelNav.modelState;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.mechanics.StateMachineContext;
import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.abm.mechanics.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.ModelNav;
import io.github.ai4ci.util.Sampler;

public enum SmartAgentLFTTesting implements BehaviourModel, DefaultNoTesting {
	
	/**
	 * Patient will probably test if they have symptoms, then wait for the
	 * result.
	 */
	REACTIVE_LFT {

		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			
			if (isSymptomaticAndCompliant(person, 2) || isHighRiskOfInfectionAndCompliant(person, 
						ModelNav.modelParam(person).getSmartAppRiskTrigger()
					)  ) {
				if (isLFTTestingAllowed(person)) doLFT(builder,person);
			}
			
		}

		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( isPositiveTestToday(person) ) { 
				decreaseSociabilityIfCompliant(builder,person);
				return REFLEX_PCR;
			} else {
				return REACTIVE_LFT;
			}
		}
		
	},
	
	REFLEX_PCR {
		
		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			
			// Maybe put a delay in here otherwise it will be next day PCR.
			if (isPCRTestingAllowed(person)) doPCR(builder,person);
			
		}

		@Override
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( isTestedToday(person) ) { 
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
	
	
	AWAIT_PCR {
		public BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (person.isLastTestExactly(Result.PENDING)) return AWAIT_PCR;
			if (person.isLastTestExactly(Result.POSITIVE)) return SELF_ISOLATE;
			resetBehaviour(builder,person);
			return REACTIVE_LFT;
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