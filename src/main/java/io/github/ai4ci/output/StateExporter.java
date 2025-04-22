package io.github.ai4ci.output;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Outbreak;

public class StateExporter implements Closeable {

	static Logger log = LoggerFactory.getLogger(StateExporter.class);
	
	Path directory;
	List<ExportSelector<?>> writers = new ArrayList<>();
	
	public static StateExporter defaultExporter(String path) {
		 return StateExporter.of(
				SystemUtils.getUserHome().toPath().resolve(path),
				ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState()), "summary.csv"),
				ExportSelector.ofMany(ImmutablePersonCSV.class, o -> o.getPeople().stream().map(CSVMapper.INSTANCE::toCSV), "linelist.csv")
		);
	}
	
	private StateExporter() {}
	
	public static StateExporter of(Path directory, ExportSelector<?>... config) {
		StateExporter out = new StateExporter();
		out.directory = directory;
		out.writers = Arrays.asList(config);
		out.writers.forEach(e -> e.finishSetup(directory));
		return out;
	}
	
	//TODO: make this a runnable thread pool with a buffer 
	
	public static class ExportSelector<X> {
		Class<X> type;
		Function<Outbreak,Stream<X>> selector;
		String filename;
		CSVWriter<X> writer;
		private ExportSelector(
				Class<X> type,
				Function<Outbreak,Stream<X>> selector,
				String file) {
			this.type = type; this.selector = selector; this.filename = file;
		}
		private void finishSetup(Path directory) {
			try {
				this.writer = CSVWriter
						.of(type, directory.resolve(filename).toFile());
			} catch (IOException e) {
				throw new RuntimeException("Couldn't setup exporter: "+directory.resolve(filename));
			}
		}
		public static <X> ExportSelector<X> ofMany(Class<X> type, Function<Outbreak,Stream<X>> selector, String file) {
			return new  ExportSelector<X>(type,selector,file);
		}
		public static <X> ExportSelector<X> ofOne(Class<X> type, Function<Outbreak,X> selector, String file) {
			return new  ExportSelector<X>(type,o -> Stream.of(selector.apply(o)),file);
		}
		public void close() {
			writer.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <X> Outbreak export(Outbreak outbreak) {
		writers.forEach(sel -> {
			ExportSelector<X> sel2 = (ExportSelector<X>) sel; 
			if (sel2.writer != null)
				sel2.writer.export(
						sel2.selector.apply(outbreak).parallel()
				);
		});
		return outbreak;
	}

	public void close() {
		this.writers.forEach(e -> e.close());
	}
	
	
}
