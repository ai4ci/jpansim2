# Copilot Notes — Coding Style and Important Paths

## Purpose

This note collects the coding conventions, javadoc rules, significant
file locations and CI/test commands that future chat sessions or a human
developer should know when working on `jpansim2`.

Keep this short and practical; the intention is to speed up future
conversations and to remind a developer of repository conventions and
where to find key files.

## Coding style (Java)

- Follow standard Java conventions: camelCase for methods/fields, Pascal
 Case for classes and interfaces, and 4-space indentation.
- Wrap at 80 characters.
- Prefer immutability for small value objects where reasonable.
- Use modern Java idioms (streams, Optional) whereever reasonable.
- Keep methods short: prefer extracting private helper methods when a
 method grows beyond ~60-80 lines.
- Eclipse formatting and style configuration in `code-style-xml` and
`clean-up.xml` files

## Javadoc rules (project-wide)

- Write Javadoc only for public classes and public methods, interface
  default public methods, and public static fields.
- Use British English spelling in Javadoc and plain ASCII (latin) text
  and punctuation only.
- Author tag: use `@author Rob Challen` in public class/method javadocs.
  Do not specify `@version`.
- Keep class-level Javadoc focussed on the main purpose and features of
  the class; avoid long digressions.
- For methods:
  - If the method behaviour is straightforward, document salient
    behaviour and important pre/post-conditions (no formulae).
  - If the method performs complex mathematical operations, include
    essential formulae. Use simple LaTeX surrounders: inline `\( ... \)`
    or block `\[ ... \]`. Prefer block equations, avoid aligned
    environments. Keep expressions minimal and suitable for MathJax.
- Include short notes in Javadoc linking to downstream uses where
  relevant (for example, which other modules or site pages depend on the
  class). Use plain URLs or relative references where helpful.

# Significant files and locations

## Top level Maven project
- Useful places to open first: `jpansim2/pom.xml`,
  `jpansim2/jpansim2-core/pom.xml`, 
  `jpansim2/dev-notes.md`.  
- Top-level repository root: this is the Maven reactor root and the
  usual place to invoke `mvn` for aggregate tasks. 
- Some project documentation lives in this parent project

## Core module
- Core module: `jpansim2/jpansim2-core/` — main sources, tests and the
  module that produces the distributable `-jar-with-dependencies.jar`.
- Most user facing documentation will live in this module in the javadocs and
`src/site/` directory

### Packages
- `io.github.ai4ci.config`: Specification of JSON configuration files for 
input. `Partial` versions of configuration interfaces used to override or modify default
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



## Analysis subproject
- (Work in progress)
- Analysis: `jpansim2/analysis/` - An R package that provides analysis 
tools to process the simulation output.

# Documentation 

- CI workflow: `.github/workflows/publish-javadoc.yaml`
- Actually performs a full maven build and `site:staging` in a branch.
- Site staging path: default is `${session.executionRootDirectory}/target/staging`.

# CI and release notes

- Release workflow: `.github/workflows/release.yml` (or similar) — builds
  the module, copies artefacts to the repo-root `staging/` and creates a
  GitHub Release.

# Maven build


- Uses UML plugin to generate UML diagrams 
- Uses plantuml plugin to generate addtional
diagrams from `.puml` files in the source code hierarchy, which will generate
`.png` files in the javadoc code hierarchy. 
- Link to generated '.png' diagram files using relative paths from javadoc. 
- Runs main class files in the `io.github.ai4ci.examples` package 
during build to generate code
- Uses Immutables and MapStruct to generate multi-threading safe code


Last updated: 2026-02-13
