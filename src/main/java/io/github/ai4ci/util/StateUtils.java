package io.github.ai4ci.util;

import static io.github.ai4ci.util.ModelNav.baseline;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonHistory.Builder;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.StateMachine;
import io.github.ai4ci.abm.StateMachine.BehaviourState;
import io.github.ai4ci.abm.StateMachineContext;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.TestResult.Type;

public class StateUtils {

	/**
	 * Flags a behaviour model as seeking (and performing) a PCR test if the 
	 * person has been symptomatic for 2 days and is compliant and has not had a test in a set 
	 * number of days (regardless of test outcome). 
	 * 
	 * The outcome of testing can be 
	 */
	public static interface DoesPCRIfSymptomatic extends StateMachine.BehaviourState {
		default public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			seekPcrIfSymptomatic(builder, person, 2);
		}
	}
	
	public static interface DefaultNoTesting extends StateMachine.BehaviourState {
		default public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {}
	}
	

	
	private static double decayToOne(double p, double rate) {
		return decayTo(p, 1, rate);
	}
	
	private static double decayToZero(double p, double rate) {
		return decayTo(p, 0, rate);
	}
	
	private static double decayTo(double p, double target, double rate) {
		return p + (target-p) * Conversions.probabilityFromRate(rate);
	}
	
	private static double linearToOne(double p, double delta) {
		return linearTo(p,1,delta);
	}
	
	private static double linearToZero(double p, double delta) {
		return linearTo(p,0,delta);
	}
	
	private static double linearTo(double p, double target, double delta) {
		if (p-delta > target) return p-delta;
		if (p+delta < target) return p+delta;
		return target;	
	}
	
	public static void decreaseSociabilityIfSymptomatic(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isSymptomatic()) {
			decreaseSociabilityIfCompliant(builder,person);
		}
	}
	
	public static void decreaseSociabilityIfCompliant(ImmutablePersonState.Builder builder, 
			PersonState person) {
		selfIsolate(builder,person,false);
	}
	
	
	
	
	public static void decreaseSociabilityStrictly(ImmutablePersonState.Builder builder, 
			PersonState person) {
		selfIsolate(builder,person,true);
	}
	
	private static void selfIsolate(ImmutablePersonState.Builder builder, 
			PersonState person, boolean partialNonCompliance) {
		if (person.isCompliant()) {
			builder.setMobilityModifier(
				baseline(person).getSelfIsolationDepth()
			);
		} else if (partialNonCompliance) {
			builder.setMobilityModifier(
					1-Conversions.scaleProbability(
						1-baseline(person).getSelfIsolationDepth(),
						person.getAdjustedCompliance()
					)
			);
		}
	}
	
	public static void decreaseSociabilitySlowlyIfSymptomatic(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isSymptomatic()) {
			builder.setMobilityModifier(
					decayTo(
						person.getMobilityModifier(), 
						ModelNav.baseline(person).getSelfIsolationDepth(),
						1.0/4 ));
		}
	}
	
	public static void restoreSociabilitySlowlyIfAsymptomatic(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (!person.isSymptomatic()) {
			restoreSociabilitySlowly(builder,person);
		}
	}
	
	public static void restoreSociabilitySlowly(ImmutablePersonState.Builder builder, 
			PersonState person) {
		builder.setMobilityModifier(
			decayToOne(person.getMobilityModifier(), 1.0/4 ));
		builder.setTransmissibilityModifier(
			decayToOne(person.getTransmissibilityModifier(), 1.0/4 ));
	}
	
	/**
	 * Restore mobility and transmissibility to baseline.
	 * @param builder
	 * @param current
	 */
	public static void resetBehaviour(ImmutablePersonState.Builder builder,
			PersonState current) {
		builder.setMobilityModifier( 1.0 );
		builder.setTransmissibilityModifier( 1.0 );
	}
	
	
	public static void seekPcrIfSymptomatic(ImmutablePersonHistory.Builder builder, 
			PersonState person) {
		seekPcrIfSymptomatic(builder, person, 1);
	}
	
	public static void doPCR(ImmutablePersonHistory.Builder builder, 
			PersonState person) {
		TestResult test = TestResult.resultFrom(person, Type.PCR).get();
		builder.addTodaysTests(test);
	}
	
	public static void seekPcrIfSymptomatic(ImmutablePersonHistory.Builder builder, 
			PersonState person, int days) {
		if (isSymptomatic(person, days) && isTestingAllowed(person) ) {
			doPCR(builder,person);
		}
	}
	
	public static boolean isSymptomatic(PersonState person, int days) {
		return person.isSymptomaticConsecutively(days) && person.isCompliant();
	}
	
	public static boolean isHighRiskOfInfection(PersonState person, double cutoff) {
		return person.getProbabilityInfectiousToday() > cutoff && person.isCompliant(); 
	}
	
	public static boolean isTestingAllowed(PersonState person) {
		return !person.isRecentlyTested(Type.PCR) && person.isCompliant();
	}
	
	/**
	 * Only for use in nextState methods. Looks to see if a test was conducted
	 * @param person
	 * @return
	 */
	public static boolean isTestedToday(PersonState person) {
		return ModelNav.history(person)
				.map(h -> !h.getTodaysTests().isEmpty())
				.orElse(Boolean.FALSE);
	}
	
	/**
	 * Linear 2% per step reduction in compliance until reaches zero
	 * @param builder
	 * @param person
	 */
	public static void complianceFatigue(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isCompliant()) {
			builder.setComplianceModifier(
				linearToZero( person.getComplianceModifier(), 0.02 )
			);
		}
	}
	
	/**
	 * Linear 1% per step improvement in compliance until 100%
	 * @param builder
	 * @param person
	 */
	public static void complianceRestoreSlowly(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isCompliant()) {
			builder.setComplianceModifier(
				linearToOne( person.getComplianceModifier(), 0.01 )
			);
		}
	}
	
	/**
	 * places machine into given behaviour for up to i iterations, then reverts to
	 * the last pushed behaviour. If the behaviour changes before the countdown
	 * is up then the machine will proceed with new behaviour.
	 * @param i
	 * @param state
	 * @return
	 */
	public static BehaviourState countdown(int i, BehaviourState state) {
		
		return new BehaviourState() {
			@Override
			public void updateHistory(Builder builder, PersonState current, StateMachineContext context, Sampler rng) {
				state.updateHistory(builder, current, context, rng);
			}

			@Override
			public BehaviourState nextState(io.github.ai4ci.abm.ImmutablePersonState.Builder builder,
					PersonState current, StateMachineContext context, Sampler rng) {
				BehaviourState out = state.nextState(builder, current, context, rng);
				if (out != state) return out;
				if (i<=1) return context.pullBehaviour();
				return countdown(i-1,out);
			}

			@Override
			public String getName() {
				return state.getName();
			}
			
		};
	}
	

	
	
	
	/**
	 * A state that reverts mobility and transmissibility to 
	 * baseline while the machine continues in normal operation, over a set time 
	 * period. 
	 * @param i
	 * @param state
	 * @return
	 */
	public static BehaviourState graduallyRestoreBehaviour(int i, BehaviourState state) {
		
		return new BehaviourState() {
			
			// linear return to 1 as i changes on each call.
			private static double frac(int i, double p) {
				if (i < 1) i=1;
				return (1-p)/i+p;
			}
			
			@Override
			public void updateHistory(Builder builder, PersonState current, StateMachineContext context, Sampler rng) {
				state.updateHistory(builder, current, context, rng);
			}
			
			

			@Override
			public BehaviourState nextState(io.github.ai4ci.abm.ImmutablePersonState.Builder builder,
					PersonState current, StateMachineContext context, Sampler rng) {
				BehaviourState out = state.nextState(builder, current, context, rng);
				if (i < 1) return out;
				if (!current.isSymptomatic()) {
					builder.setMobilityModifier( frac(i, current.getMobilityModifier()));
					builder.setTransmissibilityModifier( frac(i, current.getTransmissibilityModifier()));
				}
				return graduallyRestoreBehaviour(i-1,out);
			}

			@Override
			public String getName() {
				return state.getName();
			}
			
		};
	}
	
	/**
	 * Force all people in a model to branch to a set behaviour, the current
	 * state is pushed to allow people to return to the current behaviour 
	 * (using a returnFromBranch call).
	 * @param current
	 * @param behaviour
	 */
	public static void branchTo(OutbreakState current, BehaviourState behaviour) {
		ModelNav.people(current).forEach( ps ->
			ps.getStateMachine().forceTo(behaviour)
	);
	}
	
	/**
	 * Force one person to branch to a set behaviour, the current
	 * state is pushed to allow them to return to the current behaviour 
	 * (using a returnFromBranch call).
	 * @param current
	 * @param behaviour
	 */
	public static BehaviourState branchTo(PersonState current, BehaviourState behaviour) {
		current.getEntity().getStateMachine().branchToState(behaviour);
		return behaviour;
	}
	
	/** 
	 * return from a branched behaviour model.
	 * @param context
	 * @return
	 */
	public static BehaviourState returnFromBranch(StateMachineContext context) {
		return context.pullBehaviour();
	}
	
}
