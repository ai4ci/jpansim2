# ADR-5: Testing Strategy and Known Limitations

**Status**: Accepted

**Date**: 2026-05-02 (updated 2026-05-02)

## Context

The project has a mix of established unit tests, exploratory scripts, and deprecated code. The test suite does not clearly communicate confidence in scientific correctness, and critical components (`Updater`, `SimulationMonitor`, `SimulationFactory`, policy/behaviour models) lack coverage.

## Decision

The current test architecture uses JUnit Jupiter 5 + AssertJ with tests in `jpansim2-core/src/test/java/io/github/ai4ci/`. Tests are not separated into unit/integration/e2e categories. Test resources include example config JSON files and a JSON schema in `src/test/resources/`.

**Current state (as of 2026-05-02):** 90 tests across 22 classes, all passing.

**Key fix**: Upgraded from Maven Surefire 2.12.4 (2014, JUnit 4 only) to 3.5.2 with JUnit Jupiter engine. Before this, only 11 tests were actually discovered via bean-introspection fallback. The upgrade uncovered 4 pre-existing test failures that were never running:
- `GaussianKernel` missing `Serializable` (fixed)
- `MathematicalFunction` builder missing required attributes (fixed)
- `PersonDemographic` builder missing required `entity` (fixed)
- `TestTestUtils` assertion mismatch with Markov model behavior (fixed)

Based on the codebase exploration, the test coverage breaks down as:

**Well-covered areas:**
- Distribution utilities (`functions/`) — delay distributions, empirical distributions, mathematical functions
- Utility classes (`util/`) — thread safety, reflection, I/O, eigenvalue decomposition
- Configuration serialisation (`config/TestJackson`) — Jackson round-trips
- Bayesian risk model (`flow/mechanics/TestRiskModel`)
- Phenomenological in-host model (`flow/mechanics/TestPhenomenologicalModel`)
- Viral load model (`flow/mechanics/TestViralLoadModel`)

**Poorly covered areas:**
- No tests for `Updater` pipeline with real outbreak data
- No tests for `SimulationMonitor`, `SimulationFactory`, `SimulationExecutor`, `ExecutionBuilder` end-to-end
- No tests for policy models (`ReactiveLockdown`)
- No tests for behaviour models (`SmartAgentTesting`, etc.)
- No tests for network generators (Erdos-Renyi, Watts-Strogatz, Barabasi-Albert)
- No tests for output writers (CSV/DuckDB)
- No tests for `Outbreak` lifecycle

**Archived code:**
- `deprecated/` directory contains 8 files from concurrency experiments (Threading, async IO, bitsets) — neither removed nor integrated

## Rationale (for recommended approach)

The project priorities (documented in `design/priorities.md`) identify testing improvement as Priority 3. The recommended approach is:

1. **Classify existing tests** into stable unit tests, exploratory experiments, and deprecated/archival code.
2. **Create reference scenarios** — small, executable test simulations with explicit expectations:
   - One-agent no-transmission baseline
   - Two-agent forced-contact transmission scenario
   - Simple deterministic-style outbreak progression for regression checks
   - Policy or behaviour trigger scenarios with explicit expectations
3. **Prefer success-criteria tests** over implementation-detail tests.

## Consequences

- The current test suite passes but does not prove scientific correctness. New contributors cannot rely on it as a safety net.
- No CI gate exists for test failures beyond standard PR triggers.
- The `deprecated/` directory clutters the test tree and should be cleaned up.

## Technical Debt

- The `deprecated/` test files should be committed in a separate archive or removed.
- No model reference scenarios exist as executable tests (priority #2 in `priorities.md`).
- The combination of `deprecated/` tests and exploratory print-heavy tests makes it hard to identify which tests actually communicate confidence.
