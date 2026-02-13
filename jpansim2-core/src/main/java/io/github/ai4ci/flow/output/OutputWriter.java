package io.github.ai4ci.flow.output;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

/**
 * Interface for writing output data to a destination (e.g. file, database).
 * Implementations must be thread safe and support non blocking export of data
 * records, allowing for efficient handoff of data from the simulation thread to
 * a separate consumer thread that handles the actual writing to disk. The setup
 * method is called once to initialise the output destination and any necessary
 * resources, while the export method can be called multiple times to write
 * individual records. The flush and close methods allow for proper resource
 * management and finalization of the output. The isWaiting and join methods
 * provide mechanisms for monitoring the state of the consumer thread and
 * ensuring that all data has been written before shutting down. The report
 * method can be used to provide status updates or performance metrics about the
 * writing process.
 *
 * @param <X> the type of data records that this OutputWriter can write, which
 *            must implement the Writeable interface
 * @author Rob Challen
 */
public interface OutputWriter<X extends io.github.ai4ci.flow.output.OutputWriter.Writeable>
		extends Closeable {

	/**
	 * Marker interface for data records that can be written by an OutputWriter.
	 * Implementing classes should define the structure of the data to be
	 * exported and may include methods for serialization or formatting of the
	 * data as needed by specific OutputWriter implementations (e.g. CSV
	 * formatting, database mapping). This interface serves as a common type for
	 * all data records that can be handled by Output Writers, ensuring type
	 * safety and consistency across different output formats and destinations.
	 *
	 */
	public static interface Writeable {
//		String header();
//		String row();
	}

	/**
	 * Close the output writer and release any resources associated with it. This
	 * method should ensure that all pending data is flushed to the output
	 * destination before closing, and that any threads or resources used for
	 * writing are properly terminated. Implementations should handle any
	 * necessary cleanup to prevent resource leaks and ensure that the output
	 * destination is left in a consistent state.
	 */
	@Override
	void close();

	/**
	 * Default method to export a stream of data records. This method iterates
	 * over the provided stream and calls the export method for each individual
	 * record. Implementations can override this method if they have specific
	 * optimizations for handling batches of records, but the default
	 * implementation provides a simple way to export multiple records without
	 * requiring additional logic in the consumer code.
	 *
	 * @param supply a Stream of data records to be exported, where each record
	 *               is of type X that implements Writeable
	 */
	default void export(Stream<X> supply) {
		supply.forEach(this::export);
	}

	/**
	 * A non blocking thread safe export, allowing very quick handing off of a
	 * data export data class to whatever (single) thread that is going to write
	 * it to disk.
	 *
	 * @param single a single data record of type X that implements Writeable to
	 *               be exported; this method should return quickly and not block
	 *               the calling thread, allowing for efficient handoff to the
	 *               consumer thread that will handle the actual writing to the
	 *               output destination
	 *
	 */
	void export(X single);

	/**
	 * Flush any buffered data to the output destination. This method should
	 * ensure that all data that has been handed off for writing is actually
	 * written to disk, and that any resources used for buffering are properly
	 * managed. Implementations should handle any necessary synchronization to
	 * ensure thread safety during the flush operation.
	 */
	void flush();

	/**
	 * Check if the consumer thread is currently waiting for data to be exported.
	 * This method can be used to monitor the state of the output writer and
	 * determine if it is idle or actively processing data. Implementations
	 * should provide a thread-safe way to check this state, allowing for
	 * coordination between the producer and consumer threads.
	 *
	 * @return true if the consumer thread is waiting for data, false otherwise
	 */
	boolean isWaiting();

	/**
	 * Wait for the consumer thread to finish processing all pending data and
	 * complete any ongoing write operations. This method should block until the
	 * consumer thread has finished writing all data to the output destination,
	 * ensuring that all exports are completed before proceeding with any
	 * shutdown or cleanup operations. Implementations should handle any
	 * necessary synchronization to ensure that this method works correctly in a
	 * multi-threaded environment.
	 *
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the consumer thread to finish
	 */
	void join() throws InterruptedException;

	/**
	 * Provide a status report or performance metrics about the writing process.
	 * This method can be used to return information about the current state of
	 * the output writer, such as the number of records written, the time taken
	 * for recent exports, or any errors encountered during writing.
	 * Implementations can customize this method to provide relevant information
	 * based on the specific output format and destination being used.
	 *
	 * @return a String containing the status report or performance metrics about
	 *         the writing process
	 */
	String report();

	/**
	 * Called once to setup an output destination and the consumer thread that
	 * writes data to it, (including buffer sizes if needed). As part of
	 * initialising this might write the headers for CSV files for example, or
	 * create the DuckDB table.
	 *
	 * @param type the Class object representing the type of data records that
	 *             will be written, used for reflection or type-specific setup
	 * @param file the File object representing the output destination (e.g. file
	 *             path) where data will be written
	 * @param size an integer representing the buffer size or batch size for
	 *             writing data, which can be used to optimize performance based
	 *             on the expected volume of data
	 * @throws IOException if an I/O error occurs during setup, such as issues
	 *                     with file permissions, disk space, or other problems
	 *                     that prevent the output destination from being
	 *                     properly initialized
	 *
	 */
	void setup(Class<X> type, File file, int size) throws IOException;

}