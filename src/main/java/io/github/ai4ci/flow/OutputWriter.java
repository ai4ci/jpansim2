package io.github.ai4ci.flow;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

public interface OutputWriter<X extends CSVWriter.Writeable> extends Closeable {

	default void export(Stream<X> supply) {
		supply.forEach(this::export);
	};

	/**
	 * Called once to setup an output destination and the consumer thread that 
	 * writes data to it, (including buffer sizes if needed). As part of initialising
	 * this might write the headers for CSV files for example, or create the 
	 * DuckDB table. 
	 */
	void setup(Class<X> type, File file, int size) throws IOException;

	/**
	 * A non blocking thread safe export, allowing very quick handing off of a 
	 * data export data class to whatever (single) thread that is going to write
	 * it to disk.
	 */
	void export(X single);

	void flush();
	
	void close();

	boolean isWaiting();

	void join() throws InterruptedException;

	String report();

}