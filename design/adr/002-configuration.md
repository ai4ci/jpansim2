# ADR-2: Configuration Architecture — Immutables, Jackson Polymorphism, and Combinatorial Grid

**Status**: Accepted

**Date**: 2026-05-02

## Context

JPanSim2 experiments involve combinatorial design: multiple environment setups crossed with multiple execution configurations and facets (modifications), each replicated for stochastic variation. The configuration must support:
- JSON serialisation with human-readable comments
- Polymorphic type selection for networks, demographics, in-host models, behaviours, and policies
- Incremental modification via facets that alter base configurations
- Replication for both setups and executions
- SLURM-aware path and batch configuration

## Decision

All configuration uses the Immutables library (`@Value.Immutable` and `@Value.Modifiable` interfaces) with Jackson JSON serialisation for polymorphic type selection. The design supports:

1. **Immutable interfaces** (`@Value.Immutable`) for all configuration classes, with builders generated at compile time.
2. **Polymorphic runtime selection** via `@JsonTypeInfo` on base interfaces. Subtypes map to JSON discriminator values:
   - Networks: `"erdos-reyni"`, `"watts-strogatz"`, `"barabasi-albert"`
   - Demographics: `"unstratified"`, `"location-aware"`, `"age-stratified"`
   - In-host: `"phenomenological"`, `"stochastic"`, `"markov"`
   - Behaviours: `"fixed"`, `"symptomatic"`, `"smart-agent-testing"`, etc.
   - Policies: `"no-control"`, `"reactive-lockdown"`
3. **Facets** — `ExecutionFacet` and `SetupFacet` each contain a list of `Modification` objects that are merged into base configurations via reflection (`ReflectionUtils.merge()`).
4. **Experiment grid** — The formula is: `total simulations = setups × executions × setup_replications × execution_replications`. Each combination has a distinct URN.
5. **Version checking** — `checkConfigVersion()` validates config version against the running JAR version using a resource file generated at build time.

The config hierarchy is:
```
ExperimentConfiguration (root)
  ├── BatchConfiguration (SLURM/execution params)
  ├── List<SetupConfiguration> (network + demographics)
  └── List<ExecutionConfiguration> (R0, behaviour, policy, in-host)
```

## Rationale

- Immutables provides clean builder APIs and Jackson integration out of the box, reducing boilerplate.
- Polymorphic JSON allows the same top-level schema to encode any combination of model components without enum-based dispatch.
- Reflection-based facet merging avoids the need to define per-interface merge logic.
- The combinatorial grid enables systematic sensitivity analysis and parameter exploration.

## Consequences

- Reflection-based merging (`ReflectionUtils.merge()`) is fragile — it copies field-by-field without type awareness. Deep merging of nested objects relies on `ReflectionUtils.modify()` which uses reflection to set fields on nested objects.
- The `ReflectionUtils` approach means configuration changes cannot be validated at compile time; errors appear at runtime.
- The combination of Immutables + Jackson + MapStruct requires a carefully configured annotation processor path, documented in `design/build.md`.
- JSON config files can include comments (Guava module configured), which aids reproducibility, but this is non-standard JSON.
- `config/refdata/` (CSV-based reference demographic data loading) is WIP and not yet wired in.

## Technical Debt

- The reflection-based merging could be replaced with a proper JSON-merge library (e.g., Jackson `treeToValue` with `MERGE_OBJECTS` feature) for correctness.
- The version checking mechanism relies on a build-time generated resource file and may drift between development and release versions.
