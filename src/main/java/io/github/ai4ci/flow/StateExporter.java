package io.github.ai4ci.flow;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import io.github.ai4ci.Export;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.flow.CSVWriter.Writeable;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.OutbreakConfigurationJson;

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
	
	public static class ExportSelector<X extends CSVWriter.Writeable> {
		Export.Stage stage;
		Class<X> type;
		String filename;
		int size;
		CSVWriter<X> writer;
		Function<Outbreak, Stream<? extends Writeable>> selector;
		
		private ExportSelector(
				Class<X> type
			) {
			Export.Stage stage = type.getAnnotation(Export.class).stage();
			String file = type.getAnnotation(Export.class).value();
			int size = type.getAnnotation(Export.class).size();
			try {
				this.selector = type.getAnnotation(Export.class).selector().getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.type = type;
			
			
			this.filename = file; this.size = size; this.stage = stage;
			
		}
		
		private void finishSetup(Path directory) {
			try {
				this.writer = CSVWriter
						.of(type, directory.resolve(filename).toFile(), size);
			} catch (IOException e) {
				throw new RuntimeException("Couldn't setup exporter: "+directory.resolve(filename));
			}
		}
		
		public static <X extends CSVWriter.Writeable> ExportSelector<X> of(Class<X> type) {
			return new  ExportSelector<X>(type);
		}
		public void close() {
			writer.close();
		}
		public void flush() {
			writer.flush();
		}
//		public void purge() {
//			writer.purge();
//		}

		public boolean isWaiting() {
			return writer.isWaiting();
		}

		public void join() throws InterruptedException {
			writer.join();
		}

		public String report() {
			return writer.report();
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
							(Stream<X>) // this should execute in the forkjoinpool
							// it include the CSV mapper
							sel2.selector.apply(outbreak).parallel()
					);
			});
		return outbreak;
	}
	
	public boolean allWaiting() {
		return this.stepWriters.stream().allMatch(es -> es.isWaiting());
	}

//	public void purgeAll() {
//		this.stepWriters.forEach(e -> e.purge());
//	}
	
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

	public void joinAll() throws InterruptedException {
		for (ExportSelector<?> sw : this.stepWriters) {
			sw.join();
		}
	}
	
	protected String report() {
		return this.stepWriters.stream().map(s -> s.report()).collect(Collectors.joining("; "));
	}
	
}
