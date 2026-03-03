# Copilot Instructions for AI Coding Agents

## Project Overview
This package provides visualization and analysis tools for outputs from the JPanSim2 agent-based model. It is structured as an R package with reproducible workflows and robust data handling.

## Architecture & Key Components
- `R/state.R`: Manages global state, especially the results directory, using an environment object. Interactive directory selection uses `tcltk`.
- `R/experiment-details.R`: Reads and parses experiment settings from JSON files in the results directory. Uses helpers like `try_mutate` and `urn_groups`.
- `R/utils.R`: Utility functions, including `try_mutate` (safe mutate for data frames) and `urn_groups` (URN parsing for grouping).
- `R/import-standalone-cache.R`: Standalone caching layer for expensive computations, sourced from an external repo (see file header).
- `data-raw/`: Scripts for preparing datasets for inclusion in the package.

## Developer Workflows
- Build: `devtools::build()`
- Test: `devtools::test()`
- Documentation: `roxygen2::roxygenise()`
- Set results directory: `set_results_directory()` and `get_results_dir()`

## Project-Specific Patterns
- State is kept in the `state` environment (see `R/state.R`). Avoid global variables elsewhere.
- Use `try_mutate()` for robust data frame mutation, especially when reading/parsing JSON outputs.
- Use `urn_groups()` to extract group identifiers from URNs in experiment metadata.
- Use cache helpers in `R/import-standalone-cache.R` for expensive or repeated computations.
- Use `snake_case` for files and functions.
- Ensure documentation sections include `@param`, `@results` and `@examples` sections, and avoid `\dontrun` sections in examples.
- Tag user facing functions with `@export`. Tag utility functions with `@keywords internal`.

## Integration & Dependencies
- Experiment settings are read from JSON files named `result-settings.json` in the results directory.
- Key packages: `dplyr`, `fs`, `jsonlite`, `tcltk`, `rappdirs` (see `DESCRIPTION`).
- Some files (e.g., `import-standalone-cache.R`) are generated from external sources—do not edit by hand.

## Known Issues & Gotchas
- Functions using `tcltk` require an interactive session and may not work in headless environments.
- Logic in `R/experiment-details.R` is complex and may have edge cases—review carefully before modifying.

## Examples
- See `R/state.R` for directory management.
- See `R/utils.R` for robust data frame mutation and URN parsing.

---

If you add new patterns or workflows, update this file with concrete examples from the codebase.