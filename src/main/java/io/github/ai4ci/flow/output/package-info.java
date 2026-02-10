/**
 * Writers, exporters and related types for simulation output.
 *
 * <p>Main purpose: provide a small, robust framework for describing, selecting
 * and writing simulation export records. Types in this package are used by the
 * {@code io.github.ai4ci.flow} execution infrastructure to serialise and
 * persist output at well-defined stages of the simulation lifecycle.
 *
 * <h2>Key types and responsibilities</h2>
 * <ul>
 *   <li>{@link io.github.ai4ci.flow.output.Export} - annotation used on record
 *       interfaces to declare the export stage, target filename, size hint,
 *       selector and writer class;</li>
 *   <li>{@link io.github.ai4ci.flow.output.Export.Selector} - functional
 *       interface used to provide a stream of exportable records from an
 *       {@link io.github.ai4ci.abm.Outbreak} instance;</li>
 *   <li>{@link io.github.ai4ci.flow.output.OutputWriter} - base interface for
 *       writer implementations;</li>
 *   <li>{@link io.github.ai4ci.flow.output.CSVWriter} - concrete writer for
 *       comma separated value files;</li>
 *   <li>{@link io.github.ai4ci.flow.output.DuckDBWriter} - concrete writer for
 *       DuckDB database files used for larger per‑agent exports;</li>
 *   <li>{@link io.github.ai4ci.flow.output.QueueWriter} - asynchronous buffered
 *       writer to decouple writers from the simulation thread;</li>
 *   <li>{@link io.github.ai4ci.flow.output.SimulationExporter} - coordinates
 *       discovery of annotated record types and invokes selectors and writers
 *       at the configured export stages.</li>
 * </ul>
 *
 * <h2>Usage and integration</h2>
 * <p>Record interfaces are annotated with {@link Export} to declare their target
 * writer and export stage. The {@link Export.Selector} implementation for a
 * record is responsible for extracting the corresponding records from an
 * {@link io.github.ai4ci.abm.Outbreak} model; a selector returns a
 * {@link java.util.stream.Stream} of {@link io.github.ai4ci.flow.output.CSVWriter.Writeable}
 * instances which the configured {@link OutputWriter} consumes.
 *
 * <p>Export writers are designed to be pluggable. The export infrastructure
 * supports both synchronous writers (for small outputs) and asynchronous
 * queue-based writers (for heavy per-agent exports) allowing large-scale
 * simulations to persist outputs without blocking the main simulation loop.
 *
 * <h2>Stages and intended formats</h2>
 * <ul>
 *   <li>{@link Export.Stage#BASELINE} — baseline or pre‑simulation artefacts;
 *       typically small CSV outputs (for example infectivity profiles)</li>
 *   <li>{@link Export.Stage#START} — produced once per execution at startup;
 *       commonly configuration snapshots and demographic exports (CSV or
 *       DuckDB)</li>
 *   <li>{@link Export.Stage#UPDATE} — produced repeatedly during the run;
 *       commonly per‑agent or per‑time state exports and often written to
 *       DuckDB for efficiency</li>
 *   <li>{@link Export.Stage#FINISH} — final aggregated results exported once
 *       at the end of a run</li>
 * </ul>
 *
 * <h2>Downstream consumers</h2>
 * <p>Writers in this package are consumed by analytic pipelines, notebooks and
 * external tools that read CSV or DuckDB files for visualisation and further
 * analysis. The exported artefacts are intentionally self‑describing so that
 * downstream tools can join per‑agent and per‑run tables using shared ids.
 *
 * @author Rob Challen
 */
package io.github.ai4ci.flow.output;