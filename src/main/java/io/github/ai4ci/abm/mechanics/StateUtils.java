package io.github.ai4ci.abm.mechanics;

import static io.github.ai4ci.util.ModelNav.baseline;

import java.util.function.Predicate;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonHistory.Builder;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.TestResult.Type;
import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ModelNav;
import io.github.ai4ci.util.Sampler;

public class StateUtils {

	/**
	 * Flags a behaviour model as seeking (and performing) a PCR test if the 
	 * person has been symptomatic for 2 days in a row and is compliant and has 
	 * not had a test in a set number of days (regardless of test outcome).  
	 */
	public static interface DoesPCRIfSymptomatic extends StateMachine.BehaviourState {
		default public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {
			seekPcrIfSymptomatic(builder, person, context, 2);
		}
	}
	
	/**
	 * Flags a behaviour model for not needing any testing or history updates.
	 */
	public static interface DefaultNoTesting extends StateMachine.BehaviourState {
		default public void updateHistory(ImmutablePersonHistory.Builder builder, 
				PersonState person, StateMachineContext context, Sampler rng) {}
	}
	
	private static double decayToOne(double p, double rate) {
		return decayTo(p, 1, rate);
	}
	
//	private static double decayToZero(double p, double rate) {
//		return decayTo(p, 0, rate);
//	}
	
	protected static double decayTo(double p, double target, double rate) {
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
	
	/**
	 * Self isolate in response to symptoms but only if person is compliant.
	 */
	public static void decreaseSociabilityIfSymptomatic(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isSymptomatic()) {
			decreaseSociabilityIfCompliant(builder,person);
		}
	}
	
	/**
	 * Self isolate but only if compliant. More for an advisory or self imposed
	 * reduction in response to symptoms. 
	 * lockdown.
	 */
	public static void decreaseSociabilityIfCompliant(ImmutablePersonState.Builder builder, 
			PersonState person) {
		selfIsolateIfCompliant(builder,person);
	}
	
	
	
	/**
	 * Self isolate. If not compliant this will still have an effect. This is 
	 * the option in enforced lock-down type scenarios. 
	 */
	public static void decreaseSociabilityStrictly(ImmutablePersonState.Builder builder, 
			PersonState person) {
		builder.setMobilityModifier(
			baseline(person).getSelfIsolationDepth()
		);
	}
	
