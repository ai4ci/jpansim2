# ADR-3: Output/Export Strategy — Pre-mutation Snapshots with Async Writers

**Status**: Accepted

**Date**: 2026-05-02

## Context

Simulation output must be captured continuously during execution without blocking the simulation loop. The output volume is large: per-timestep lineage data for all individuals, contact network snapshots, behaviour state counts, and test results. A synchronous approach would stall the simulation.

## Decision

The export pipeline coordinates pre-mutation snapshot exports at defined lifecycle stages:

1. **Export stages**: `START` (simulation start), `BASELINE` (post-baseline, pre-simulation), `UPDATE` (before daily state mutation), `FINISH` (simulation end). Exports occur *before* state mutation to capture a consistent snapshot.

2. **SimulationExporter** — Orchestrates exports by filtering exporters by stage, mapping outbreak data to `CSVWriter.Writeable` objects via parallel streams, and delegating to writer threads.

3. **Writer types**:
   - `CSVWriter` — writes records to CSV files
   - `DuckDBWriter` — writes records to DuckDB columnar database files
   - `QueueWriter` — buffered async writer for throughput

4. **Output record definitions** — Each record type defines specific CSV/DuckDB serialisation views via Immutables `@Value.Immutable` interfaces with specific annotations for stage and writer targeting:
   - `OutbreakCSV` — daily outbreak summary
   - `LineListDuckDB` — per-person daily lineage
   - `ContactDuckDB` — contact network snapshots
   - `OutbreakHistoryCSV` — test-positivity matrix
   - `OutbreakFinalStateCSV` — end-of-simulation summary

5. **Finalisation** — `SimulationExporter.finalise(Outbreak)` exports the `FINISH` stage, flushes all writers, and records the outbreak configuration as JSON. `finaliseAll()` closes all writers and writes a `result-settings.json` summary.

## Rationale

- Pre-mutation exports ensure consistency: the exported state matches the state that was used for computation on that day.
- Async writers prevent I/O from becoming a bottleneck in the simulation loop.
- DuckDB output provides columnar storage suitable for post-hoc analysis with SQL and R, matching the analysis pipeline.
- MapStruct (`CSVMapper`) generates the state-to-record conversion code at compile time.

## Consequences

- The parallel stream on `export()` (line 155-156 of `SimulationExporter`) uses the ForkJoinPool and shares threads with other parallel operations. There is no explicit pool isolation.
- Output record definitions duplicate field names across CSV and DuckDB targets — for example, `OutbreakCSV.summaryFileName` and `OutbreakCSV` fields vs. equivalent `LineListDuckDB` fields.
- The `OutbreakHistoryCSV` has TODOs about testing reporting delay, death/admission delays, and weekend effects that are not yet modelled.
- There is no compression for large output files — CSV records for the full lineage can grow to many gigabytes.

## Technical Debt

- No compression for output files is mentioned as a potential improvement.
- The output record definitions could benefit from a more abstract schema layer to deduplicate field definitions.
- The `SimulationExporter` does not validate that all writers will fit on disk before starting.
