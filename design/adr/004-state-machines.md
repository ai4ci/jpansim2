# ADR-4: Behaviour and Policy State-Machine Architecture

**Status**: Accepted

**Date**: 2026-05-02

## Context

Individual agents and the outbreak as a whole must track and transition between state-based behaviours: agents may seek tests, self-isolate, comply with or ignore guidance, and respond to symptoms; the outbreak-level system may trigger lockdowns or other non-pharmaceutical interventions. State transitions are driven by evidence (symptoms, test results, risk perception) and external triggers (policy decisions).

## Decision

Both behaviour and policy are modelled as enum-based state machines implementing the generic `State<BUILDER, HISTORY, STATE, X>` interface:

1. **Base interface**: `State.BehaviourState` (per-person) and `State.PolicyState` (per-outbreak), each with two abstract methods:
   - `nextState()` — determines state transitions for the next day
   - `updateHistory()` — mutates the history builder for records (testing, contacts, etc.)

2. **State machine orchestration**: `StateMachine` class manages two-phase updates:
   - `performHistoryUpdate()` called first (records testing, contacts, exposures)
   - `performStateUpdate()` called second (determines next state via `nextState()`)

3. **Branch mechanism**: `rememberCurrentState()` and `returnFromBranch()` allow temporary state changes (e.g., self-isolation during lockdown) without losing the original state context. This supports scenarios where an agent temporarily leaves `ReactiveTestAndIsolate.REACTIVE_PCR` for `LockdownIsolation.ISOLATE` and returns via `graduallyRestoreBehaviour()`.

4. **Behaviour models** (per-person):
   - `FixedBehaviour` — DO_NOTHING (unresponsive baseline)
   - `Symptomatic` — basic symptomatic response with compliance
   - `ReactiveTestAndIsolate` — tests when symptomatic, self-isolates with compliance fatigue
   - `SmartAgentTesting` — tests when high-risk (contact notification) via app
   - `SmartAgentLFTTesting` — LFT first, then reflex PCR
   - `LockdownIsolation` — strict sociability decrease during lockdown
   - `NonCompliant` — all modifiers = 1 (ignores all guidance)

5. **Policy models** (per-outbreak):
   - `NoControl` — DEFAULT (no intervention baseline)
   - `ReactiveLockdown` — MONITOR → LOCKDOWN → MONITOR based on trigger thresholds at 95% confidence

6. **Triggers**: `Trigger` interface for outbreak-level thresholds:
   - `TEST_POSITIVITY` — fraction of positive tests
   - `SCREENING_TEST_POSITIVITY` — population screening positivity
   - `TEST_COUNT` — absolute test count
   - `HOSPITAL_BURDEN` — hospitalisation rate

## Rationale

- Enum-based states provide type safety and exhaustiveness (compiler warns on missing cases).
- The two-phase update (history then state) mirrors the temporal ordering of events within a day: contacts and tests happen, then decisions are made.
- The branch mechanism supports nested state changes (lockdown overriding normal behaviour) without requiring a single monolithic state space.
- Evidence-driven states (SmartAgentTesting, ReactiveTestAndIsolate) use the `RiskModel` computed log-odds to trigger actions.

## Consequences

- Each behaviour model is independent; there is no compositional framework for combining multiple behaviours simultaneously (beyond the branch mechanism which is limited to one override).
- The state spaces are small and hand-coded; adding a new behaviour requires implementing `BehaviourState` and wiring it into `ExecutionConfiguration`.
- Policy transitions use `confidentlyGreaterThan(0.95)` which requires accumulating sufficient evidence over the history window. This introduces a delay between a policy trigger crossing a threshold and the actual state transition.
- No tests exist yet for policy models or behaviour models as integrated systems.

## Technical Debt

- `NonCompliant` state uses `periodTrigger(30)` for reset — the 30-day hard-coded period should be configurable.
- No policy model supports phased or graduated interventions (e.g., reduced mobility instead of full lockdown).
- The branch stack has no limit and could grow unbounded under rapid policy changes (unconfirmed).
