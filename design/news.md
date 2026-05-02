# Project status

## 2026-05-02 Populated design documents

Comprehensive exploration of the codebase completed. Design documents added:

- **Architectural Decision Records (5)** in `design/adr/`:
  - Execution pipeline (ADR-1): Memory-aware producer-consumer with OSHI monitoring
  - Configuration architecture (ADR-2): Immutables, Jackson polymorphism, combinatorial facets
  - Output/export strategy (ADR-3): Pre-mutation snapshots with async writers
  - State machine architecture (ADR-4): Enum-based behaviour and policy models with branch mechanism
  - Testing strategy (ADR-5): Coverage gaps and reference scenario approach

- **Methods documentation (6)** in `design/methods/`:
  - Calibration (001): R₀ calibration via percolation theory and Poisson binomial quartic surrogate
  - Phenomenological in-host (002): Biphasic logistic curves with exposure superposition
  - Stochastic in-host (003): Compartment model with virions, target cells, immune cells
  - Markov in-host (004): Discrete-time disease and symptom progression chains
  - Risk model (005): Bayesian temporal risk with symptom/test/contact evidence and convolution kernels
  - Distributions (006): Parametric, empirical, delay distributions and interpolation

- Created directory structure for `design/implementation/` (scoping, in-progress, complete subdirectories)
- Updated `design/overview.md` with new artefact listings