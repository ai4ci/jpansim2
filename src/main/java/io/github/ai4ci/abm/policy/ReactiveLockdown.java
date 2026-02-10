package io.github.ai4ci.abm.policy;

import static io.github.ai4ci.flow.mechanics.StateUtils.branchPeopleTo;
import static io.github.ai4ci.flow.mechanics.StateUtils.randomlyScreen;

import io.github.ai4ci.abm.ImmutableOutbreakHistory;
import io.github.ai4ci.abm.ImmutableOutbreakState.Builder;
import io.github.ai4ci.abm.ModelNav;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.behaviour.LockdownIsolation;
import io.github.ai4ci.flow.mechanics.State;
import io.github.ai4ci.flow.mechanics.StateMachineContext;
import io.github.ai4ci.util.Sampler;

/**
 * A reactive lockdown policy implementation that triggers population-wide
 * interventions based on epidemiological thresholds.
 *
 * <p>
 * This enum implements a state machine for managing lockdown policies that
 * respond dynamically to changing outbreak conditions. The policy transitions
 * between monitoring and lockdown states based on test positivity rates and
 * configured threshold triggers.
 *
 * <h2>Policy States</h2>
 * <ul>
 * <li><b>MONITOR</b>: Passive surveillance state with routine population
 * screening</li>
 * <li><b>LOCKDOWN</b>: Active intervention state with enforced isolation
 * measures</li>
 * </ul>
 *
 * <h2>Trigger Mechanism</h2>
 * <p>
 * Policy transitions are triggered by comparing current outbreak metrics
 * against configured thresholds ({@link Trigger}) with statistical confidence:
 * <ul>
 * <li><b>Lockdown Start</b>: Triggered when test positivity confidently exceeds
 * the start threshold (95% confidence)</li>
 * <li><b>Lockdown Release</b>: Triggered when test positivity confidently falls
 * below the release threshold (95% confidence)</li>
 * </ul>
 *
 * <h2>Behavioral Impact</h2>
 * <p>
 * When lockdown is triggered, the policy forces population-wide behavior
 * changes:
 * <ul>
 * <li><b>Entering Lockdown</b>: Branches all people to
 * {@link LockdownIsolation#ISOLATE} behavior state</li>
 * <li><b>Exiting Lockdown</b>: Branches all people to
 * {@link LockdownIsolation#RELEASE} behavior state</li>
 * </ul>
 *
 * <h2>Screening Strategy</h2>
 * <p>
 * Both policy states perform routine population screening to monitor outbreak
 * progression and inform policy decisions.
 *
 * @see LockdownIsolation
 * @see io.github.ai4ci.flow.mechanics.StateUtils#branchPeopleTo
 * @see io.github.ai4ci.flow.mechanics.StateUtils#randomlyScreen
 * @see PolicyModel
 */
public enum ReactiveLockdown implements PolicyModel {

	/**
	 * Monitoring state that performs routine population screening and evaluates
	 * whether to trigger lockdown based on test positivity thresholds.
	 *
	 * <p>
	 * In this state:
	 * <ul>
	 * <li>Routine screening tests are conducted across the population</li>
	 * <li>Test positivity rates are monitored continuously</li>
	 * <li>Lockdown is triggered when positivity exceeds start threshold with 95%
	 * confidence</li>
	 * </ul>
	 *
	 * <p>
	 * The trigger comparison uses statistical confidence intervals to prevent
	 * premature lockdown activation due to random fluctuations in test results.
	 */
	MONITOR {

		@Override
		public void updateHistory(ImmutableOutbreakHistory.Builder builder, OutbreakState current,
				StateMachineContext context, Sampler rng) {
			randomlyScreen(current, rng);
		}

		@Override
		public State.PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getLockdownTrigger()
					.confidentlyGreaterThan(ModelNav.modelParam(current).getLockdownStartTrigger(), 0.95)) {
				branchPeopleTo(current, LockdownIsolation.ISOLATE);
				return LOCKDOWN;
			}
			return MONITOR;
		}
	},

	/**
	 * Active lockdown state that enforces population-wide isolation measures and
	 * evaluates whether to release based on improving epidemiological conditions.
	 *
	 * <p>
	 * In this state:
	 * <ul>
	 * <li>Routine screening continues to monitor outbreak progression</li>
	 * <li>Population-wide isolation behavior is enforced</li>
	 * <li>Lockdown is released when positivity falls below release threshold with
	 * 95% confidence</li>
	 * </ul>
	 *
	 * <p>
	 * The release condition ensures lockdowns are maintained until there is
	 * statistical confidence that the outbreak has sufficiently subsided.
	 */
	LOCKDOWN {

		@Override
		public void updateHistory(ImmutableOutbreakHistory.Builder builder, OutbreakState current,
				StateMachineContext context, Sampler rng) {
			randomlyScreen(current, rng);
		}

		@Override
		public State.PolicyState nextState(Builder builder, OutbreakState current, StateMachineContext context,
				Sampler rng) {
			if (current.getLockdownTrigger()
					.confidentlyLessThan(ModelNav.modelParam(current).getLockdownReleaseTrigger(), 0.95)) {
				branchPeopleTo(current, LockdownIsolation.RELEASE);
				return MONITOR;
			}
			return LOCKDOWN;
		}
	};

}