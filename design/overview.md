# Background and purpose

- `jpansim2-core/src/site/markdown/index.md` user facing documentation
- `README.md` high level user facing documentation


# Significant files and locations

## design artefacts
- `design/overview.md`: this document
- `design/build.md`: build instructions
- `design/conventions.md`: code conventions

## Top level Maven project
- Useful places to open first: `pom.xml`, `jpansim2-core/pom.xml`
- Top-level repository root: this is the Maven reactor root and the usual place to invoke `mvn` for aggregate tasks. 
- Some project documentation lives in this parent project

## core module
- Core module: `jpansim2-core/` — main sources, tests and the module that produces the distributable `-jar-with-dependencies.jar`.
- Most user facing documentation will live in this module in the javadocs and `src/site/` directory

### Packages
- `io.github.ai4ci.config`: Specification of JSON configuration files for 
input using Jackson annotations. `Partial` versions of configuration interfaces used to override or modify default
settings of `Immutable` versions. N.B. some low level structural configuration is also in 
`io.github.ai4ci.functions` package
- `io.github.ai4ci.abm`: main structural classes of the agent based model.
- `io.github.ai4ci.output`: specification of output views of model for CSV
and DuckDB serialisation.
- `io.github.ai4ci.flow`: simulation construction (`builders` subpackage), 
execution (`mechanics` subpackage) and output (`output` subpackage)
- `io.github.ai4ci.flow`:
- Consult `package-info.java` javadocs to understand more detail on the 
structure of the project.

### Key classes
- `io.github.ai4ci.flow.SimulationMonitor` entry point to an end to end 
simulation run.
- `io.github.ai4ci.flow.builders.AbstractModelBuilder` main entry 
to things relating to building a simulation
- `io.github.ai4ci.flow.mechanics.Updater` main per simulation step
execution engine
- `io.github.ai4ci.flow.output.SimulationExporter` responsible for 
writing output
- `io.github.ai4ci.abm.Outbreak` mutable main simulation data class.

## codegen module
- (Work in progress)
- Codegen module: `jpansim2-codegen/` — this is a placeholder. In the future
we will look to use a annotation processing framework to build specific part of the
`jpansim2-code` codebase

## analysis directory
- (Work in progress)
- An R package that provides analysis tools to process the simulation output.

## publications directory
- Currently limited but contains some background to the purpose and scope

## scratch directory
`.gitignore`d contents and can be used for running end to end testing