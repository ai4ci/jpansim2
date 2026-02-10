package io.github.ai4ci.flow.output;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.ai4ci.abm.Outbreak;

/**
 * Annotation to mark data classes for export during simulation execution.
 *
 * <p>Classes annotated with {@code @Export} are recognized by the simulation
 * exporter and are processed according to the specified parameters. The
 * annotation provides metadata about the export stage, output file name,
 * expected record size, selector function for data extraction, and the writer
 * class responsible for handling the export.
 *
 * <p>By using this annotation, developers can easily designate which data classes
 * should be exported at different stages of the simulation, and how they should
 * be processed and written to output files.
 *
 * @author Rob Challen
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Export {

	/**
	 * Enumeration of export stages during simulation execution.
	 *
	 * <p>Defines the stages at which data can be exported: BASELINE for initial
	 * model parameters, START for simulation start, UPDATE for per‑step updates,
	 * and FINISH for final outputs after simulation completion.
	 */
	public static enum Stage {
		/** Export data at the baseline stage, typically for initial model parameters. */
		BASELINE, 
		/** Export data at the start of the simulation, typically for initial conditions. */
		START, 
		/** Export data during simulation updates, typically for time‑indexed records. */
		UPDATE, 
		/** Export data at the finish of the simulation, typically for final outputs. */
		FINISH
	}
	
	/**
	 * Functional interface for selecting data to export from an Outbreak simulation.
	 *
	 * <p>Implementations of this interface define how to extract and transform
	 * data from the Outbreak object into a stream of records that can be written
	 * to output files. The selector function is specified in the {@code @Export}
	 * annotation and is used by the exporter to retrieve the relevant data for
	 * export at the appropriate stage of the simulation.
	 */
	public static interface Selector extends Function<Outbreak,Stream<? extends CSVWriter.Writeable>> {} 

	/**
	 * Get the output file name for the export.
	 *
	 * @return the name of the output file to which the exported data will be written
	 * relative to the output directory; should include the appropriate file extension.
	 */
	String value();
	
	/**
	 * Get the export stage at which the data should be exported.
	 *
	 * @return the stage of the simulation (BASELINE, START, UPDATE, FINISH) at
	 * which the data should be exported
	 */
	Stage stage();
	
	/**
	 * Get the indicative cache size of the records to be exported.
	 *
	 * @return the expected number of fields in each record; used for validation
	 * and to ensure consistency with the writer's output format
	 */
	int size();
	
	/**
	 * Get the selector class  responsible for extracting data from the Outbreak simulation for export.
	 * The selector class implements the Export.Selector interface and defines the logic for
	 * transforming the Outbreak object into a stream of records that can be written to the output file.
	 * 
	 * @return 	the class that implements the Selector interface and provides the logic
	 * for extracting data from the Outbreak simulation for export.
	 */
	Class<? extends Selector> selector();
	
	/**
	 * Get the writer class responsible for handling the export of the data to the specified output file.
	 * @return the class that implements the OutputWriter interface and defines how
	 * to write the exported data to the output file in the desired format (e.g., CSV, DuckDB).
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends OutputWriter> writer();
	
}
