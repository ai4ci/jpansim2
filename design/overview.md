# JPanSim2 — Project Overview

## Design documentation
* The project is a complex historical code base which is in the process of being migrated into a managed development workflow in stages.
* The project has recently been refactored into a multi-module maven project.
* A longer term goal is to migrate the project towards test driven development with fully documented design.
* Design artefacts will be in this `design` folder, with a meaningful snake-case filename.
* Use your memory to keep a record of when you last looked at individual design artefacts, and look for recent modifications to the files. Expect them to change unpredictably without a clean git commit record.
* Design artefact templates are in the `design/templates` folder - use these when populating design documents.
* The `design/overview.md` provides a background to the code structure and is a good entry point for orientation.
* The `design/priorities.md` is collaboratively authored with user and provides a list of current priorities, outstanding issues and technical debt. Value effort trade off is used for deciding on the next priority to implement.
* The `design/news.md` provides a high level list of changes and should be updated in each major commit.
* The `design/adr/` folder should contain architectural decision records as a set of markdown files one per decision.
* The `design/methods/` folder should contain record of scientific methods employed or considered in the project as a set of markdown files or Rmarkdown files.
* The `design/implementation/scoping` folder should contain a detailed description of the planned implementation of tests, issues or features from the `design/priorities.md` file.
* The `design/implementation/in-progress` folder should contain actively worked on implementations.
* The `design/implementation/complete` folder should contain old implementation check-lists for issues or features in the `design/news.md` file.
* This design and implementation process is not expected to be linear. Possible high level processes to follow are:
  1. `discuss priorities` -> `scope implementation` -> `agree implementation with user` -> (repeat) `write tests` -> `implement feature` -> (until) `meets success criteria` -> `update design`
  2. `discuss priorities` -> `explore codebase and do research` -> `update methods or adr` -> (optional) `add emerging issues to priorities`
  3. `discuss priorities` -> `explore design and do research` -> `agree scope with user` -> (repeat) `update prioritised tests` -> (until) `test coverage satisfactory` -> (optional) `add emerging issues to priorities`
* The file `design/conventions.md` records stylistic conventions to adhere to when creating code or documentation.


---

## Quick Mental Model

```
CLI: JPanSim2 → SimulationMonitor (memory-aware scheduling via OSHI)
                    ↓
              SimulationFactory (pre-builds simulation instances)
                    ↓
              ExecutionBuilder (5-stage build pipeline)
                    ↓  uses →  DefaultModelBuilder
                               1. Setup (social network from JGraphT)
                               2. Baseline outbreak (R₀ calibration)
                               3. Baseline persons (demographics, mobility)
                               4. Initialise outbreak (OutbreakState)
                               5. Initialise persons (PersonState, in-host model)
                    ↓
              SimulationExecutor (runs daily step loop)
                    ↓  drives →  Updater
                                   ↓ contacts (JGraphT social network traversal)
                                   ↓ exposures (SocialRelationship.contactProbability)
                                   ↓ in-host update (InHost*State)
                                   ↓ behaviour state machine (BehaviourState)
                                   ↓ policy state machine (PolicyState)
                    ↓
              SimulationExporter → CSVWriter / DuckDBWriter
                    ↓
              R package jpansim2analysis (post-hoc analysis and visualisation)
```

---

## Directory Structure

```
jpansim2/                        ← Maven reactor root; invoke `mvn` here
├── pom.xml                      ← Parent POM (aggregator, groupId io.github.ai4ci, v0.3.3)
├── jpansim2-core/               ← Main module: all production code and tests
├── jpansim2-codegen/            ← Placeholder annotation-processor module (WIP)
├── analysis/                    ← R package `jpansim2analysis`
├── design/                      ← Architecture docs
├── publications/                ← Background on purpose and scope
├── scratch/                     ← .gitignore'd; used for end-to-end test runs
├── README.md                    ← High-level user-facing documentation
├── AGENTS.md                    ← AI agent operating instructions
└── .github/                     ← CI workflows (publish-javadoc, release)
```

---

## Design Artefacts

- `design/overview.md` — this document; essential project context
- `design/build.md` — build, release, SLURM packaging, troubleshooting
- `design/conventions.md` — Java and R coding conventions, Javadoc rules

---

## Maven Module Structure

| Module | Artifact ID | Purpose |
|---|---|---|
| Parent POM | `jpansim2-parent` | Reactor root; shared deps; site plugin |
| `jpansim2-core/` | `jpansim2-core` | All production and test code; produces fat jar |
| `jpansim2-codegen/` | `jpansim2-codegen` | WIP annotation-processor placeholder |

**Key dependencies (jpansim2-core):**
- `org.immutables` — compile-time immutable value classes with Jackson integration
- `org.mapstruct` — compile-time mapper generation (e.g. `HistoryMapper`)
- Jackson 2.x — JSON/CSV serialisation, JSON Schema generation
- JGraphT 1.5.2 — social network graph
- Apache Commons Math3 / Statistics — distributions and stats
- mXparser — formula evaluation in config
- DuckDB JDBC — columnar output database
- OSHI — JVM/system memory monitoring for throttling
- JUnit Jupiter 5 + AssertJ — tests

