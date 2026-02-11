package io.github.ai4ci.flow.mechanics;

import static io.github.ai4ci.abm.ModelNav.baseline;

import java.util.function.Predicate;

import io.github.ai4ci.abm.ImmutablePersonHistory;
import io.github.ai4ci.abm.ImmutablePersonHistory.Builder;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModelNav;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.TestResult.Type;
import io.github.ai4ci.flow.mechanics.State.BehaviourState;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

/**
 * Utility methods for state transitions, behavior modifications, and testing
 * logic within the agent-based modeling framework.
 *
 * <p>
 * This class provides reusable implementations for common state machine
 * operations including mobility adjustments, testing procedures, compliance
 * management, and controlled state transitions with branching support.
 *
 * <h2>Key Functionality Areas</h2>
 * <ul>
 * <li><b>Testing Interfaces</b>: Default interfaces for PCR testing
 * behaviors</li>
 * <li><b>Mobility Control</b>: Methods for modifying agent sociability based on
 * symptoms and compliance</li>
 * <li><b>Behavior Restoration</b>: Gradual restoration of normal behavior after
 * restrictions</li>
 * <li><b>State Branching</b>: Controlled temporary state transitions with
 * automatic return paths</li>
 * <li><b>Testing Logic</b>: Screening and diagnostic testing with eligibility
 * checks</li>
 * </ul>
 *
 * @see State
 * @see StateMachine
 * @see BehaviourState
 */
public class StateUtils {

	/**
	 * Interface for behavior states that require no testing or history updates.
	 *
	 * <p>
	 * Used for baseline behaviors or states where testing is not applicable
	 * (e.g., deceased agents, non-reactive behaviors). Provides empty
	 * implementation to satisfy interface requirements without adding testing
	 * overhead.
	 */
	public static interface DefaultNoTesting extends State.BehaviourState {
		/**
		 * Empty history update - no testing or state modifications occur.
		 */
		@Override
		default public void updateHistory(
				ImmutablePersonHistory.Builder builder, PersonState person,
				StateMachineContext context, Sampler rng
		) {}
	}

	/**
	 * Interface for behavior states that perform PCR testing when symptomatic.
	 *
	 * <p>
	 * This default implementation automatically triggers PCR testing when: -
	 * Person has been symptomatic for 2 consecutive days AND - Person is
	 * compliant with testing protocols AND - Person has not been recently tested
	 * (within disease incubation period)
	 *
	 * <p>
	 * The timing threshold (2 days) prevents premature testing while ensuring
	 * timely intervention after symptom onset.
	 */
	public static interface DoesPCRIfSymptomatic extends State.BehaviourState {
		/**
		 * Default history update that automatically seeks PCR testing when
		 * symptomatic.
		 *
		 * @param builder builder for person history updates
		 * @param person  current person state
		 * @param context state machine context
		 * @param rng     random number generator
		 */
		@Override
		default public void updateHistory(
				ImmutablePersonHistory.Builder builder, PersonState person,
				StateMachineContext context, Sampler rng
		) {
			seekPcrIfSymptomatic(builder, person, context, 2);
		}
	}

	/**
	 * Branches all people to specified behavior without filtering.
	 *
	 * Convenience method that applies to entire population without filters.
	 *
	 * @param current   current outbreak state
	 * @param behaviour target behavior state
	 */
	public static void branchPeopleTo(
			OutbreakState current, State.BehaviourState behaviour
	) {
		branchPeopleTo(current, behaviour, p -> true);
	}

//	private static double decayToZero(double p, double rate) {
//		return decayTo(p, 0, rate);
//	}

	/**
	 * Branches filtered people to specified behavior state.
	 *
	 * <p>
	 * Forces selected people to temporary behavior state while preserving
	 * current state in branch stack for later recovery. Dead people are
	 * excluded.
	 *
	 * @param current   current outbreak state
	 * @param behaviour target behavior state
	 * @param filter    predicate to select which people to modify
	 */
	public static void branchPeopleTo(
			OutbreakState current, State.BehaviourState behaviour,
			Predicate<Person> filter
	) {
		ModelNav.people(current).filter(filter)
				.filter(p -> !p.getCurrentState().isDead()).forEach(ps -> {
					ps.getStateMachine().forceTo(behaviour);
				});
	}

	/**
	 * Branches single person to specified behavior state.
	 *
	 * <p>
	 * Preserves current state in branch stack and returns the target behavior
	 * for immediate use in state transitions.
	 *
	 * @param current   person state to modify
	 * @param behaviour target behavior state
	 * @return the target behavior state for chaining
	 */
	public static State.BehaviourState branchTo(
			PersonState current, State.BehaviourState behaviour
	) {
		current.getEntity().getStateMachine().rememberCurrentState(behaviour);
		return behaviour;
	}

