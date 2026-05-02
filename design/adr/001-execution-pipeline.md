# ADR-1: Simulation Execution Pipeline — Monitor/Factory/Executor with Memory-Aware Backpressure

**Status**: Accepted

**Date**: 2026-05-02

## Context

JPanSim2 simulations are memory-bound. A single outbreak with tens of thousands of agents, each holding in-host state, risk model state, policy state, and full contact/exposure histories, can consume 2-4 GB of heap. Experiments typically run hundreds of simulation replications across combinatorial configuration grids. A naive approach of running all simulations concurrently would OOM the system.

## Decision

The execution pipeline uses a three-component producer-consumer pattern with explicit memory-based backpressure:

1. **SimulationMonitor** — Singleton orchestrator thread that monitors JVM heap and system memory via OSHI. It runs the main experiment loop, spawning producers and consumers, and implementing backpressure through pause/resume signals.

2. **SimulationFactory** — Background daemon thread that pre-builds and caches `Outbreak` instances in a `ConcurrentLinkedQueue<Outbreak>` with default cache size of 2. The factory iterates over all setup/execution/replication combinations, building each `Outbreak` once and caching it for reuse across different execution facets on the same environment.

3. **SimulationExecutor** — Extends `PauseableThread`. Each executor handles one simulation to completion, running the daily step loop (`export` then `update` per timestep) until the simulation horizon is reached.

Memory thresholds (three-tier):
- **>80% memory used** or **<1 GB system free**: pause factory production.
- **>90% memory used** or **<0.5 GB free**: pause both factory and executors, run `System.gc()`, set 5-minute abort timer.
- **>95% memory used** or **<0.25 GB free**: abort all simulations if no threads blocked (OOM condition).

## Rationale

- A reactive (RxJava3) approach was explored (commented-out code in `SimulationMonitor.java:367-412`) but the simpler thread-based approach was retained for maintainability.
- Pre-building and caching simulations in the factory reduces setup time for multi-facet experiments where the same environment supports multiple execution configurations.
- Cloning uses serialization-based deep copy (`Cloner`) which handles the full graph of immutable objects efficiently.
- The 80/90/95% cascade is intentionally conservative to avoid the system swapping or running out of memory entirely.

## Consequences

- Single-threaded simulation execution per node (each executor runs one simulation sequentially). The code comments note this as "Amdahl's Law with a vengeance" (`Updater.java:470`) — the sequential update phases create a fundamental bottleneck on high-core machines.
- Memory-based backpressure is reactive (pause after thresholds are crossed), not proactive. There is no predictive model to prevent memory builds from crossing boundaries.
- The commented-out RxJava3 code suggests a desire for finer-grained backpressure that was never completed.
- The factory caches only 2 simulations by default, which limits parallelism for setup-heavy workloads.

## Technical Debt

- The `ExecutionBuilder` is hardwired to `DefaultModelBuilder` with TODOs for factory injection, ServiceLoader discovery, or configuration-based selection (`ExecutionBuilder.java:93-114`).
- The sequential update phases in `Updater` limit scaling on multi-core nodes despite multi-threading elsewhere.
- The commented-out RxJava3 code should either be completed or removed.
