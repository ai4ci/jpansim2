package io.github.ai4ci.flow.output;

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
 *
 * @param <X> the type of data objects to be exported, which must implement
 *            OutputWriter.Writeable
 */
public class CSVWriter<X extends OutputWriter.Writeable>
		implements Closeable, OutputWriter<X> {

	QueueWriter queueWriter;
	CSVUtil<X> converter;

	/**
	 * No-argument constructor for reflection-based instantiation. The actual
	 * setup of the writer (including file destination and buffer sizes) is
	 * handled in the setup method, which is called after instantiation.
	 */
	public CSVWriter() {
		// no args constructor for reflection
	}

	@Override
	public void close() {
		this.queueWriter.halt();
	}

	@Override
	public void export(Stream<X> supply) {
		supply.forEach(this::export);
	}

	/**
	 * A non blocking thread safe export, allowing very quick handing off of a
	 * data export data class to whatever (single) thread that is going to write
	 * it to disk.
	 */
	@Override
	public void export(X single) {
		// queueWriter.submit(single.row());
		this.queueWriter.submit(this.converter.row(single)); // reflection
	}

	@Override
	public void flush() {
		this.queueWriter.flush();
	}

	@Override
	public boolean isWaiting() { return this.queueWriter.isWaiting(); }

	@Override
	public void join() throws InterruptedException {
		this.queueWriter.join();
	}

	@Override
	public String report() {
		return this.queueWriter.report();
	}

	/**
	 * Called once to setup an output destination and the consumer thread that
	 * writes data to it, (including buffer sizes if needed). As part of
	 * initialising this might write the headers for CSV files for example.
	 */
	@Override
	public void setup(Class<X> type, File file, int size) throws IOException {
		if (Files.exists(file.toPath())) { Files.delete(file.toPath()); }
		this.converter = new CSVUtil<>(type);
		Files.createDirectories(file.getParentFile().toPath());
		this.queueWriter = new QueueWriter(
				file, size, type.getSimpleName() + " writer"
		);
		String headers = this.converter.headers();
		this.queueWriter.submit(headers);
	}

}