	/**
	 * Applies gradual compliance deterioration over time (fatigue effect).
	 *
	 * <p>
	 * Reduces compliance modifier linearly toward zero using configuration
	 * deterioration rate. Models behavioral fatigue in sustained restrictions.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void complianceFatigue(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		if (person.isCompliant()) {
			builder.setComplianceModifier(
					linearToZero(
							person.getComplianceModifier(),
							ModelNav.modelParam(person)
									.getComplianceDeteriorationRate()
					)
			);
		}
	}

	/**
	 * Restores compliance modifier toward baseline over time.
	 *
	 * <p>
	 * Increases compliance modifier linearly toward 1.0 using configuration
	 * improvement rate. Used during recovery or improved compliance scenarios.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void complianceRestoreSlowly(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		if (person.isCompliant()) {
			builder
					.setComplianceModifier(
							linearToOne(
									person.getComplianceModifier(),
									ModelNav.modelParam(person)
											.getComplianceImprovementRate()
							)
					);
		}
	}

	/**
	 * Creates a timed behavior state that automatically returns after i
	 * iterations.
	 *
	 * <p>
	 * Wraps a target state to automatically revert after specified iterations.
	 * Uses state machine branching to preserve the baseline state and recover
	 * automatically when countdown expires.
	 *
	 * @param i     number of iterations before automatic return
	 * @param state target behavior state to temporarily apply
	 * @return a wrapped state that automatically reverts after countdown
	 */
	public static State.BehaviourState countdown(
			int i, State.BehaviourState state
	) {

		return new State.BehaviourState() {
			@Override
			public String getName() { return state.getName(); }

			@Override
			public State.BehaviourState nextState(
					io.github.ai4ci.abm.ImmutablePersonState.Builder builder,
					PersonState current, StateMachineContext context, Sampler rng
			) {
				var out = state.nextState(builder, current, context, rng);
				if (out != state) { return out; }
				if (i <= 1) { return context.pullBehaviour(); }
				return countdown(i - 1, out);
			}

			@Override
			public void updateHistory(
					Builder builder, PersonState current,
					StateMachineContext context, Sampler rng
			) {
				state.updateHistory(builder, current, context, rng);
			}

		};
	}

	/**
	 * Applies exponential decay toward target value based on rate.
	 *
	 * <p>
	 * Modifies parameter p by decaying it toward target using the specified
	 * rate. The rate is converted to a probability for the decay step. Used for
	 * smooth transitions in mobility or compliance modifiers.
	 *
	 * @param p      current parameter value to modify
	 * @param target target value to decay toward (e.g., 0 for reduction, 1 for
	 *               restoration)
	 * @param rate   decay rate controlling speed of change (higher = faster)
	 * @return the modified parameter value after applying decay step
	 */
	public static double decayTo(double p, double target, double rate) {
		return p + (target - p) * Conversions.probabilityFromRate(rate);
	}

	private static double decayToOne(double p, double rate) {
		return decayTo(p, 1, rate);
	}

	/**
	 * Self-isolation behavior applied only if person is compliant.
	 *
	 * <p>
	 * Models advisory-based isolation where compliance determines adherence to
	 * recommendations rather than enforced restrictions. Used in scenarios where
	 * isolation is suggested but not mandatory.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void decreaseSociabilityIfCompliant(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		selfIsolateIfCompliant(builder, person);
	}

	/**
	 * Applies self-isolation mobility reduction if person is compliant AND
	 * symptomatic.
	 *
	 * <p>
	 * This is typically used for voluntary isolation scenarios where compliance
	 * determines whether the isolation measures are followed. Side effects:
	 * <ul>
	 * <li>Sets mobility modifier to self-isolation depth if compliant</li>
	 * <li>No action taken if person is non-compliant</li>
	 * </ul>
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void decreaseSociabilityIfSymptomatic(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		if (person.isSymptomatic()) {
			decreaseSociabilityIfCompliant(builder, person);
		}
	}

	/**
	 * Gradually decreases mobility modifier each day person is symptomatic.
	 *
	 * <p>
	 * Applies continuous mobility reduction using exponential decay toward the
	 * self-isolation depth. The decay rate is controlled by the organic mobility
	 * change parameter from model configuration.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void decreaseSociabilitySlowlyIfSymptomatic(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		if (person.isSymptomatic()) {
			builder.setMobilityModifier(
					decayTo(
							person.getMobilityModifier(),
							ModelNav.baseline(person).getSelfIsolationDepth(),
							ModelNav.modelParam(person)
									.getOrganicRateOfMobilityChange()
					)
			);
		}
	}

	/**
	 * Applies strict mobility reduction regardless of compliance status.
	 *
	 * <p>
	 * Models enforced lock-down scenarios where mobility restrictions are
	 * mandatory rather than advisory. Sets mobility to the minimum
	 * self-isolation depth defined in the baseline configuration.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void decreaseSociabilityStrictly(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		builder.setMobilityModifier(baseline(person).getSelfIsolationDepth());
	}

	/**
	 * Performs diagnostic LFT test and updates reactive testing flag.
	 *
	 * <p>
	 * Similar to PCR testing but uses LFT (Lateral Flow Test) with different
	 * sensitivity characteristics. Sets reactive testing flag.
	 *
	 * @param builder builder for person history updates
	 * @param person  current person state
	 * @param context state machine context
	 * @return the test result generated
	 */
	public static TestResult doLFT(
			ImmutablePersonHistory.Builder builder, PersonState person,
			StateMachineContext context
	) {
		var test = TestResult.screeningResultFrom(person, Type.LFT).get();
		builder.addTodaysTests(test);
		context.setReactivelyTestedToday(true);
		return test;
	}

