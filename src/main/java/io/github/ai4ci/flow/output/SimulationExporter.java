package io.github.ai4ci.flow.output;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.OutbreakConfigurationJson;

/**
 * This manages the various files that are being exported to so and their
 * respective threads that the outbreak simulation doesn't have to. It has a
 * single method, export(outbreak, stage), that is called at the different
 * points in the lifecycle. This class is responsible for firstly setting up
 * files and writer threads as specified by the list of ExportSelectors provided
 * or coordinating mapping an outbreak to streams of the items to be output,
 */
public class SimulationExporter implements Closeable {

	// static Logger log = LoggerFactory.getLogger(SimulationExporter.class);

	/**
	 * Factory method to create a SimulationExporter with the given directory and
	 * configuration. The configuration is provided as a varargs array of
	 * ExportSelector objects, which specify what data to export at each stage of
	 * the simulation. The method initializes the SimulationExporter instance,
	 * sets up the export writers based on the provided configuration, and
	 * returns the initialized SimulationExporter.
	 *
	 * @param directory the directory where export files will be written
	 * @param config    varargs array of ExportSelector objects specifying export
	 *                  configuration
	 * @return an initialized SimulationExporter instance ready for use
	 */
	public static SimulationExporter of(
			Path directory, ExportSelector<?>... config
	) {
		return of(directory, Arrays.asList(config));
	}

	/**
	 * Factory method to create a SimulationExporter with the given directory and
	 * configuration. The configuration is provided as a list of ExportSelector
	 * objects, which specify what data to export at each stage of the
	 * simulation. The method initializes the SimulationExporter instance, sets
	 * up the export writers based on the provided configuration, and returns the
	 * initialized SimulationExporter.
	 *
	 * @param directory the directory where export files will be written
	 * @param config    list of ExportSelector objects specifying export
	 *                  configuration
	 * @return an initialized SimulationExporter instance ready for use
	 */
	public static SimulationExporter of(
			Path directory, List<ExportSelector<?>> config
	) {
		var out = new SimulationExporter();
		out.directory = directory;
		out.stepWriters = config;
		out.stepWriters.forEach(e -> e.finishSetup(directory));
		return out;
	}

	Path directory;
	List<ExportSelector<?>> stepWriters = new ArrayList<>();
	List<OutbreakConfigurationJson> outbreakCfg = new ArrayList<>();

	private SimulationExporter() {}

	/**
	 * This method checks if all export writers are currently in a waiting state.
	 * It iterates through the list of ExportSelector objects (stepWriters) and
	 * checks if the writer associated with each ExportSelector is waiting. If
	 * all writers are waiting, the method returns true; otherwise, it returns
	 * false. This can be used to determine if all export threads are currently
	 * paused or waiting for data to export.
	 *
	 * @return true if all export writers are waiting, false otherwise
	 */
	public boolean allWaiting() {
		return this.stepWriters.stream()
			.allMatch(
				es -> es.getWriter()
					.isWaiting()
			);
	}

