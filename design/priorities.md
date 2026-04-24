# Priorities

This is an evolving roadmap for the next stage of development.
It is intended to support collaborative discussion with the user before work is
scoped in detail.

The main recommendation for the next stage is:

1. make the current platform easier to understand;
2. make the current platform safer to change;
3. only then accelerate new feature work.

That sequence fits the current state of the repository:

- the multi-module Maven migration is recent;
- there is already useful design/build documentation to extend;
- the test tree mixes proper unit tests with exploratory and deprecated code;
- there is a backlog of older analysis notebooks and in-host work that needs to
  be reconciled with the current implementation.

## Prioritisation principles

- Prefer work that reduces ambiguity for every later task.
- Prefer work that improves confidence in scientific and software behaviour.
- Treat blocked research features separately from immediately actionable work.
- Defer interface and platform expansion until the CLI simulation workflow is
  better documented and tested.

## Proposed order for the next stage

| Priority | Theme | Why it comes first |
|---|---|---|
| 1 | Document as-is architecture and methods | Future feature and test work is currently slowed by missing context |
| 2 | Improve testing and define executable reference scenarios | The codebase needs a trustworthy baseline before substantial changes |
| 3 | Add AI skills for routine workflows | Automation becomes much more useful once the model and workflows are legible |
| 4 | Consolidate analysis package and example outputs | Best done after reference scenarios exist |
| 5 | Integrate further in-host modelling and calibration work | High value, but currently blocked by missing inputs and validation approach |
| 6 | Add venue-aware modelling | Depends on clearer requirements, data, and testing harnesses |
| 7 | Alternative server frontend | Useful later, but not a good next-stage investment |

## 1. Document as-is project architecture and technical debt

**Goal:** A useful set of architecture records that explain how the current
platform is assembled, where key extension points live, and where technical debt
is constraining development.
**Status:** Recommended first
**Effort:** Low to moderate
**Value:** High

**Why now**

- The repository has already been reorganised into a multi-module Maven project.
- `design/overview.md` and `design/build.md` now give enough structure to extend
  into focused ADRs.
- The code contains visible TODOs around output, builder composition, reporting,
  venue modelling, and performance; these should be turned into documented debt
  rather than rediscovered ad hoc.

**Suggested outputs**

- Populate `design/adr/` with short as-is ADRs covering:
  - simulation execution pipeline;
  - configuration and schema generation approach;
  - output/export strategy;
  - behaviour and policy state-machine architecture;
  - testing strategy and known limitations.
- Add a short technical debt section to each ADR where appropriate.
- Distinguish between deliberate design choices, temporary compromises, and
  unknowns.

**Done when**

- A new contributor can locate the main execution path, configuration path, and
  output path without reading large parts of the code.
- Known technical debt is captured as named items rather than implied by code
  comments alone.

## 2. Document as-is methods

**Goal:** A useful set of mathematical and modelling notes that explain the
current methods and where they are used.
**Status:** Recommended first
**Effort:** Low to moderate
**Value:** High

**Why now**

- The in-host area appears scientifically important and is represented in both
  production code and older analysis notebooks.
- Calibration and delay/distribution logic are central to model behaviour and
  should be documented before they are changed.
- This work reduces risk for both testing and future model extension.

**Suggested outputs**

- Populate `design/methods/` using the current code and the notebooks under
  `analysis/R/old/`.
- Start with:
  - in-host phenomenological model;
  - in-host stochastic and Markov formulations;
  - calibration of transmission-related parameters;
  - delay distributions, kernels, and risk model evidence accumulation.
- For each method note:
  - what the method represents;
  - where it is implemented;
  - what assumptions are implicit;
  - what remains uncertain or unvalidated.

**Done when**

- There is a clear mapping from key scientific concepts to code locations.
- The in-host and calibration work can be discussed without reopening old
  notebooks each time.

## 3. Improve testing and document existing features

**Goal:** Create a trustworthy baseline of automated tests and small executable
reference scenarios, and use them to document current behaviour.
**Status:** Recommended second
**Effort:** High
**Value:** High

**Why now**

- Some tests are already solid unit tests, especially around utility and
  distribution code.
- Other tests are exploratory, print-heavy, or live in deprecated areas, so the
  current suite does not yet communicate confidence clearly.
- New feature work should be gated by a smaller set of high-signal tests that
  describe intended behaviour.

**Suggested outputs**

- Classify the current test tree into:
  - stable unit tests;
  - exploratory experiments;
  - deprecated or archival code.
