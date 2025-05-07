package io.github.ai4ci.output;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import io.github.ai4ci.Export;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.flow.CSVWriter;

/**
 * This manages the various files that are being exported to so and their
 * respective threads that the outbreak simulation doesn't have to. It has a 
 * single method, export(outbreak, stage), that is called at the different 
 * lifecycle   
 */
public class StateExporter implements Closeable {
	
	static Logger log = LoggerFactory.getLogger(StateExporter.class);
	
	Path directory;
	List<ExportSelector<?>> stepWriters = new ArrayList<>();
	List<OutbreakConfigurationJson> outbreakCfg = new ArrayList<>();
	
	public static StateExporter defaultExporter(String path) {
		 return StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve(path),
				ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState())),
				ExportSelector.ofMany(ImmutablePersonStateCSV.class, o -> o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV))
		);
	}
	
	private StateExporter() {}
	
	public static StateExporter of(Path directory, ExportSelector<?>... config) {
		return of(directory,Arrays.asList(config));
	}
	
	public static StateExporter of(Path directory, List<ExportSelector<?>> config) {
		StateExporter out = new StateExporter();
		out.directory = directory;
		out.stepWriters = config;
		out.stepWriters.forEach(e -> e.finishSetup(directory));
		return out;
	}
	
	//TODO: make this a runnable thread pool with a buffer 
	public static class ExportSelector<X extends CSVWriter.Writeable> {
		Export.Stage stage;
		Class<X> type;
		Function<Outbreak,Stream<X>> selector;
		String filename;
		int size;
		CSVWriter<X> writer;
		
		private ExportSelector(
				Class<X> type,
				Function<Outbreak,Stream<X>> selector
			) {
			Export.Stage stage = type.getAnnotation(Export.class).stage();
			String file = type.getAnnotation(Export.class).value();
			int size = type.getAnnotation(Export.class).size();
			this.type = type; this.selector = selector; this.filename = file; this.size = size; this.stage = stage;
			
		}
		
		private void finishSetup(Path directory) {
			try {
				this.writer = CSVWriter
						.of(type, directory.resolve(filename).toFile(), size);
			} catch (IOException e) {
				throw new RuntimeException("Couldn't setup exporter: "+directory.resolve(filename));
			}
		}
		
		public static <X extends CSVWriter.Writeable> ExportSelector<X> ofMany(Class<X> type, Function<Outbreak,Stream<X>> selector) {
			return new  ExportSelector<X>(type,selector);
		}
		public static <X extends CSVWriter.Writeable> ExportSelector<X> ofOne(Class<X> type, Function<Outbreak,X> selector) {
			return new  ExportSelector<X>(type,o -> Stream.of(selector.apply(o)));
		}
		public void close() {
			writer.close();
		}
		public void flush() {
			writer.flush();
		}
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
			.filter(s -> s.stage.equals(stage))
			.forEach(sel -> {
				ExportSelector<X> sel2 = (ExportSelector<X>) sel; 
				if (sel2.writer != null)
					sel2.writer.export(
							// this should execute in the forkjoinpool
							// it include the CSV mapper
							sel2.selector.apply(outbreak).parallel()
					);
			});
		return outbreak;
	}

	public void close() {
		this.stepWriters.forEach(e -> e.close());
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
		stepWriters.forEach(w -> w.flush());
		this.outbreakCfg.add(CSVMapper.INSTANCE.toJson(outbreak));
	}
	
	public void writeInputConfiguration(ExperimentConfiguration tmp) {
		try {
			tmp.writeConfig(directory);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