	/**
	 * If the person is compliant reduce their mobility to the minimum. If 
	 * not compliant then if partial flag is set they will reduce their mobility 
	 * to a level scaled by their level of compliance.
	 */
	private static void selfIsolateIfCompliant(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isCompliant()) {
			// non compliant people
			builder.setMobilityModifier(
				baseline(person).getSelfIsolationDepth()
			);
		} 
	}
	
	/**
	 * Decrease mobility by a set proportion for every day that the
	 * person is symptomatic, towards the minimum value of their self isolation 
	 */
	public static void decreaseSociabilitySlowlyIfSymptomatic(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isSymptomatic()) {
			builder.setMobilityModifier(
					decayTo(
						person.getMobilityModifier(), 
						ModelNav.baseline(person).getSelfIsolationDepth(),
						ModelNav.modelParam(person).getOrganicRateOfMobilityChange() ));
		}
	}
	
	/**
	 * Restore mobility and transmissibility by a set proportion to their default
	 * levels while a patient remains asymptomatic 
	 */
	public static void restoreSociabilitySlowlyIfAsymptomatic(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (!person.isSymptomatic()) {
			restoreSociabilitySlowly(builder,person);
		}
	}
	
	/**
	 * Restore mobility and transmissibility by a set proportion to their default
	 * levels. 
	 */
	public static void restoreSociabilitySlowly(ImmutablePersonState.Builder builder, 
			PersonState person) {
		builder.setMobilityModifier(
			decayToOne(
					person.getMobilityModifier(), 
					ModelNav.modelParam(person).getOrganicRateOfMobilityChange() 
			));
		builder.setTransmissibilityModifier(
			decayToOne(
					person.getTransmissibilityModifier(), 
					ModelNav.modelParam(person).getOrganicRateOfMobilityChange()
			));
	}
	
	/**
	 * Restore mobility and transmissibility to baseline.
	 */
	public static void resetBehaviour(ImmutablePersonState.Builder builder,
			PersonState current) {
		builder.setMobilityModifier( 1.0 );
		builder.setTransmissibilityModifier( 1.0 );
	}
	
	/**
	 * Seek a test if any symptoms (and compliant, and not recently tested)
	 */
	public static void seekPcrIfSymptomatic(ImmutablePersonHistory.Builder builder, 
			PersonState person, StateMachineContext context) {
		seekPcrIfSymptomatic(builder, person, context, 1);
	}
	
	/**
	 * do a PCR test.
	 */
	public static TestResult doPCR(ImmutablePersonHistory.Builder builder, 
			PersonState person, StateMachineContext context) {
		TestResult test = TestResult.resultFrom(person, Type.PCR).get();
		builder.addTodaysTests(test);
		context.setReactivelyTestedToday(true);
		return test;
	}
	
	public static TestResult screenPCR(ImmutablePersonHistory.Builder builder, 
			PersonState person) {
		TestResult test = TestResult.screeningResultFrom(person, Type.PCR).get();
		builder.addTodaysTests(test);
		return test;
	}
	
	/**
	 * do a LFT test.
	 */
	public static TestResult doLFT(ImmutablePersonHistory.Builder builder, 
			PersonState person, StateMachineContext context) {
		TestResult test = TestResult.screeningResultFrom(person, Type.LFT).get();
		builder.addTodaysTests(test);
		context.setReactivelyTestedToday(true);
		return test;
	}
	
	public static TestResult screenLFT(ImmutablePersonHistory.Builder builder, 
			PersonState person) {
		TestResult test = TestResult.resultFrom(person, Type.LFT).get();
		return test;
	}
	
	/**
	 * The person will test themselves using PCR (immediately) if they are 
	 * symptomatic consecutively for a number of days, compliant and they have 
	 * not recently been tested. Recent here is defined
	 * as they have had a PCR test within the last presumed incubation period
	 * of the disease
	 */
	public static void seekPcrIfSymptomatic(ImmutablePersonHistory.Builder builder, 
			PersonState person, StateMachineContext context, int days) {
		if (isSymptomaticAndCompliant(person, days) && isPCRTestingAllowed(person) ) {
			doPCR(builder, person, context);
		}
	}
	
	public static boolean isSymptomaticAndCompliant(PersonState person, int days) {
		return person.isSymptomaticConsecutively(days) && person.isCompliant();
	}
	
	public static boolean isHighRiskOfInfectionAndCompliant(PersonState person, double cutoff) {
		double pInf = person.getProbabilityInfectiousToday();
		return  pInf > cutoff && person.isCompliant();
	}
	
	/**
	 * The person is compliant and they have not had a recent test (within the
	 * presumed incubation period of disease). If there is some question as to
	 * whether they need a test and they have recently has one this will stop
	 * them having another one.
	 */
	public static boolean isPCRTestingAllowed(PersonState person) {
		return !person.isRecentlyTested(Type.PCR) && person.isCompliant();
	}
	
	/**
	 * The person is compliant and they have not had a recent LFT test (within 
	 * the last 2 days. If there is some question as to
	 * whether they need a test and they have recently has one this will stop
	 * them having another one.
	 */
	public static boolean isLFTTestingAllowed(PersonState person) {
		return !person.isRecentlyTested(Type.LFT, 2) && person.isCompliant();
	}
	
	/**
	 * Only for use in nextState behaviour methods. Looks to see if a test was 
	 * conducted on this day and the result is positive. This will generally
	 * only be the case for LFTs 
	 */
	public static boolean isPositiveTestToday(PersonState person) {
		return ModelNav.history(person).stream()
				.flatMap(h -> h.getTodaysTests().stream())
				.anyMatch(tr -> tr.resultOnDay(person.getTime()).equals(Result.POSITIVE));
	}
	
	/**
	 * Linear step reduction in compliance until reaches zero. The amount is 
	 * configured in the {@link ExecutionConfiguration}.
	 */
	public static void complianceFatigue(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isCompliant()) {
			builder.setComplianceModifier(
				linearToZero( person.getComplianceModifier(), 
					ModelNav.modelParam(person).getComplianceDeteriorationRate())
			);
		}
	}
	
	/**
	 * Linear step improvement in compliance until reaches 1. The amount is 
	 * configured in the {@link ExecutionConfiguration}.
	 */
	public static void complianceRestoreSlowly(ImmutablePersonState.Builder builder, 
			PersonState person) {
		if (person.isCompliant()) {
			builder.setComplianceModifier(
				linearToOne( person.getComplianceModifier(), 
					ModelNav.modelParam(person).getComplianceImprovementRate()
				)
			);
		}
	}
	
	/**
	 * places machine into given behaviour for up to i iterations, then reverts to
	 * the last pushed behaviour. If the behaviour changes before the countdown
	 * is up then the machine will proceed with new behaviour.
	 * @param i the number of iterations
	 * @param state the state to temporarily enter
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
	 * @param i time over which to restore behaviour
	 * @param state next state to enter into immediately.
	 */
	public static BehaviourState graduallyRestoreBehaviour(int i, BehaviourState state) {
		
		return new BehaviourState() {
			
			// linear return to 1 as i changes on each call.
			private double frac(int i, double p) {
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
	
	public static void randomlyScreen(OutbreakState current, Sampler rng) {
		ModelNav.people(current)
			.filter(p -> !p.getCurrentState().isDead())
			.forEach( ps -> {
				if (ps.getNextHistory().isPresent()) {
					randomlyScreen(
						ps.getNextHistory().get(),
						ps.getCurrentState(), current.getScreeningProbability(), rng);
				} else {
					throw new RuntimeException("Tried to update screening after at wrong time in lifecycle");
				}
			});
	}
	
	public static void randomlyScreen(ImmutablePersonHistory.Builder builder, PersonState ps, double screeningProbability, Sampler rng) {
		if (rng.bern(screeningProbability)) screenPCR(builder,ps);
	}

	/**
	 * {@link #branchPeopleTo(OutbreakState, BehaviourState, Predicate)}
	 */
	public static void branchPeopleTo(OutbreakState current, BehaviourState behaviour) {
		branchPeopleTo(current, behaviour, p -> true);
	}
	
	/**
	 * Force all people in a model to branch to a specified behaviour, the current
	 * state is pushed to allow people to return to the current behaviour 
	 * (using a returnFromBranch call).
	 * @param current the current model state.
	 * @param behaviour the target behaviour.
	 * @param filter ally update only to certain people (e.g. by age for changing behaviour)
	 */
	public static void branchPeopleTo(OutbreakState current, BehaviourState behaviour, Predicate<Person> filter) {
		ModelNav.people(current)
			.filter(filter)
			.filter(p -> !p.getCurrentState().isDead())
			.forEach( ps -> {
				ps.getStateMachine().forceTo(behaviour);
			}
	);
	}
	
	/**
	 * Force all people in a model to branch to a set behaviour, the current
	 * state is pushed to allow people to return to the current behaviour 
	 * (using a returnFromBranch call).
	 */
	public static void returnPeopleFromBranch(OutbreakState current) {
		ModelNav.people(current).forEach( ps -> {
			if (!ps.getCurrentState().isDead()) { //.getStateMachine().getState().equals(BehaviourModel.NonCompliant.DEAD))
				ps.getStateMachine().returnFromBranch();
			}
		}
	);
	}
	
	/**
	 * Force one person to branch to a set behaviour, the current
	 * state is pushed to allow them to return to the current behaviour 
	 * (using a returnFromBranch call).
	 */
	public static BehaviourState branchTo(PersonState current, BehaviourState behaviour) {
		current.getEntity().getStateMachine().rememberCurrentState(behaviour);
		return behaviour;
	}
	
	/** 
	 * return from a branched behaviour model.
	 */
	public static BehaviourState toLastBranchPoint(StateMachineContext context) {
		return context.pullBehaviour();
	}
	
}