	@Override
	public void close() {
		this.stepWriters.forEach(
			e -> e.getWriter()
				.close()
		);
		try {
			Files.deleteIfExists(this.directory.resolve("result-settings.json"));
			var om = new ObjectMapper();
			om.enable(SerializationFeature.INDENT_OUTPUT);
			om.registerModules(new GuavaModule());
			om.setSerializationInclusion(Include.NON_NULL);
			om.setSerializationInclusion(Include.NON_EMPTY);
			om.writeValue(
				this.directory.resolve("result-settings.json")
					.toFile(),
				this.outbreakCfg
			);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * This method exports data from the given outbreak at a specific stage of
	 * the simulation. It iterates through the list of ExportSelector objects
	 * (stepWriters) and filters them based on the specified stage. For each
	 * ExportSelector that matches the stage, it retrieves the associated writer
	 * and selector function. If the writer is not null, it applies the selector
	 * function to the outbreak to obtain a stream of data items to export. The
	 * export operation is executed in parallel using a parallel stream, allowing
	 * for concurrent processing of the data items. Finally, the method returns
	 * the outbreak object.
	 *
	 * @param <X>      the type of data items to be exported, which must
	 *                 implement the CSVWriter.Writeable interface
	 * @param stage    the stage of the simulation at which to export data
	 * @param outbreak the outbreak object containing data to be exported
	 * @return the same outbreak object after exporting data
	 */
	@SuppressWarnings("unchecked")
	public <X extends CSVWriter.Writeable> Outbreak export(
			Export.Stage stage, Outbreak outbreak
	) {
		this.stepWriters.stream()
			.filter(
				s -> s.getStage()
					.equals(stage)
			)
			.forEach(sel -> {
				var sel2 = (ExportSelector<X>) sel;
				if (sel2.getWriter() != null) {
					sel2.getWriter()
						.export(
							(Stream<X>) sel2.selector(outbreak)
								.parallel()
					// this should execute export in the forkjoinpool
					// which is useful because it include the
					// mapping from the X (csv export object) to the
					// binary representation (e.g. String for CSV)
						);
				}
			});
		return outbreak;
	}

//	public void purgeAll() {
//		this.stepWriters.forEach(e -> e.purge());
//	}

	/**
	 * This method exports data from the given outbreak at multiple stages of the
	 * simulation. It checks the current state of the outbreak and determines
	 * which stages to export based on specific conditions. If the outbreak is at
	 * the baseline stage (experimentReplica, modelReplica, and time are all 0),
	 * it exports data for the BASELINE stage. If the outbreak is at the start
	 * stage (time is 0), it exports data for the START stage. Finally, it always
	 * exports data for the UPDATE stage. The method returns the outbreak object
	 * after performing the necessary exports.
	 *
	 * @param <X>      the type of data items to be exported, which must
	 *                 implement the CSVWriter.Writeable interface
	 * @param outbreak the outbreak object containing data to be exported
	 * @return the same outbreak object after exporting data
	 */
	public <X extends CSVWriter.Writeable> Outbreak export(Outbreak outbreak) {
		if (outbreak.getExperimentReplica() == 0
				&& outbreak.getModelReplica() == 0 && outbreak.getCurrentState()
					.getTime() == 0) {
			this.export(Export.Stage.BASELINE, outbreak);
		}
		if (outbreak.getCurrentState()
			.getTime() == 0) {
			this.export(Export.Stage.START, outbreak);
		}
		this.export(Export.Stage.UPDATE, outbreak);
		return outbreak;
	}

	/**
	 * This method finalizes the export process for a given outbreak. It first
	 * calls the export method with the FINISH stage to export any remaining data
	 * for the outbreak. Then, it flushes all the writers associated with the
	 * ExportSelector objects in the stepWriters list to ensure that all data is
	 * written to the output files. Finally, it adds the configuration of the
	 * outbreak (converted to JSON format using CSVMapper) to the outbreakCfg
	 * list for later use or reference.
	 *
	 * @param outbreak the outbreak object containing data to be finalized and
	 *                 exported
	 */
	public void finalise(Outbreak outbreak) {
		this.export(Export.Stage.FINISH, outbreak);
		this.stepWriters.forEach(
			w -> w.getWriter()
				.flush()
		);
		this.outbreakCfg.add(CSVMapper.INSTANCE.toJson(outbreak));
	}

	/**
	 * This method finalizes the export process for all outbreaks. It iterates
	 * through the list of ExportSelector objects (stepWriters) and calls the
	 * close method on each associated writer to ensure that all resources are
	 * released and any remaining data is flushed to the output files. This is
	 * typically called at the end of the simulation to clean up resources and
	 * finalize all exports.
	 */
	public void finaliseAll() {
		this.stepWriters.forEach(
			w -> w.getWriter()
				.close()
		);
	}

	/**
	 * This method waits for all export writer threads to complete their
	 * execution. It iterates through the list of ExportSelector objects
	 * (stepWriters) and calls the join method on each associated writer thread.
	 * This ensures that the main thread will wait until all export writer
	 * threads have finished their tasks before proceeding. If any thread is
	 * interrupted while waiting, an InterruptedException is thrown.
	 *
	 * @throws InterruptedException if any thread is interrupted while waiting
	 *                              for the export writer threads to complete
	 */
	public void joinAll() throws InterruptedException {
		for (ExportSelector<?> sw : this.stepWriters) {
			sw.getWriter()
				.join();
		}
	}

	/**
	 * This method generates a report summarizing the status of all export
	 * writers. It iterates through the list of ExportSelector objects
	 * (stepWriters) and retrieves the report from each associated writer. The
	 * individual reports are then concatenated into a single string, separated
	 * by semicolons, and returned as the final report. This can be used to
	 * provide an overview of the export process and the status of each writer.
	 *
	 * @return a string report summarizing the status of all export writers
	 */
	public String report() {
		return this.stepWriters.stream()
			.map(
				s -> s.getWriter()
					.report()
			)
			.collect(Collectors.joining("; "));
	}

	/**
	 * This method writes the input configuration of the experiment to a file in
	 * the specified directory. It takes an ExperimentConfiguration object as
	 * input and calls its writeConfig method, passing the directory path where
	 * the configuration should be saved. If an IOException occurs during the
	 * writing process, it is caught and rethrown as a RuntimeException.
	 *
	 * @param tmp the ExperimentConfiguration object containing the configuration
	 *            to be written to a file
	 */
	public void writeInputConfiguration(ExperimentConfiguration tmp) {
		try {
			tmp.writeConfig(this.directory, true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