- Rewrite or replace exploratory tests with assertions where feasible.
- Create a small library of reference scenarios, for example:
  - one-agent no-transmission baseline;
  - two-agent forced-contact transmission scenario;
  - simple deterministic-style outbreak progression for regression checks;
  - policy or behaviour trigger scenarios with explicit expectations.
- Prefer tests against success criteria and observable outputs over internal
  implementation details.
- Use the scenarios to improve user-facing examples and future AI tooling.

**Done when**

- There is a small, understandable suite that can be trusted during refactors.
- The team can point to a handful of reference scenarios that define current
  behaviour.

## 4. Add AI skills to the project to make it easier to work with

**Goal:** Enable routine model development, execution, and analysis workflows to
be driven reliably through the chat interface.
**Status:** Recommended third
**Effort:** Moderate
**Value:** High
**Dependencies:** Architecture, methods, and testing baseline

**Why after the documentation and testing work**

- Skills are much more valuable when they can rely on stable documentation and
  reference scenarios.
- Otherwise they risk automating guesswork.

**Suggested outputs**

- Create a skill to generate a valid configuration file from a testable
  hypothesis or experiment description.
- Create a skill to run simulations locally or over SSH/HPC.
- Create a skill to analyse run outputs and summarise findings.
- Create a skill to scaffold extensions for new behaviour or policy models.

**Done when**

- Common workflows can be repeated safely with less manual setup.
- Skill outputs align with documented examples and validated scenarios.

## 5. Extend analysis scripts and package completeness

**Goal:** Improve the completeness and usability of the R analysis package.
**Status:** Recommended after reference scenarios exist
**Effort:** Low to moderate
**Value:** Moderate
**Dependencies:** Testing baseline and reusable example outputs

**Suggested outputs**

- Add a static dataset of simulation output for package examples and tests.
- Add graphing for final-state outputs across multiple runs.
- Add statistical analyses that support testable hypotheses, not just ad hoc
  exploration.
- Review and either migrate or retire older work in:
  - `analysis/R/old/abm-output.Rmd`
  - `analysis/R/old/analysis.qmd`
  - related in-host notebooks.

**Done when**

- The package has a clear role in the workflow.
- Example outputs and tests do not depend on fragile local reruns.

## 6. Integrate in-host modelling work and in-host calibration

**Goal:** Adopt the wider in-host modelling work and integrate it coherently into
the platform.
**Status:** Blocked
**Effort:** Unknown
**Value:** Potentially high
**Dependencies:** Methods documentation, reference scenarios, access to missing
material

**Current blockers**

- Need to locate and import older scripts and documentation from GitHub and
  Overleaf.
- Need a clear mechanism for calibrating models to real-world parameters.
- Need agreement on what level of validation is required before integration.

**Suggested next action**

- Treat this as a discovery and scoping task after the earlier documentation
  work, not as immediate implementation.

## 7. Add venues to the model

**Goal:** Allow policy models to investigate school closures and similar
venue-specific interventions.
**Status:** Blocked
**Effort:** Unknown
**Value:** Potentially high
**Dependencies:** Network design decisions, data assumptions, and test harness

**Why not yet**

- Venue-aware modelling likely requires substantial changes to network
  construction, contact semantics, and calibration.
- The existing code already hints at this need, but not yet at a settled design.

**Notes**

- Full implementation may require explicit venue structure such as school and
  workplace layers.
- A lighter interim approach may be possible by selectively reducing
  relationship strength between age or context-linked groups, but this should be
  treated as an approximation, not a substitute for proper venue modelling.

## 8. Alternative server frontend

**Goal:** Allow interactive use of the simulation through a server interface.
**Status:** Deferred
**Effort:** Unknown
**Value:** Unclear until core workflows are stabilised

**Reason for deferral**

- The current CLI and batch workflow should be made easier to understand and
  test before introducing another deployment and interaction model.
- A server frontend would add API design, state management, and reproducibility
  concerns on top of an already complex core.

**Possible future scope**

- Run a single simulation as a REST API server using simplified configuration.
- Allow single-step advancement and state inspection via API calls.
- Allow controlled parameter changes and branching into counterfactual runs.

## Recommended definition of the next stage

The next stage should be defined narrowly as:

1. architecture capture;
2. methods capture;
3. a high-signal testing baseline with a few reference scenarios;
4. only then workflow automation and selected feature expansion.

That gives the project a stronger foundation for TDD, design discussion, and
scientific extension without overcommitting to large blocked features too soon.
