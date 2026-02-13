Configuration for JPanSim2

This page documents the JSON-based experiment configuration format used by
JPanSim2. It explains the main sections, how to specify distributions and
functions, sensible defaults for setup and execution, and how to produce
side‑by‑side comparisons of multiple configurations.

This document references the generated schema found at
`target/generated-config/examples/schema.json` and a set of example
configurations (if present) at `target/site/apidocs/examples`.

Main contract
- Inputs: a JSON file describing an experiment with top-level keys
  `batchConfig` and `executionConfig`, plus optional `setupConfig` and
  `facets`.
- Outputs: validated experiment objects used by JPanSim2 and any derived
  reports.
- Error modes: missing required fields or invalid types as described in
  the JSON schema. Behaviour on invalid input depends on the runner; by
  default validation errors prevent execution.

High-level structure
- `batchConfig` — controls overall batch execution parameters such as
  `simulationDuration`, `exporters` and `urnBase`.
- `executionConfig` — the core simulation configuration: epidemiological
  model choices, distributions for stochastic parameters, kernels and
  in‑host models.
- `setupConfig` — optional per‑run setup facets.
- `facets` — additional execution facets for advanced options.

Specifying distributions

Distributions are used throughout the configuration for quantities such
as `r0`, incubation periods, infectious durations and so on. The schema
supports a number of concrete distribution objects; the generated schema
lists these under `$defs/Distribution` and their concrete variants such
as `ImmutableNormalDistribution`, `ImmutableGammaDistribution`,
`ImmutablePointDistribution`, `ImmutableEmpiricalDistribution` and
others.

General rules

- Any field typed as a `Distribution` may accept one of the concrete
  distribution objects. Use the object form, for example:

  - Normal distribution (mean and standard deviation):

    ```json
    {
      "mean": 5.0,
      "sd": 1.2
    }
    ```

  - Point distribution (fixed value):

    ```json
    {
      "mean": 3.0
    }
    ```

  - Uniform distribution:

    ```json
    {
      "min": 0.0,
      "max": 10.0
    }
    ```

  - Empirical distribution (explicit points and cumulative probability):

    ```json
    {
      "x": [0.0, 1.0, 2.0],
      "cumulativeProbability": [0.1, 0.6, 1.0]
    }
    ```

- See the concrete type definitions in
  `target/generated-config/examples/schema.json` under `$defs` for the
  exact property names the generator expects. For example the schema
  defines properties `mean` and `sd` for the Normal and Gamma types.

Link functions and kernels

- Kernel-like configuration (for `riskModelContactsKernel`,
  `riskModelSymptomKernel` etc.) accepts types defined under
  `$defs/KernelFunction`, which include `ImmutableEmpiricalKernel`,
  `ImmutableDiscreteFunction` and `ImmutableGaussianKernel`.
- A kernel object generally expresses a discrete or continuous kernel as
  arrays of points, sums, maxima/minima and optional link functions.
- Link functions are brief enumerated values under `$defs/LinkFunction`
  (for example `NONE`, `LOG`, `LOGIT`). Use these where `link` is
  permitted in the definition.

Functions and empirical forms

- `SimpleFunction` fields accept either an empirical function (x-y
  pairs), a fixed-value function or a mathematical function (the
  schema calls this `ImmutableMathematicalFunction`). Use empirical
  functions to represent piecewise behaviour or measured data; use fixed
  values for constants.
- Empirical function example:

  ```json
  {
    "interpolationType": "LINEAR",
    "x": [0, 7, 14],
    "y": [0.1, 0.5, 0.2]
  }
  ```

Defaults for setup and execution

The configuration schema provides many optional fields. The runtime
applies sensible defaults when fields are omitted. Typical defaults used
by JPanSim2 are:

- `simulationDuration`: default is often set to a reasonable period such
  as 365 days unless the batch or execution config overrides it.
- `r0`: if omitted, default model parameters determine baseline
  transmissibility; set explicitly for reproducibility.
- `exporters`: empty or a minimal set such as `SUMMARY` is used by
  default; include additional exporters (for example `LINELIST`) as
  needed.

Always consult the code that parses the configuration for the exact
behaviour. The generated schema is authoritative for allowed shapes and
property names: `target/generated-config/examples/schema.json`.

Side-by-side comparisons of configurations

To produce side-by-side comparison runs where multiple configurations
are executed and the outputs compared (for example two variants of an
intervention), prefer one of two approaches:

1. Multiple experiment files and a comparison driver

   - Create separate experiment JSON files, for example
     `experiment-a.json` and `experiment-b.json`.
   - Use a small wrapper script or a CI job to run the runner twice and
     store outputs in separate folders. Compare results with your
     preferred analysis pipeline.

   Example CI snippet:

   ```bash
   mvn -Dconfig=experiment-a.json -DskipTests process-classes package exec:java
   cp -r target/output target/output-a
   mvn -Dconfig=experiment-b.json -DskipTests process-classes package exec:java
   cp -r target/output target/output-b
   # now run analysis comparing target/output-a and target/output-b
   ```

2. Single experiment file with multiple `setupConfig` entries

   - Use `setupConfig` as an array of setup facets each describing the
     variant. The runner can be instructed to expand these into multiple
     runs; check the runner's CLI or code for support.
   - Add `facets` that alter execution behaviour per variant.

Implementation notes and CI / GitHub Pages recommendations

- The generated examples and schema are created under
  `target/generated-config/examples`. Ensure the build phase that runs
  these generators executes before the `site` or `javadoc` steps in CI.
  For reproducible site publishing run:

  ```bash
  mvn -DskipTests process-classes site
  ```

  This runs the default lifecycle up to `process-classes` and then the
  site lifecycle, ensuring generated examples and any `.nojekyll` copy
  are available under `target/site`.

- Place a `.nojekyll` file at the site publishing root so GitHub Pages
  does not filter files beginning with underscores (for example put it
  at `target/site/.nojekyll` before publishing, or commit it into the
  Pages branch at the root).

References and downstream uses

- JSON schema: `target/generated-config/examples/schema.json` — authoritative
  description of allowed objects and property names.
- Example configurations: `target/site/apidocs/examples` (if present in the
  built site) — concrete config files you can copy and adapt.

Author: Rob Challen
