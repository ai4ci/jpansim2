package io.github.ai4ci.flow;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import io.github.ai4ci.util.CSVUtil;
import io.github.ai4ci.util.QueueWriter;

public class CSVWriter<X extends CSVWriter.Writeable> implements Closeable {
	
	public static interface Writeable {
//		String header();
//		String row();
	}
	
	File file;
	CsvMapper cm;
	CsvSchema sch;
	Class<X> type;
	Queue queueWriter;
	
	public void export(Stream<X> supply) {
		supply.forEach(this::export);
	}
	
	public static interface Queue {
		public void submit(String item) throws InterruptedException;
		public void halt() throws InterruptedException;
		public void flush() throws InterruptedException;
	}
	
	public static <X extends Writeable> CSVWriter<X> of(Class<X> type, File file, int size) throws IOException {
		return new CSVWriter<X>(type, file, size);
	}
	
	private CSVWriter(Class<X> type, File file, int size) throws IOException {
		this.type = type;
		if (Files.exists(file.toPath())) Files.delete(file.toPath());
		Files.createDirectories(file.getParentFile().toPath());
		
 		String headers = CSVUtil.headers(type);
		queueWriter = new QueueWriter(file, size, type.getSimpleName()+" writer", headers);
		//queueWriter = new SimpleAsyncFileWriter(file, size, type.getSimpleName()+" writer", headers);
		
	}
	
	public void export(X single) {
		try {
			// queueWriter.submit(single.row());
			queueWriter.submit(CSVUtil.row(single)); // reflection
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			queueWriter.halt();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void flush() {
		try {
			queueWriter.flush();
		} catch (InterruptedException e) {
			
		}
	}
	
}
