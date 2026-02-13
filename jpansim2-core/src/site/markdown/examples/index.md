# Sample configuration files

- [Default configuration](default/config.json)

  A baseline simulation using an Erdos-Rényi contact network and
  default parameters (r0 ≈ 1.75). Useful as a starting point for
  experiments and for producing standard CSV exports (demographics,
  summary, infectivity profiles, final state and behaviour outputs).

- [Age Stratified](age-stratification/config.json)

  Demonstrates age-stratified demographics and age-dependent
  parameter adjustments (for example case fatality and hospitalisation
  rates). Use this example to explore how outcomes vary with an age
  structured population.

- [Behaviour comparison](behaviour-comparison/config.json)

  Compares alternative behaviour models via a `behaviour` facet
  (for example smart-agent, reactive testing and symptomatic
  behaviours). Run this example to evaluate how different
  individual-level behaviours affect epidemic trajectories.

- [Concurrency test](concurrency-test/config.json)

  Uses a Barabási–Albert network and a higher basic reproduction
  number (r0) to explore dynamics where many concurrent
  transmissions are possible; includes exports that support contact
  network and historical test analysis.

- [In-host test](in-host-test/config.json)

  Provides a facet that switches between multiple in-host models
  (markov, phenomenological, stochastic). Use this example to
  compare how alternative in-host representations change infectious
  profiles and downstream population-level outcomes.

- [Lockdown compliance](lockdown-compliance/config.json)

  Illustrates reactive lockdown policies and varying isolation
  severity. Facets allow changing trigger thresholds and maximum
  social contact reduction to study policy sensitivity and compliance
  effects.

- [Network types](network-type/config.json)

  Runs equivalent scenarios on different network topologies
  (Erdos-Rényi, Watts–Strogatz, Barabási–Albert) and across a small
  range of R values to compare how topology affects spread.

- [Test R0](test-R0/config.json)

  A compact example that varies the basic reproduction number (R)
  via a facet (1.0, 2.0, 3.0). Useful for sensitivity analysis of
  epidemic size and timing with different transmissibility.

### Json schema

The configuration files conform to a JSON schema which is found below. However 
this is a generated file and not all details are present.

- [Configuration schema](schema.json)