**Build notes:**
- Main class: `io.github.ai4ci.JPanSim2`
- `exec-maven-plugin` runs `WriteExampleConfig` and `InterfaceSchemaGenerator` at `process-classes` phase
- PlantUML diagrams generated from `.puml` files at build time
- Fat jar produced by `maven-assembly-plugin`

---

## Core Module — Java Package Guide

All sources in `jpansim2-core/src/main/java/io/github/ai4ci/`.

Consult `package-info.java` files for detailed architectural notes within each package.

### `io.github.ai4ci` — Entry Point

- `JPanSim2` — CLI entry point; parses `-o` (output dir) and `-c` (config file) options
- `SlurmAwareLogger` — SLURM-compatible logging

### `io.github.ai4ci.abm` — Agent-Based Model Data Structures

Central mutable simulation container and all agent data.

| Class/Interface | Role |
|---|---|
| `Outbreak` | **Central mutable simulation container**: population list, social network, config, state, history, policy state machine |
| `Person` | Individual agent |
| `PersonDemographic` | Static per-person demographics |
| `PersonBaseline` | Immutable per-person baseline (mobility, compliance, behaviour model) |
| `PersonState` | Mutable per-time-step person state |
| `PersonHistory` | Daily contact, exposure, test record |
| `PersonTemporalState` | Interface exposing timestamped observables: viral load, severity, `isInfectious()`, `isSymptomatic()` |
| `SocialRelationship` | Weighted edge between two agents; computes `contactProbability(mobilityA, mobilityB)` |
| `Contact` | Materialised contact event between two agents |
| `Exposure` | Viral dose event linking exposer to exposee |
| `TestResult` | Test observation: viral load truth, noisy sample, result vs. LoD, log-likelihood ratio |
| `OutbreakBaseline` | Immutable R₀-calibrated transmission parameters |
| `OutbreakState` | Mutable outbreak-level state (screening settings, policy state) |
| `OutbreakHistory` | Outbreak-level daily history |
| `Calibration` | Calibrates transmission parameter to match configured R₀ |
| `ModelNav` | Helper accessors for reading config/baseline during build and runtime |
| `ModelUpdate` | Pre-canned update configurations for the `Updater` |

**Sub-packages:**

- `abm.inhost` — Three in-host viral dynamics models:
  - `InHostPhenomenologicalState` — curve-fitted observed infection patterns
  - `InHostStochasticState` — stochastic difference equations (binomial/Poisson) for virions, TEIR cells, immune cells
  - `InHostMarkovState` — Markov chain discrete state transitions

- `abm.behaviour` — Individual behaviour state machines (enum-based, implement `State.BehaviourState`)

- `abm.policy` — System-level policy state machines:
  - `NoControl` — baseline no-intervention
  - `ReactiveLockdown` — threshold-triggered lockdown (MONITOR → LOCKDOWN → TRANSITION)
  - `PolicyModel` and `Trigger` interfaces

- `abm.riskmodel` — Bayesian temporal risk estimation:
  - `RiskModel` — accumulates symptom, test, and contact evidence via convolution filters and log-odds Bayesian update
  - `ConvolutionFilter` — temporal weighting kernels for retrospective evidence

### `io.github.ai4ci.config` — Configuration (JSON/Jackson)

All configuration is immutable (`@Value.Immutable`) with Jackson polymorphism for runtime selection.

| Sub-package | Contents |
|---|---|
| `config` | `ExperimentConfiguration`, `TestParameters`, `BatchConfiguration`, `ExecutionFacet` |
| `config.setup` | `SetupConfiguration`, `NetworkConfiguration` + implementations (`ErdosReyni`, `WattsStrogatz`, `BarabasiAlbert`); `DemographicConfiguration` + implementations |
| `config.execution` | `ExecutionConfiguration` — R₀, behaviour model, policy model, available tests, in-host config |
| `config.inhost` | `InHostConfiguration` + polymorphic implementations for each in-host model |
| `config.refdata` | WIP — CSV-based reference demographic data loading (e.g. `UKCensus`); not yet wired in |

### `io.github.ai4ci.flow` — Simulation Execution Pipeline

| Component | Role |
|---|---|
| `SimulationMonitor` | **Top-level entry**: monitors JVM + system memory (OSHI); throttles and spawns `SimulationExecutor` threads |
| `SimulationFactory` | Pre-configures and caches simulation instances for batch execution |
| `SimulationExecutor` | Runs a single simulation to completion |
| `ExecutionBuilder` | Orchestrates the 5-stage build pipeline |

**`flow.builders` — Model Construction (5 stages):**

