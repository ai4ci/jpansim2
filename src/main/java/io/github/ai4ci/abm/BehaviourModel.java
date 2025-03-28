package io.github.ai4ci.abm;

import static io.github.ai4ci.util.ModelNav.modelState;
import static io.github.ai4ci.util.StateUtils.*;

import io.github.ai4ci.abm.ImmutablePersonHistory.Builder;
import io.github.ai4ci.abm.StateMachine.BehaviourState;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.StateUtils.DefaultNoTesting;
import io.github.ai4ci.util.StateUtils.DoesPCRIfSymptomatic;

/**
 * Called during an update cycle before any changes have been made
 * This means any references to state refers to current state, but 
 * any references to history refer to the previous state.
 */
public interface BehaviourModel extends StateMachine.BehaviourState {
	
	
	
	public static enum NonCompliant implements BehaviourModel, DefaultNoTesting {
		
		DEFAULT {
			@Override
			public BehaviourState nextState(ImmutablePersonState.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				decreaseSociabilitySlowlyIfSymptomatic(builder,person);
				restoreSociabilitySlowlyIfAsymptomatic(builder,person);
				complianceRestoreSlowly(builder,person);
				// N.B.: this could lead to frequent flip-flopping from 
				// compliance to non compliance:
				if (person.isCompliant()) return returnFromBranch(context);
				return NonCompliant.DEFAULT;
			}
		};
		
		public String getName() {return NonCompliant.class.getSimpleName()+"."+this.name();}
		
	}

	public static enum ReactiveTestAndIsolate implements BehaviourModel, DefaultNoTesting {
	
		/**
		 * Patient will probably test if they have symptoms, then wait for the
		 * result.
		 */
		REACTIVE_PCR {
	
			@Override
			public void updateHistory(ImmutablePersonHistory.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				seekPcrIfSymptomatic(builder, person, 1);
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
					if (rng.periodTrigger(modelState(person).getPresumedInfectiousPeriod())) {
						resetBehaviour(builder,person);
						return REACTIVE_PCR;
					}
				}
				complianceFatigue(builder,person);
				return SELF_ISOLATE;
			}		
		};
		
		public String getName() {return ReactiveTestAndIsolate.class.getSimpleName()+"."+this.name();}
	}

	public static enum LockdownIsolation implements BehaviourModel, DoesPCRIfSymptomatic {
		
		ISOLATE {
			public BehaviourState nextState(ImmutablePersonState.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				decreaseSociabilityStrictly(builder,person);
				return WAIT;
			}
		},
		
		WAIT {
			public BehaviourState nextState(ImmutablePersonState.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				if (!person.isCompliant()) {
					return branchTo(person, NonCompliant.DEFAULT);
				} else {	
					complianceFatigue(builder,person);
					return WAIT;
				}
			}
		},
		
		RELEASE {
			public BehaviourState nextState(ImmutablePersonState.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				builder.setMobilityModifier(1.25);
				return graduallyRestoreBehaviour(10,RELAX);
			}
		},
		
		RELAX {
			public BehaviourState nextState(ImmutablePersonState.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				if (person.isSymptomatic()) return returnFromBranch(context);
				complianceRestoreSlowly(builder,person);
				return RELAX;
			}
		}
		
		;
		
		public String getName() {return LockdownIsolation.class.getSimpleName()+"."+this.name();}
		
	}
	
	public static enum SmartAgentTesting implements BehaviourModel, DefaultNoTesting {
		
		/**
		 * Patient will probably test if they have symptoms, then wait for the
		 * result.
		 */
		REACTIVE_PCR {
	
			@Override
			public void updateHistory(ImmutablePersonHistory.Builder builder, 
					PersonState person, StateMachineContext context, Sampler rng) {
				
				if (isSymptomatic(person, 2) || isHighRiskOfInfection(person, 0.5)  ) {
					if (isTestingAllowed(person)) doPCR(builder,person);
				}
				
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
					if (rng.periodTrigger(modelState(person).getPresumedInfectiousPeriod())) {
						resetBehaviour(builder,person);
						return REACTIVE_PCR;
					}
				}
				complianceFatigue(builder,person);
				return SELF_ISOLATE;
			}		
		};
		
		public String getName() {return SmartAgentTesting.class.getSimpleName()+"."+this.name();}
	}
	
//	public static enum SmartAgent implements BehaviourModel, DefaultNoTesting {
//		
//		DEFAULT {
//
//			@Override
//			public void updateHistory(Builder builder, PersonState current, StateMachineContext context, Sampler rng) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public BehaviourState nextState(ImmutablePersonState.Builder builder,
//					PersonState current, StateMachineContext context, Sampler rng) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//			
//		};
//		
//		public String getName() {return this.getClass().getSimpleName()+"."+this.name();}
//		
//	}
	
}
