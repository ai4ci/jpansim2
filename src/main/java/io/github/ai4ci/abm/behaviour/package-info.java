/**
 * Behaviour models for the agent-based simulator.
 *
 * <p>The classes in this package implement per-agent behaviour state machines
 * used during an update cycle. Behaviour models are small, focused enums or
 * classes that implement {@link io.github.ai4ci.abm.behaviour.BehaviourModel}
 * (which itself extends {@link io.github.ai4ci.flow.mechanics.State.BehaviourState}).
 * They encapsulate the decision logic executed for each person during the
 * model's <em>history update</em> and <em>state update</em> phases.
 *
 * <h2>Runtime context</h2>
 * <p>Behaviour state machines are driven from the core mechanics in
 * {@code io.github.ai4ci.abm.mechanics}. Each update cycle proceeds roughly in
 * the following order:
 * <ol>
 *   <li>history update — behaviour models may record events (for example
 *       seeking a test or recording contacts); implemented via
 *       {@code updateHistory(...)} in behaviour enums;</li>
 *   <li>state update decision — the behaviour state's {@code nextState(...)}
 *       method is invoked to decide the next behaviour state for the person;
 *       this is where transitions, timed triggers and checks (e.g. testing
 *       results or compliance) are implemented;</li>
 *   <li>state commit — the chosen {@code BehaviourState} is attached to the
 *       {@code Person}'s runtime state and used by the mechanics for the next
 *       timestep.</li>
 * </ol>
 *
 * <h2>Helper utilities</h2>
 * <p>Behaviour implementations should use the helpers in
 * {@link io.github.ai4ci.flow.mechanics.StateUtils} for common operations such
 * as branching back to a previously saved behaviour (see
 * {@link io.github.ai4ci.flow.mechanics.StateUtils#toLastBranchPoint}),
 * applying compliance/mobility modifiers, and scheduling tests
 * (e.g. {@code doPCR}, {@code doLFT}, {@code seekPcrIfSymptomatic}). These
 * helpers keep behaviour enums concise and focused on control flow rather than
 * low-level state manipulation.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>Behaviour models are intentionally small and declarative: each enum
 *       value represents a distinct behaviour state and implements the
 *       transition logic in {@code nextState(...)}.</li>
 *   <li>Where a behaviour needs to temporarily suspend the agent's previous
 *       behaviour (for example during lockdown) the mechanics support branching
 *       to the behaviour and later returning via the branch-point helper
 *       methods.</li>
 *   <li>Testing and isolation behaviours often implement both
 *       {@code updateHistory(...)} (to schedule tests) and
 *       {@code nextState(...)} (to react to test results). See
 *       {@link io.github.ai4ci.abm.behaviour.SmartAgentLFTTesting} and
 *       {@link io.github.ai4ci.abm.behaviour.SmartAgentTesting} for examples.</li>
 * </ul>
 *
 * <h2>Where to look next</h2>
 * <ul>
 *   <li>{@link io.github.ai4ci.flow.mechanics.State} — behaviour/state interfaces
 *       and shared state types used by the mechanics;</li>
 *   <li>{@link io.github.ai4ci.flow.mechanics.StateUtils} — helpers for common
 *       state-machine operations (branching, compliance adjustments, testing);
 *   </li>
 *   <li>Enum implementations in this package (for example
 *       {@link io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate},
 *       {@link io.github.ai4ci.abm.behaviour.SmartAgentTesting},
 *       {@link io.github.ai4ci.abm.behaviour.LockdownIsolation}) which show
 *       typical usage patterns.</li>
 * </ul>
 */
package io.github.ai4ci.abm.behaviour;
