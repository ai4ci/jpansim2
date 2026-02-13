# Getting Started

This documentation site contains the API reference and a short
introduction to using JPanSim2. For a fuller project overview, examples
and release artifacts see the project repository and releases:

- [Project landing page](../index.html)
- [Project on GitHub](https://github.com/ai4ci/jpansim2)
- [Releases (jar artifacts)](https://github.com/ai4ci/jpansim2/releases)
- [JavaDocs](apidocs/index.html)
- [Configuration examples](examples/index.html)

## Background and purpose

JPanSim2 is a discrete time, stochastic agent based simulation for
infectious disease outbreaks. Transmission is simulated on an explicit
social network whose links are weighted by relationship closeness.
Agents have in-host viral load and immunity models and a behaviour state
model that governs mobility, test-seeking and compliance. The framework
supports top-down policy interventions and flexible configuration to
explore outbreak and behavioural scenarios.

## Installation

The simulation requires a Java 11 runtime environment. JPanSim2 is
distributed as a single dependency free executable jar from the
releases page.

## Setting up

### Directory layouts

The application is configured with a JSON file that defines the
simulations to be executed and the output directory. The
configuration file may be placed in the current working directory or
specified explicitly on the command line.

Example configuration files and a JSON schema are available in
examples](examples/index.html). When running the application locally, choose or create an
output directory; a default configuration named `config.json` will be
looked for in that output directory unless overridden with `-c`.

Outputs will be written to the output directory as CSV files. When
running on SLURM, outputs for each node are placed in numbered
sub-directories under the chosen output directory.

If no output directory is specified, the current working directory is
used.

### Configuring a simulation

A simulation configuration is organised into three complementary
parts:

- **Batch configuration**

  The top-level file that describes how environments and executions are
  assembled and run. A batch may contain multiple setups and
  executions and controls replication and output routing across runs.

- **Setup configuration**

  Defines a simulation environment: the population, demographic
  features, social network and baseline outbreak parameters. A single
  batch may include several setups to compare different environments.

- **Execution configuration**

  Specifies run-time parameters for a simulation execution, for
  example the number of time steps, outbreak scenario overrides and the
  number of replicates to perform on a constructed environment.

In simple cases a batch can contain a single setup and a single
execution. More complex experiments combine multiple setups,
executions and facets to produce a grid of runs for comparison, and run
each of these across many replicas.

### Configuring multiple simulations

A configuration file may define multiple simulations. A base
configuration can be extended with *facets* which specify lists of
modifications to the base configuration; every combination of facet
modifications is executed to form a comparison grid. Each simulation
execution has a unique identifier (a URN) and produces CSV output
written to the configured output directory.

Set up environment configurations and facets to describe variations in
population, outbreak parameters or behaviours. Environments may be
constructed once and re-used across multiple executions. Where
replication is required, the configuration allows multiple replicated
environments to isolate the effect of stochasticity.

Execution configurations can also be extended with facets to explore
variations in run-time parameters such as the number of time steps,
outbreak scenario overrides and the number of replicates to perform on
each a constructed environment.

The number of simulation environments built is the product of the
number of setup facets and their modifications, multiplied by the
number of replicated environments. For example, a configuration with 2
setup facets each with 3 modifications and 10 replicated environments
would build 180 environments (3x3x10).

The number of simulations run for each environment is the product of
the number of executions and their modifications, multiplied by the
number of replicates. For example, a configuration with 2 facets each
with 3 modifications and 10 replicates would run 180 simulations per
environment (3x3x10).

## Running JPanSim2

### Running locally

In the simplest form, place a configuration file in an empty directory
named `config.json` and run the jar from that directory. Outputs are
written as CSV files, or in the case of larger output as DuckDB
databases into the same directory for downstream analysis. E.g.:

```bash
java -jar path/to/jpansim2-<VERSION>.jar
```

Command line options include:

The application accepts the following command line options:

- `-o, --output <output directory>`

  The path to the output directory. Defaults to the current working
  directory.

- `-c, --config <config file>`

  The path to the configuration file. Defaults to `config.json` in the
  output / working directory.

### Running on SLURM

A sample submit script is provided (for example `slurm.sh` in the
repository). To run many environment setups in parallel across SLURM
nodes use the array facility, for example:

```bash
sbatch --array=1-32 slurm.sh
```

When using SLURM the outputs for each node are placed in numbered
sub-directories under the chosen output directory.

## Analysing the results

Each simulation writes CSV files to the output directory. The
repository includes an analysis notebook and scripts in the
`analysis` directory of the project which may be used as a starting
point for downstream analysis. The project authors also maintain
example analysis code that expects the CSV output layout produced by
the simulations.

## Useful links within the documentation

- [Configuration API: io.github.ai4ci.config]
  (apidocs/io/github/ai4ci/config/package-summary.html) — reference for
  supported configuration options used by the simulation.
- [Configuration examples](examples/index.html)
- [Source repository](https://github.com/ai4ci/jpansim2)