1. `DefaultNetworkSetup` — generates social network graph from `NetworkConfiguration`
2. `DefaultOutbreakBaseliner` — calibrates R₀, sets policy/behaviour defaults
3. `DefaultPersonBaseliner` — assigns demographics, mobility, compliance, app-use probability
4. `DefaultOutbreakInitialiser` — sets initial `OutbreakState`
5. `DefaultPersonInitialiser` — sets initial `PersonState`, initialises in-host model

`AbstractModelBuilder` → `DefaultModelBuilder` composes these 5 focused interfaces.

**`flow.mechanics` — Per-Step Engine:**

| Component | Role |
|---|---|
| `Updater` | **Daily step engine**: contacts → exposures → state/history updates for all agents and outbreak |
| `StateMachine` | Orchestrates `updateHistory` then `nextState` for behaviour and policy models |
| `StateMachineContext` | Shared policy signals available to all state machines |
| `State` | Base interface; sub-types: `BehaviourState` (per-person enum), `PolicyState` (per-outbreak enum) |

**`flow.output` — Export Pipeline:**

Exports happen at defined lifecycle stages (START, BASELINE, UPDATE, FINISH) before state mutation.

| Component | Role |
|---|---|
| `SimulationExporter` | Coordinates pre-mutation snapshot exports |
| `CSVWriter` | Writes records to CSV files |
| `DuckDBWriter` | Writes records to DuckDB database files |
| `QueueWriter` | Buffered async writer |

### `io.github.ai4ci.output` — Output Record Specifications

Defines CSV/DuckDB serialisation views:
- `OutbreakCSV`, `OutbreakHistoryCSV`, `OutbreakFinalStateCSV`, `OutbreakBehaviourCountCSV`, `OutbreakContactCountCSV`
- `LineListDuckDB`, `ContactDuckDB`, `PersonDemographicsDuckDB`, `PersonTestsDuckDB`
- `InfectivityProfileCSV`, `DebugParametersCSV`, `OutbreakConfigurationJson`

### `io.github.ai4ci.functions` — Mathematical Functions

Distribution utilities, empirical function fitting, delay distributions — used for sampling and in-host parameter derivation.

### `io.github.ai4ci.util` — Utilities

`Sampler`, `Conversions` (log-odds, probability transforms), `Repository` (CSV loading with FK joins), `Ephemeral` (lazy init), `ThreadSafeArray`, `AtomicDouble`, `PauseableThread`, `DuckDBUtil`, `CSVUtil`, `ReflectionUtils`, etc.

### `io.github.ai4ci.example` — Examples and Schema Generation

- `WriteExampleConfig` — run at `process-classes` phase; produces example JSON configs in `target/generated-config/`
- `InterfaceSchemaGenerator` — run at `process-classes` phase; produces R schema file `target/analysis/schemas.R`

---

## Test Structure

All tests in `jpansim2-core/src/test/java/io/github/ai4ci/`.
Framework: JUnit Jupiter 5 + AssertJ. No integration/unit test separation yet.

| Test area | What it tests |
|---|---|
| `abm/` | Calibration, age stratification, ABM utilities, behaviour state machine |
| `flow/mechanics/` | Phenomenological model, risk model, viral load model |
| `config/` | Jackson serialisation round-trips, convolution kernels, test parameters, CSV repository loading |
| `functions/` | Distribution, empirical function, delay distribution utilities |
| `util/` | Thread safety, reflection, fast I/O, eigenvalue decomposition |
| `deprecated/` | Archived concurrency experiments (retained for reference) |

Test resources: example config JSON files and a JSON schema in `src/test/resources/`.

---

## R Analysis Package (`analysis/`)

**Package name**: `jpansim2analysis`  
**Style**: tidyverse idioms, all namespace-qualified with `::`  
**Documentation**: Roxygen2 with markdown

Key R files:
- `experiment-details.R` — reading/summarising experiment metadata
- `plot-experiments.R` — ggplot2-based simulation output visualisation
- `state.R` — state-related analysis utilities
- `write-repository.R` — writing analysis output repositories

Key imports: `dplyr`, `ggplot2`, `readr`, `duckdb`, `purrr`, `tidyr`, `jsonlite`, `rmarkdown`, `knitr`

The Maven build generates `schemas.R` from Java config classes at `process-classes` phase.

---

## Key Entry Points for Common Tasks

| Task | Where to start |
|---|---|
| Running a simulation | `JPanSim2` (CLI), then `SimulationMonitor` |
| Changing simulation logic (daily step) | `Updater` in `flow.mechanics` |
| Adding a new in-host model | `abm.inhost`, `config.inhost`, `DefaultPersonInitialiser` |
| Adding a new policy | `abm.policy`, wire into `ExecutionConfiguration` |
| Adding a new behaviour | `abm.behaviour`, wire into `ExecutionConfiguration` |
| Adding new CSV/DuckDB output | `io.github.ai4ci.output`, `SimulationExporter` |
| Changing configuration structure | `io.github.ai4ci.config` (Immutables + Jackson) |
| Analysing output | R package `jpansim2analysis` in `analysis/` |
| End-to-end testing | `scratch/` directory |
