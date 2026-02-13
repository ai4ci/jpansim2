/**
 * Policy models for outbreak-level interventions and control strategies.
 *
 * <p>
 * This package contains implementations of policy-level state machines that
 * model various outbreak control strategies, including monitoring, lockdowns,
 * and reactive interventions based on epidemiological indicators.
 *
 * <h2>Core Components</h2>
 * <ul>
 * <li>{@link io.github.ai4ci.abm.policy.PolicyModel} - Base interface for
 * policy state implementations that extend the core State.PolicyState
 * functionality</li>
 * <li>{@link io.github.ai4ci.abm.policy.NoControl} - Policy state representing
 * no central control, serving as the baseline/default policy</li>
 * <li>{@link io.github.ai4ci.abm.policy.ReactiveLockdown} - Policy states for
 * reactive lockdown strategies triggered by epidemiological thresholds</li>
 * <li>{@link io.github.ai4ci.abm.policy.Trigger} - Interface for defining
 * policy activation triggers based on outbreak state metrics</li>
 * </ul>
 *
 * <h2>Policy State Machine Patterns</h2>
 * <p>
 * Policy models follow the state machine pattern defined in the mechanics
 * package:
 * <ul>
 * <li>Policy states implement {@code State.PolicyState} interface</li>
 * <li>State transitions are managed through {@code nextState()} method
 * implementations</li>
 * <li>History updates can perform policy-specific actions like population
 * screening</li>
 * <li>Policies can force behavior state transitions across the population</li>
 * </ul>
 *
 * <h2>Reactive Lockdown Implementation</h2>
 * <p>
 * The {@code ReactiveLockdown} enum provides a complete implementation of a
 * threshold-based policy response system:
 * <ul>
 * <li><b>MONITOR</b>: Passive monitoring with routine screening</li>
 * <li><b>LOCKDOWN</b>: Active intervention with population-wide behavior
 * restrictions</li>
 * <li><b>TRANSITION</b>: Intermediate state for policy change management</li>
 * </ul>
 *
 * <h2>Trigger Mechanisms</h2>
 * <p>
 * Policies can be activated based on various epidemiological triggers:
 * <ul>
 * <li>Test positivity rates</li>
 * <li>Screening test positivity</li>
 * <li>Case incidence thresholds</li>
 * <li>Other outbreak metrics</li>
 * </ul>
 *
 * <h2>Integration with Outbreak Modeling</h2>
 * <p>
 * Policy models interact with the broader outbreak simulation framework by:
 * <ul>
 * <li>Modifying outbreak state through builder patterns</li>
 * <li>Influencing individual agent behavior via state machine transitions</li>
 * <li>Responding to changing epidemiological conditions</li>
 * <li>Providing decision points for public health intervention simulations</li>
 * </ul>
 *
 * @see io.github.ai4ci.flow.mechanics.State.PolicyState
 * @see io.github.ai4ci.abm.OutbreakState
 * @see io.github.ai4ci.flow.mechanics.StateMachine
 */
package io.github.ai4ci.abm.policy;