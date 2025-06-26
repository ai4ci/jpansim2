package io.github.ai4ci.flow;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import io.github.ai4ci.util.CSVUtil;

/**
 * This class provides infrastructure for translating a high throughput stream
 * of Writeable objects from multiple threads. This maps the data to strings in
 * the calling thread to take advantage of parallelisation. 
 */
public class CSVWriter<X extends CSVWriter.Writeable> implements Closeable, OutputWriter<X> {
	
	public static interface Writeable {
//		String header();
//		String row();
	}
	
	Queue<String> queueWriter;
	CSVUtil<X> converter;
	
	@Override
	public void export(Stream<X> supply) {
		supply.forEach(this::export);
	}
	
	/** this was abstracted to allow for swapping concurrentModified queue for
	 * a custom version. It may not be strictly required any more. */
	public static interface Queue<X> {
		public void submit(X item) throws InterruptedException;
		public void halt() throws InterruptedException;
		public void flush() throws InterruptedException;
		public boolean isWaiting();
		//public void purge();
		public void join() throws InterruptedException;
		default public String report() {
			return isWaiting() ? "empty" : "writing";
		};
	}
	
	/**
	 * Called once to setup an output destination and the consumer thread that 
	 * writes data to it, (including buffer sizes if needed). As part of initialising
	 * this might write the headers for CSV files for example. 
	 */
	@Override
	public void setup(Class<X> type, File file, int size) throws IOException {
		if (Files.exists(file.toPath())) Files.delete(file.toPath());
		converter = new CSVUtil<X>(type);
		Files.createDirectories(file.getParentFile().toPath());
		queueWriter = new QueueWriter(file, size, type.getSimpleName()+" writer");
		String headers = converter.headers();
		try {
			queueWriter.submit(headers);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * A non blocking thread safe export, allowing very quick handing off of a 
	 * data export data class to whatever (single) thread that is going to write
	 * it to disk.
	 */
	@Override
	public void export(X single) {
		try {
			// queueWriter.submit(single.row());
			queueWriter.submit(converter.row(single)); // reflection
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

	@Override
	public void flush() {
		try {
			queueWriter.flush();
		} catch (InterruptedException e) {
			
		}
	}

	@Override
	public boolean isWaiting() {
		return queueWriter.isWaiting();
	}

	@Override
	public void join() throws InterruptedException {
		queueWriter.join();
	}

	@Override
	public String report() {
		return queueWriter.report();
	}
	
}
