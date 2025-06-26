package io.github.ai4ci.flow;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.ai4ci.Export;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.CSVWriter.Writeable;

/** This is the first stage of configuring a destination for export based
 * on the specification of the data and the mappings to get the data from 
 * the outbreak class. This also defines the type of output (e.g. CSV) and
 * initialisation of the output file in an output directory as appropriate.
 */
public class ExportSelector<X extends CSVWriter.Writeable> {
	private Export.Stage stage;
	private Class<X> type;
	private String filename;
	private int size;
	
	// TODO: Abstract CSVWriter and implement DuckDBWriter using appendable interface
	// https://duckdb.org/docs/stable/clients/java.html#appender
	// This will need to be configurable in the Export annotation.
	private OutputWriter<X> writer;
	private Function<Outbreak, Stream<? extends Writeable>> selector;
	
	private ExportSelector(Class<X> type) {
		Export.Stage stage = type.getAnnotation(Export.class).stage();
		String file = type.getAnnotation(Export.class).value();
		int size = type.getAnnotation(Export.class).size();
		try {
			this.selector = type.getAnnotation(Export.class).selector().getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.type = type;
		this.filename = file; 
		this.size = size; 
		this.stage = stage;
	}
	
	@SuppressWarnings("unchecked")
	public void finishSetup(Path directory) {
		try {
			this.writer = (OutputWriter<X>) type.getAnnotation(Export.class).writer().getDeclaredConstructor().newInstance();
			this.writer.setup(type, directory.resolve(filename).toFile(), size);
		} catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Couldn't setup exporter: "+directory.resolve(filename),e);
		}
	}
	
	public OutputWriter<X> getWriter() {return writer;}
	
	public static <X extends CSVWriter.Writeable> ExportSelector<X> of(Class<X> type) {
		return new ExportSelector<X>(type);
	}

	public Export.Stage getStage() {
		return stage;
	}

//	public Function<Outbreak, Stream<? extends Writeable>> getSelector() {
//		return selector;
//	}

	public Stream<? extends Writeable> selector(Outbreak outbreak) {
		return selector.apply(outbreak);
	}
}