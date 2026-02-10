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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * or coordinating mapping 
 * an outbreak to streams of the items to be output,
 */
public class SimulationExporter implements Closeable {
	
	static Logger log = LoggerFactory.getLogger(SimulationExporter.class);
	
	Path directory;
	List<ExportSelector<?>> stepWriters = new ArrayList<>();
	List<OutbreakConfigurationJson> outbreakCfg = new ArrayList<>();
	
	private SimulationExporter() {}
	
	public static SimulationExporter of(Path directory, ExportSelector<?>... config) {
		return of(directory,Arrays.asList(config));
	}
	
	public static SimulationExporter of(Path directory, List<ExportSelector<?>> config) {
		SimulationExporter out = new SimulationExporter();
		out.directory = directory;
		out.stepWriters = config;
		out.stepWriters.forEach(e -> e.finishSetup(directory));
		return out;
	}
	
	public <X extends CSVWriter.Writeable> Outbreak export(Outbreak outbreak) {
		if (outbreak.getExperimentReplica() == 0 && 
				outbreak.getModelReplica() == 0 &&
				outbreak.getCurrentState().getTime() == 0
		) {
			export(Export.Stage.BASELINE, outbreak);
		}
		if (outbreak.getCurrentState().getTime() == 0) export(Export.Stage.START,outbreak);
		export(Export.Stage.UPDATE, outbreak);
		return outbreak;
	}
	
	@SuppressWarnings("unchecked")
	public <X extends CSVWriter.Writeable> Outbreak export(Export.Stage stage, Outbreak outbreak) {
		stepWriters
			.stream()
			.filter(s -> s.getStage().equals(stage))
			.forEach(sel -> {
				ExportSelector<X> sel2 = (ExportSelector<X>) sel; 
				if (sel2.getWriter() != null)
					sel2.getWriter().export(
							(Stream<X>) sel2.selector(outbreak).parallel()
							// this should execute export in the forkjoinpool
							// which is useful because it include the 
							// mapping from the X (csv export object) to the 
							// binary representation (e.g. String for CSV)
					);
			});
		return outbreak;
	}
	
	public boolean allWaiting() {
		return this.stepWriters.stream().allMatch(es -> es.getWriter().isWaiting());
	}

//	public void purgeAll() {
//		this.stepWriters.forEach(e -> e.purge());
//	}
	
	public void close() {
		this.stepWriters.forEach(e -> e.getWriter().close());
		try {
			Files.deleteIfExists(directory.resolve("result-settings.json"));
			ObjectMapper om = new ObjectMapper();
			om.enable(SerializationFeature.INDENT_OUTPUT);
			om.registerModules(new GuavaModule());
			om.setSerializationInclusion(Include.NON_NULL);
			om.setSerializationInclusion(Include.NON_EMPTY);
			om.writeValue(directory.resolve("result-settings.json").toFile(), this.outbreakCfg);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	public void finalise(Outbreak outbreak) {
		export(Export.Stage.FINISH, outbreak);
		stepWriters.forEach(w -> w.getWriter().flush());
		this.outbreakCfg.add(CSVMapper.INSTANCE.toJson(outbreak));
	}
	
	public void finaliseAll() {
		stepWriters.forEach(w -> w.getWriter().close());
	}
	
	public void writeInputConfiguration(ExperimentConfiguration tmp) {
		try {
			tmp.writeConfig(directory);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void joinAll() throws InterruptedException {
		for (ExportSelector<?> sw : this.stepWriters) {
			sw.getWriter().join();
		}
	}
	
	public String report() {
		return this.stepWriters.stream().map(s -> s.getWriter().report()).collect(Collectors.joining("; "));
	}
	
}
