# Priorities

This is an evolving list of priorities.
These are the starting point of a collaborative discussion with the user.
The discussion, and research will feed into implementation scoping.
These are the basis for a discussion and not for implementation.

## Document as-is project architecture and technical debt.

**Goal:** Useful set of architecture decision documents that give enough context to frame future development.
**Status:** Not started
**Effort:** Low
**Value:** Moderate

- Populate `design/adr` folder with as-is technical architecture from code analysis

## Document as-is methods

**Goal:** useful set of mathematical methods and where they are used.
**Status:** Not started
**Effort:** Low
**Value:** Moderate

- Populate `design/methods` folder with as-is methods from code and experiments in `analysis/R/old` directory.
- Particularly methods around in-host models need documentation

## Improve testing and document existing features.

**Goal:** Rewrite tests using proper assertions & in the long term move towards full test coverage and test driven development
**Status:** Not started
**Effort:** High
**Value:** High

- Identify and rewrite tests for utility functions that can be directly tested without full simulation
- Create set of simple simulation setups with expected behaviour (e.g. 1 agent, 2 agents)
- Identify simulation configurations that mimic more deterministic models and compare simulation outputs to well known epidemiological relationships e.g. SEIR models

## Add AI skills to the project to make it easier to work with.

**Goal:** Enable chat interface to model via opencode / claude code
**Status:** Not started
**Effort:** Moderate
**Value:** High

- create a skill to generate a valid configuration file given a testable hypothesis.
- create a skill to run the simulation locally or over SSH on HPC.
- create a skill to analyse the outputs of a run.
- create a skill to create new vesion of JPanSim with extended behaviour or policy models.

## Extend analysis scripts

**Goal:** Improve completeness of R package
**Status:** Not started
**Effort:** Low
**Value:** Low

- static data set of simulation output for testing and examples.
- graphing for final state outputs from multiple model runs
- statistical analyses to prove hypotheses
- `analysis/R/old/abm-output.Rmd` and `analysis/R/old/analysis.Qmd`

## Integrate in-host modelling work and in host calibration

**Goal:** Adopt in host modelling work by Conor and integrate into platform
**Status:** Blocked
**Effort:** Unknown
**Value:** Unknown

- need to find and import old scripts and documentation into this project from github and overleaf
- need mechanism to calibrate models to real world parameters

## Add venues to model

**Goal:** Allow policy models to investigate school closures 
**Status:** Blocked
**Effort:** Unknown
**Value:** Unknown

- implementation of things like school and work-place closures
- would require significant effort in creating appropriate network, which is why we looked into repository and reference data classes.
- alternatives maybe possible by selectively decreasing relationship strength between close age groups

## Alternative server frontend

**Goal:** Allow interactive use of simulation
**Status:** Deferred
**Effort:** Unknown
**Value:** Unknown

- run single simulation as a REST api server using simplified configuration
- allow single step model advance & output model state using REST calls.
- possible to have API calls to make dynamic changes to the model parameters, e.g. adjust person state / outbreak state
- allow cloning of simulation at point of changing model parameters and investigate divergence between counterfactual

