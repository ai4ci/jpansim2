/**
 * Data export and output generation for simulation results and analytics.
 * 
 * <p>This package provides comprehensive data export capabilities for capturing
 * simulation results at different stages of the execution lifecycle. The exports
 * are organized by {@link io.github.ai4ci.flow.output.Export.Stage} to ensure data
 * is captured at appropriate points during simulation execution.
 * 
 * <h2>Export Stage Organization</h2>
 * <p>Exports are triggered at specific stages of the simulation lifecycle:
 * 
 * <h3>BASELINE Stage</h3>
 * <p>Exports that occur once during model initialization, capturing baseline
 * configuration and static model characteristics:
 * <ul>
 *   <li>{@link io.github.ai4ci.output.InfectivityProfileCSV} - Baseline infectivity profiles
 *       exported to {@code ip.csv}</li>
 * </ul>
 * 
 * <h3>START Stage</h3>
 * <p>Exports that occur at simulation startup, capturing initial state and
 * configuration parameters:
 * <ul>
 *   <li>{@link io.github.ai4ci.output.DebugParametersCSV} - Debug and configuration parameters
 *       exported to {@code debug.csv}</li>
 *   <li>{@link io.github.ai4ci.output.PersonDemographicsCSV} - Initial demographic data
 *       exported to {@code demog.duckdb}</li>
 * </ul>
 * 
 * <h3>UPDATE Stage</h3>
 * <p>Exports that occur during each time step update, capturing dynamic simulation
 * state and epidemiological metrics:
 * <ul>
 *   <li>{@link io.github.ai4ci.output.OutbreakCSV} - Summary statistics exported to {@code summary.csv}</li>
 *   <li>{@link io.github.ai4ci.output.OutbreakHistoryCSV} - Test positivity time series
 *       exported to {@code test-positivity.csv}</li>
 *   <li>{@link io.github.ai4ci.output.OutbreakContactCountCSV} - Contact network statistics
 *       exported to {@code contact-counts.csv}</li>
 *   <li>{@link io.github.ai4ci.output.OutbreakBehaviourCountCSV} - Behavior state counts
 *       exported to {@code behaviours.csv}</li>
 *   <li>{@link io.github.ai4ci.output.LineListCSV} - Case line list data exported to {@code linelist.duckdb}</li>
 *   <li>{@link io.github.ai4ci.output.PersonTestsCSV} - Individual test results
 *       exported to {@code cases.duckdb}</li>
 *   <li>{@link io.github.ai4ci.output.ContactCSV} - Contact tracing data exported to {@code contacts.duckdb}</li>
 * </ul>
 * 
 * <h3>FINISH Stage</h3>
 * <p>Exports that occur at simulation completion, capturing final state and
 * summary results:
 * <ul>
 *   <li>{@link io.github.ai4ci.output.OutbreakFinalStateCSV} - Final simulation state
 *       exported to {@code final-state.csv}</li>
 * </ul>
 * 
 * <h2>Output Formats</h2>
 * <p>The package supports multiple output formats:
 * <ul>
 *   <li><b>CSV files</b>: Standard comma-separated values for interoperability</li>
 *   <li><b>DuckDB databases</b>: High-performance columnar storage for large datasets</li>
 * </ul>
 * 
 * <h2>Writer Implementations</h2>
 * <p>Different writer implementations handle the output generation:
 * <ul>
 *   <li>{@link io.github.ai4ci.flow.output.CSVWriter} - Writes to CSV files</li>
 *   <li>{@link io.github.ai4ci.flow.output.DuckDBWriter} - Writes to DuckDB databases</li>
 * </ul>
 * 
 * <h2>Data Mapping</h2>
 * <p>The {@link io.github.ai4ci.output.CSVMapper} class provides mapping utilities
 * to convert internal model objects to export-friendly CSV representations.
 * 
 * <h2>Common Base Interfaces</h2>
 * <p>Common base interfaces ensure consistency across export types:
 * <ul>
 *   <li>{@link io.github.ai4ci.output.CommonCSV.State} - Base for state-related exports</li>
 *   <li>{@link io.github.ai4ci.flow.output.OutputWriter.Writeable} - Marker interface for exportable data</li>
 * </ul>
 * 
 * <h2>Integration with Flow Package</h2>
 * <p>The output package integrates with the flow execution framework through:
 * <ul>
 *   <li>{@link io.github.ai4ci.flow.output.Export} annotations that declare export timing and parameters</li>
 *   <li>{@link io.github.ai4ci.flow.output.Export.Selector} interfaces that extract data from models</li>
 *   <li>Automatic invocation by {@link io.github.ai4ci.flow.output.SimulationExporter} during execution</li>
 * </ul>
 * 
 * @see io.github.ai4ci.flow.output.Export
 * @see io.github.ai4ci.flow.output.Export.Stage
 * @see io.github.ai4ci.flow.output.CSVWriter
 * @see io.github.ai4ci.output.CSVMapper
 * @see io.github.ai4ci.output.CommonCSV
 */
package io.github.ai4ci.output;