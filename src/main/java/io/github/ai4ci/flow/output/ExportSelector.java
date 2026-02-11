package io.github.ai4ci.flow.output;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.OutputWriter.Writeable;

/**
 * This is the first stage of configuring a destination for export based on the
 * specification of the data and the mappings to get the data from the outbreak
 * class. This also defines the type of output (e.g. CSV) and initialisation of
 * the output file in an output directory as appropriate.
 *
 * @param <X> the type of data to be exported, which must implement
 *            CSVWriter.Writeable
 */
public class ExportSelector<X extends CSVWriter.Writeable> {
	/**
	 * Factory method to create an ExportSelector for a given export type. This
	 * method takes a class type that extends CSVWriter.Writeable and is
	 * annotated with the Export annotation. It initializes the ExportSelector
	 * with the export configuration specified in the annotation, such as the
	 * export stage, filename, buffer size, and selector function. The created
	 * ExportSelector can then be used to set up the output writer and manage the
	 * export process for the specified data type.
	 *
	 * @param <X>  the type of data to be exported, which must implement
	 *             CSVWriter.Writeable
	 * @param type the class type of the export data, which should be annotated
	 *             with Export to provide configuration details for the export
	 *             process
	 * @throws RuntimeException if the provided class type does not have the
	 *                          required Export annotation or if there are issues
	 *                          with instantiating the selector function
	 *
	 * @return an instance of ExportSelector configured for the specified export
	 *         type, ready to be set up with an output writer and used for
	 *         exporting data during the simulation
	 *
	 */
	public static <X extends CSVWriter.Writeable> ExportSelector<X> of(
			Class<X> type
	) {
		return new ExportSelector<>(type);
	}

	private Export.Stage stage;
	private Class<X> type;
	private String filename;

	private int size;
	// TODO: Abstract CSVWriter and implement DuckDBWriter using appendable
	// interface
	// https://duckdb.org/docs/stable/clients/java.html#appender
	// This will need to be configurable in the Export annotation.
	private OutputWriter<X> writer;

	private Function<Outbreak, Stream<? extends Writeable>> selector;

	private ExportSelector(Class<X> type) {
		Export.Stage stage = type.getAnnotation(Export.class).stage();
		String file = type.getAnnotation(Export.class).value();
		int size = type.getAnnotation(Export.class).size();
		try {
			this.selector = type.getAnnotation(Export.class).selector()
					.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.type = type;
		this.filename = file;
		this.size = size;
		this.stage = stage;
	}

	/**
	 * Initializes the output writer for the specified export type and sets up
	 * the output file in the given directory. This method is called after the
	 * ExportSelector is created to prepare the output destination for exporting
	 * data. The writer is instantiated based on the Export annotation's writer
	 * class, and the setup method is called to configure it with the export
	 * type, output file path, and buffer size.
	 *
	 * @param directory the path to the output directory where the export file
	 *                  will be created and configured
	 * @throws RuntimeException if there is an error during writer instantiation
	 *                          or setup, such as issues with file creation or
	 *                          reflection errors when accessing the writer class
	 *
	 */
	@SuppressWarnings("unchecked")
	public void finishSetup(Path directory) {
		try {
			this.writer = this.type.getAnnotation(Export.class).writer()
					.getDeclaredConstructor().newInstance();
			this.writer.setup(
					this.type, directory.resolve(this.filename).toFile(), this.size
			);
		} catch (IOException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(
					"Couldn't setup exporter: " + directory.resolve(this.filename), e
			);
		}
	}

	/**
	 * The stage of the outbreak simulation at which this export should be
	 * performed. This is determined by the Export annotation on the data type
	 * and is used to manage the timing of exports during the simulation
	 * execution. The stage indicates when the selector function should be
	 * applied to extract data from the outbreak and write it to the output file,
	 * allowing for proper sequencing of exports based on the simulation's
	 * lifecycle.
	 *
	 * @return the Export.Stage value indicating when this export should be
	 *         performed
	 */
	public Export.Stage getStage() { return this.stage; }

	/**
	 * The OutputWriter instance responsible for writing the exported data to the
	 * output file. This writer is configured based on the Export annotation's
	 * writer class and is set up with the appropriate output file path and
	 * buffer size. The getWriter method provides access to this writer for use
	 * during the export process, allowing the ExportSelector to manage the flow
	 * of data from the outbreak simulation to the output destination through the
	 * configured writer.
	 *
	 * @return the OutputWriter instance that has been initialized and set up for
	 *         writing exported data of type X to the output file
	 */
	public OutputWriter<X> getWriter() { return this.writer; }

	/**
	 * Applies the selector function to the given outbreak to retrieve a stream
	 * of data records that are ready to be exported. The selector function is
	 * defined in the Export annotation and is responsible for extracting the
	 * relevant data from the outbreak simulation and transforming it into a
	 * stream of Writeable records that can be written to the output file by the
	 * configured writer. This method is called during the export process to
	 * obtain the data to be exported for each simulation step, allowing the
	 * ExportSelector to manage the flow of data from the outbreak to the output
	 * destination.
	 *
	 * @param outbreak the outbreak simulation instance from which to extract the
	 *                 data for export; the selector function will use this
	 *                 outbreak to generate the stream of records to be exported
	 * @throws RuntimeException if there is an error during the application of
	 *                          the selector function, such as issues with data
	 *                          extraction from the outbreak or problems with the
	 *                          selector function's logic
	 *
	 * @return a stream of Writeable records generated by applying the selector
	 *         function to the given outbreak, which can then be written to the
	 *         output file by the configured writer
	 *
	 */
	public Stream<? extends Writeable> selector(Outbreak outbreak) {
		return this.selector.apply(outbreak);
	}
}