	/**
	 * Performs diagnostic PCR test and updates reactive testing flag.
	 *
	 * <p>
	 * Used for diagnostic testing triggered by symptoms or risk exposure. Sets
	 * reactive testing flag and adds test result to person's daily history.
	 *
	 * @param builder builder for person history updates
	 * @param person  current person state
	 * @param context state machine context
	 * @return the test result generated
	 */
	public static TestResult doPCR(
			ImmutablePersonHistory.Builder builder, PersonState person,
			StateMachineContext context
	) {
		var test = TestResult.resultFrom(person, Type.PCR).get();
		builder.addTodaysTests(test);
		context.setReactivelyTestedToday(true);
		return test;
	}

	/**
	 * Creates a behavior state that gradually restores mobility over iterations.
	 *
	 * <p>
	 * Applies linear restoration of mobility and transmissibility modifiers
	 * toward baseline while maintaining the wrapped state's logic. Restoration
	 * continues only while the person is asymptomatic.
	 *
	 * @param i     number of iterations over which to restore behavior
	 * @param state target behavior state to apply during restoration
	 * @return a wrapped state that gradually restores behavior
	 */
	public static State.BehaviourState graduallyRestoreBehaviour(
			int i, State.BehaviourState state
	) {

		return new State.BehaviourState() {

			// linear return to 1 as i changes on each call.
			private double frac(int i, double p) {
				if (i < 1) { i = 1; }
				return (1 - p) / i + p;
			}

			@Override
			public String getName() { return state.getName(); }

			@Override
			public State.BehaviourState nextState(
					io.github.ai4ci.abm.ImmutablePersonState.Builder builder,
					PersonState current, StateMachineContext context, Sampler rng
			) {
				var out = state.nextState(builder, current, context, rng);
				if (i < 1) { return out; }
				if (!current.isSymptomatic()) {
					builder.setMobilityModifier(
							this.frac(i, current.getMobilityModifier())
					);
					builder.setTransmissibilityModifier(
							this.frac(i, current.getTransmissibilityModifier())
					);
				}
				return graduallyRestoreBehaviour(i - 1, out);
			}

			/**
			 * Delegates history updates to the wrapped state without modification.
			 *
			 * @param builder builder for person history updates
			 * @param current person state to evaluate
			 * @param context state machine context
			 * @param rng     random number generator
			 */
			@Override
			public void updateHistory(
					Builder builder, PersonState current,
					StateMachineContext context, Sampler rng
			) {
				state.updateHistory(builder, current, context, rng);
			}

		};
	}

	/**
	 * Checks if person has high risk of being infectious and is compliant.
	 *
	 * <p>
	 * Evaluates if person's probability of being infectious today exceeds the
	 * specified cutoff and if they are compliant with testing protocols. Used to
	 * identify individuals who should be prioritized for testing or isolation.
	 *
	 * @param person current person state to evaluate
	 * @param cutoff probability threshold for high risk of infection
	 * @return true if person is high risk and compliant, false otherwise
	 */
	public static boolean isHighRiskOfInfectionAndCompliant(
			PersonState person, double cutoff
	) {
		var pInf = person.getProbabilityInfectiousToday();
		return pInf > cutoff && person.isCompliant();
	}

