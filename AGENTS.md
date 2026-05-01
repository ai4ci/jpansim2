# JPanSim2 - a large scale disease outbreak simulation

## Background and Purpose

JPanSim2 is an agent-based model (ABM) for simulating pandemic dynamics, including
contact networks, in-host viral dynamics, behavioural responses, and policy
interventions. It supports combinatorial experiment grids and SLURM batch execution.

- `jpansim2-core/src/site/markdown/index.md` — user-facing Maven site documentation
- `README.md` — high-level user-facing documentation (CLI usage, config structure)
- `design/overview.md` — design documentation (work in progress)
- `publications/` — background material on purpose and scope

## Ground rules
* If the user input looks like a bash command they have probably forgotten to escape it and you should stop and ask for guidance.
* If the user input looks incomplete ask the user and wait for the next message.
* If you are uncertain about user intentions clarify with the user and do not make assumptions.
* If the user input is a pasted log file, or similar, analyse it and ask the user for next steps; do not immediately start work.
* Do not revert unexpected code changes. It is likely that the user has changed it deliberately and may have not committed the changes: ask the user what to do.
* Keep any bash commands simple so that they are more likely to be error free. Avoid multi-line bash commands wherever possible.
* Consult the user freely who is expert in this project unless you are specifically instructed to work autonomously.
* You should assume there are mistakes and inconsistencies between code and documentation and that the codebase is not complete.
* You should use paths relative to your home directory in case it changes.

## Development rules
* Before any git commits update relevant design documents with changes.
* It is **mandatory** to use test driven development when implementing new features.
* All tests must be implemented and proven to fail before starting development of a feature.
* All tests must be proven to pass before development of a feature is complete.
* Tests should be directed at the success criteria not the implementation. Failing tests for existing functionality are expected.

## Eclipse MCP Tools available
This project exposes Eclipse IDE capabilities as a locally hosted MCP server, if Eclipse is running. When available prefer using these MCP tools over direct file edits or native bash commands:

- **eclipse-coder** — file editing, refactoring, patching, formatting
- **eclipse-ide** — code analysis, navigation, testing, building, search
- **eclipse-runner** — launch, debug, breakpoints, stepping
- **eclipse-git** — perform git operations via eclipse

Usage:
- always check `eclipse-ide__getCompilationErrors` after code changes
- `eclipse-coder__applyPatch` for multi-hunk edits (more reliable than replaceString)
- `eclipse-coder__replaceString` for single targeted replacements
- `eclipse-ide__getProjectLayout` with `scopePath` and `maxDepth` for large projects

## Working with outputs
* The `scratch` directory is designed for running end to end tests and allows working with simulation output.
* The **r-btw** MCP server allows you to run R code, list variables in connected environment, consult package documentation, build r packages.
* The `analysis` directory contains an R package with functions to load and visualise simulation output.


