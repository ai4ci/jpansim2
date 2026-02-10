package io.github.ai4ci.abm.behaviour;

import static io.github.ai4ci.abm.ModelNav.modelState;
import static io.github.ai4ci.flow.mechanics.StateUtils.complianceFatigue;
import static io.github.ai4ci.flow.mechanics.StateUtils.decreaseSociabilityIfCompliant;
import static io.github.ai4ci.flow.mechanics.StateUtils.doPCR;
import static io.github.ai4ci.flow.mechanics.StateUtils.isHighRiskOfInfectionAndCompliant;
import static io.github.ai4ci.flow.mechanics.StateUtils.isPCRTestingAllowed;
import static io.github.ai4ci.flow.mechanics.StateUtils.isSymptomaticAndCompliant;
import static io.github.ai4ci.flow.mechanics.StateUtils.resetBehaviour;

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
 * Smart agent testing behaviour model. This is the behaviour of a person who
 * will test reactively if they have symptoms, and then self isolate if they
 * test positive. They will also test reactively if they are at high risk of
 * infection (e.g. due to a contact notification) and compliant, and then self
 * isolate if they test positive. This is used as a baseline for comparison with
 * other compliant behaviour models.
 * 
 * @author Rob Challen
 */
public enum SmartAgentTesting implements BehaviourModel, DefaultNoTesting {
	
	/**
	 * Patient will probably get a PCR test if they have symptoms, then wait for the
	 * result. They will also test if they are at high risk of infection (e.g. due
	 * to a contact notification) and compliant, and then wait for the result.
	 */
	REACTIVE_PCR {

		@Override
		public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			
			if (isSymptomaticAndCompliant(person, 2) || isHighRiskOfInfectionAndCompliant(person, 
						ModelNav.modelParam(person).getSmartAppRiskTrigger()
					)  ) {
				if (isPCRTestingAllowed(person)) doPCR(builder,person,context);
			}
			
		}

		@Override
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if ( context.isReactivelyTestedToday() ) { 
				decreaseSociabilityIfCompliant(builder,person);
				return AWAIT_PCR;
			} else {
				return REACTIVE_PCR;
			}
		}
		
	},
	
	/** Patient is awaiting the result of a PCR test. If they test positive they will self isolate, if they test negative they will return to normal behaviour. */
	AWAIT_PCR {
		public State.BehaviourState nextState(ImmutablePersonState.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			if (person.isLastTestExactly(Result.PENDING)) return AWAIT_PCR;
			if (person.isLastTestExactly(Result.POSITIVE)) return SELF_ISOLATE;
			resetBehaviour(builder,person);
			return REACTIVE_PCR;
		}
	},
	
	/** Patient is self isolating. They will return to normal behaviour after the presumed infectious period, or if they are not compliant. */
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
					return REACTIVE_PCR;
				}
			}
			complianceFatigue(builder,person);
			return SELF_ISOLATE;
		}		
	};
	
	
}