	/**
	 * Checks eligibility for LFT testing based on recent test history.
	 *
	 * <p>
	 * Uses shorter restriction period (2 days) since LFTs are typically used
	 * more frequently than PCR tests.
	 *
	 * @param person current person state to evaluate
	 * @return true if LFT testing is allowed, false if tested within 2 days
	 */
	public static boolean isLFTTestingAllowed(PersonState person) {
		return !person.isRecentlyTested(Type.LFT, 2) && person.isCompliant();
	}

	/**
	 * Checks eligibility for PCR testing based on recent test history.
	 *
	 * <p>
	 * Prevents excessive testing by ensuring person hasn't been tested within
	 * the disease incubation period defined in configuration.
	 *
	 * @param person current person state to evaluate
	 * @return true if PCR testing is allowed, false if recently tested
	 */
	public static boolean isPCRTestingAllowed(PersonState person) {
		return !person.isRecentlyTested(Type.PCR) && person.isCompliant();
	}

	/**
	 * Checks if person received a positive test result today.
	 *
	 * <p>
	 * Scans today's test results for positive outcomes. Primarily used with LFT
	 * results since they provide immediate feedback.
	 *
	 * @param person current person state to evaluate
	 * @return true if positive test result exists for today
	 */
	public static boolean isPositiveTestToday(PersonState person) {
		return ModelNav.history(person).stream()
				.flatMap(h -> h.getTodaysTests().stream()).anyMatch(
						tr -> tr.resultOnDay(person.getTime()).equals(Result.POSITIVE)
				);
	}

	/**
	 * Checks if person has been symptomatic for specified days and is compliant.
	 *
	 * <p>
	 * Used to determine if symptom-driven testing or isolation should be
	 * triggered based on consecutive symptom duration and compliance status.
	 *
	 * @param person current person state to evaluate
	 * @param days   number of consecutive symptomatic days required
	 * @return true if symptomatic for specified days and compliant, false
	 *         otherwise
	 */
	public static boolean isSymptomaticAndCompliant(
			PersonState person, int days
	) {
		return person.isSymptomaticConsecutively(days) && person.isCompliant();
	}

	private static double linearTo(double p, double target, double delta) {
		if (p - delta > target) { return p - delta; }
		if (p + delta < target) { return p + delta; }
		return target;
	}

	private static double linearToOne(double p, double delta) {
		return linearTo(p, 1, delta);
	}

	private static double linearToZero(double p, double delta) {
		return linearTo(p, 0, delta);
	}

	/**
	 * Applies individual PCR screening test based on probability.
	 *
	 * @param builder              builder for person history updates
	 * @param ps                   current person state
	 * @param screeningProbability probability of screening test occurring
	 * @param rng                  random number generator for probability check
	 */
	public static void randomlyScreen(
			ImmutablePersonHistory.Builder builder, PersonState ps,
			double screeningProbability, Sampler rng
	) {
		if (rng.bern(screeningProbability)) { screenPCR(builder, ps); }
	}

	/**
	 * Applies random screening PCR tests to eligible population.
	 *
	 * <p>
	 * Selects living people with available history builders and applies
	 * screening probability check. Throws exception if called at wrong lifecycle
	 * timing.
	 *
	 * @param current current outbreak state
	 * @param rng     random number generator for probability checks
	 */
	public static void randomlyScreen(OutbreakState current, Sampler rng) {
		ModelNav.people(current).filter(p -> !p.getCurrentState().isDead())
				.forEach(ps -> {
					if (!ps.getNextHistory().isPresent()) {
						throw new RuntimeException(
								"Tried to update screening after at wrong time in lifecycle"
						);
					}
					randomlyScreen(
							ps.getNextHistory().get(), ps.getCurrentState(),
							current.getScreeningProbability(), rng
					);
				});
	}

	/**
	 * Resets mobility and transmissibility modifiers to baseline (1.0).
	 *
	 * <p>
	 * Immediately restores normal behavior without gradual transition. Used when
	 * forced behavior reset is needed (e.g., policy changes, state transitions).
	 *
	 * @param builder builder for person state modifications
	 * @param current person state to evaluate
	 */
	public static void resetBehaviour(
			ImmutablePersonState.Builder builder, PersonState current
	) {
		builder.setMobilityModifier(1.0);
		builder.setTransmissibilityModifier(1.0);
	}

	/**
	 * Unconditional gradual restoration of mobility and transmissibility to
	 * baseline.
	 *
	 * <p>
	 * Applies exponential decay restoration toward full mobility (1.0)
	 * regardless of symptomatic status. Uses the organic mobility change rate
	 * from configuration to control restoration speed.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void restoreSociabilitySlowly(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		builder
				.setMobilityModifier(
						decayToOne(
								person.getMobilityModifier(),
								ModelNav.modelParam(person)
										.getOrganicRateOfMobilityChange()
						)
				);
		builder
				.setTransmissibilityModifier(
						decayToOne(
								person.getTransmissibilityModifier(),
								ModelNav.modelParam(person)
										.getOrganicRateOfMobilityChange()
						)
				);
	}

	/**
	 * Gradually restores mobility and transmissibility toward baseline when
	 * asymptomatic.
	 *
	 * <p>
	 * Used during recovery phase to transition from restricted to normal
	 * behavior. Restoration occurs only when person is asymptomatic, preventing
	 * premature restoration while infectious.
	 *
	 * @param builder builder for person state modifications
	 * @param person  current person state to evaluate
	 */
	public static void restoreSociabilitySlowlyIfAsymptomatic(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		if (!person.isSymptomatic()) {
			restoreSociabilitySlowly(builder, person);
		}
	}

	/**
	 * Returns all people to their previous behavior states from branches.
	 *
	 * <p>
	 * Reverses branching operations by recovering previous states from the state
	 * machine branch stack. Excludes dead people.
	 *
	 * @param current current outbreak state
	 */
	public static void returnPeopleFromBranch(OutbreakState current) {
		ModelNav.people(current).forEach(ps -> {
			if (!ps.getCurrentState().isDead()) {
				ps.getStateMachine().returnFromBranch();
			}
		});
	}

	/**
	 * Performs asymptomatic screening LFT test without reactivity tracking.
	 *
	 * @param builder builder for person history updates
	 * @param person  current person state
	 * @return the screening test result
	 */
	public static TestResult screenLFT(
			ImmutablePersonHistory.Builder builder, PersonState person
	) {
		var test = TestResult.resultFrom(person, Type.LFT).get();
		return test;
	}

	/**
	 * Performs asymptomatic screening PCR test without reactive testing flag.
	 *
	 * <p>
	 * Used for routine screening rather than symptom-driven diagnostics. Does
	 * not set reactive testing flag since this is not symptom-initiated.
	 *
	 * @param builder builder for person history updates
	 * @param person  current person state
	 * @return the screening test result
	 */
	public static TestResult screenPCR(
			ImmutablePersonHistory.Builder builder, PersonState person
	) {
		var test = TestResult.screeningResultFrom(person, Type.PCR).get();
		builder.addTodaysTests(test);
		return test;
	}

	/**
	 * Performs diagnostic PCR test with 1-day symptom threshold. Seek a test if
	 * any symptoms (and compliant, and not recently tested)
	 *
	 * @param builder builder for person history updates
	 * @param person  current person state
	 * @param context state machine context
	 */
	public static void seekPcrIfSymptomatic(
			ImmutablePersonHistory.Builder builder, PersonState person,
			StateMachineContext context
	) {
		seekPcrIfSymptomatic(builder, person, context, 1);
	}

	/**
	 * The person will test themselves using PCR (immediately) if they are
	 * symptomatic consecutively for a number of days, compliant and they have
	 * not recently been tested. Recent here is defined as they have had a PCR
	 * test within the last presumed incubation period of the disease
	 *
	 * @param builder builder for person history updates
	 * @param person  current person state
	 * @param context state machine context
	 * @param days    number of consecutive symptomatic days required to trigger
	 *                testing
	 *
	 */
	public static void seekPcrIfSymptomatic(
			ImmutablePersonHistory.Builder builder, PersonState person,
			StateMachineContext context, int days
	) {
		if (isSymptomaticAndCompliant(person, days)
				&& isPCRTestingAllowed(person)) {
			doPCR(builder, person, context);
		}
	}

	/**
	 * If the person is compliant reduce their mobility to the minimum. If not
	 * compliant then if partial flag is set they will reduce their mobility to a
	 * level scaled by their level of compliance.
	 */
	private static void selfIsolateIfCompliant(
			ImmutablePersonState.Builder builder, PersonState person
	) {
		if (person.isCompliant()) {
			// non compliant people
			builder.setMobilityModifier(baseline(person).getSelfIsolationDepth());
		}
	}

	/**
	 * Retrieves previous behavior state from branch stack.
	 *
	 * <p>
	 * Used during state transitions to recover from temporary branched states.
	 *
	 * @param context state machine context containing branch stack
	 * @return the previous behavior state from branch
	 */
	public static State.BehaviourState toLastBranchPoint(
			StateMachineContext context
	) {
		return context.pullBehaviour();
	}

}
