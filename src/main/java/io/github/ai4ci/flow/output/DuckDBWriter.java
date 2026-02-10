package io.github.ai4ci.flow.output;

import static io.github.ai4ci.util.DuckDBUtil.append;
import static io.github.ai4ci.util.DuckDBUtil.createSql;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.util.CSVUtil;

/**
 * OutputWriter implementation for writing records to a DuckDB database file.
 * This writer uses a background thread to continuously flush records from a
 * concurrent queue to the database, allowing for efficient batch inserts. The
 * writer is designed to handle records of a specified type that implements
 * CSVWriter.Writeable, and it manages the database connection, table creation,
 * and record insertion in a thread-safe manner. The writer also provides
 * methods for flushing the queue, checking if the writer is waiting for new
 * records, and reporting the current status of the writer.
 * 
 * @param <X> the type of records to be written, which must implement
 *            CSVWriter.Writeable for CSV serialization
 */
public class DuckDBWriter<X extends CSVWriter.Writeable> implements OutputWriter<X> {

	QueueConnection queue;
	CSVUtil<X> converter;
	static Logger log = LoggerFactory.getLogger(DuckDBWriter.class);
	
	/**
	 * Initialises a new DuckDBWriter instance. The actual setup of the database
	 * connection and table schema is performed in the setup method, which must be
	 * called before exporting any records. The constructor does not perform any
	 * operations on the database and is designed to allow for flexible
	 * initialization of the writer, with the setup method handling all necessary
	 * configuration based on the provided file and record type.
	 * 
	 */
	public DuckDBWriter() {
	}

	@Override
	public void setup(Class<X> type, File file, int size) throws IOException {
		converter = new CSVUtil<X>(type);
		String tableName = file.toPath().getFileName().toString()
				.replaceFirst("(?<!^)[.].*", "");
		try {
			queue = new QueueConnection(file,type,tableName);
		} catch (SQLException e) {
			throw new IOException("Cannot setup duckdb",e);
		}
		//Properties props = new Properties();
		//props.setProperty(DuckDBDriver.JDBC_STREAM_RESULTS, String.valueOf(true));
	}

	@Override
	public void export(X single) {
		Object[] tmp = converter.valuesOf(single);
		queue.submit(tmp);
	}

	@Override
	public void flush() {
		queue.flush();
	}

	@Override
	public void close() {
		queue.halt();

	}

	@Override
	public boolean isWaiting() {
		return queue.isWaiting();
	}

	@Override
	public void join() throws InterruptedException {
		queue.join();
	}

	@Override
	public String report() {
		return queue.report();
	}

	/**
	 *  Inner class that manages the database connection and record flushing in a separate thread. The QueueConnection
	 *  continuously checks for new records in the queue and flushes them to the database. It also handles shutdown
	 *  signals and ensures that all records are flushed before closing the connection. The class provides
	 *  synchronization mechanisms to manage waiting for new records and to signal the thread to wake up when new records
	 *  are added or when a shutdown signal is received.
	 *  
	 */
	public class QueueConnection extends Thread {
		ConcurrentLinkedQueue<Object[]> queue;
		DuckDBConnection conn;
		String tableName;
		volatile boolean stop = false;
		volatile boolean waiting = false;
		volatile private Object semaphore = new Object();

		/**
		 * Initialises a new QueueConnection for writing to a DuckDB database. Sets up
		 * the database file and creates the appropriate table schema based on the
		 * provided class type. The thread will continuously flush records from the
		 * queue to the database until halted.
		 * 
		 * @param file the file to which the DuckDB database will be written; if the
		 *             file already exists, it will be deleted and recreated
		 * @param type the class type of the records to be written; used to generate the
		 *             appropriate SQL table schema
		 * @param name the name of the table to be created in the DuckDB database for
		 *             storing the records
		 * @throws SQLException if there is an error connecting to the DuckDB database
		 *                      or executing the table creation SQL
		 * @throws IOException  if there is an error deleting or creating the database
		 *                      file
		 */
		public QueueConnection(File file, Class<X> type, String name) throws SQLException, IOException {
			this.queue = new ConcurrentLinkedQueue<Object[]>();
			if (file.exists()) Files.delete(file.toPath());
			Files.createDirectories(file.toPath().getParent());
			conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:"+file.getAbsolutePath());
			tableName = name;

			try (var stmt = conn.createStatement()) {
				stmt.execute(createSql(type, name));
			}

			this.setPriority(9);
			this.setName("DuckDbWriter: "+name);
			// this.setDaemon(true);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					QueueConnection.this.halt();
				}
			});
			this.start();
		}

		/**
		 * Submits a record to the queue for writing to the database. If the writer
		 * thread is currently waiting for new records, it will be notified to wake up
		 * and process the queue.
		 * 
		 * @param item the record to be added to the queue, represented as an array of
		 *             objects corresponding to the columns of the database table
		 */
		public void submit(Object[] item) {
			if (queue.offer(item) && waiting) {
				synchronized(semaphore) { semaphore.notifyAll(); };
			}
		}

		/**
		 * Halts the writer thread, signaling it to stop processing and flush any
		 * remaining records in the queue to the database before closing the connection.
		 * If the thread is currently waiting for new records, it will be notified to
		 * wake up and complete the shutdown process.
		 */
		public void halt() {
			this.stop = true;
			synchronized(semaphore) { semaphore.notifyAll(); };
		}
		
		/**
		 * Reports the current status of the writer thread, indicating whether it is
		 * waiting for new records or actively writing to the database.
		 * 
		 * @return a string indicating the current status of the writer thread; returns
		 *         "empty" if the thread is waiting for new records, and "writing" if it
		 *         is actively processing the queue
		 */
		public String report() {
			return isWaiting() ? "empty" : "writing";
		};

		/**
		 * The main run loop of the writer thread. Continuously checks for new records
		 * in the queue and flushes them to the database. If the queue is empty, the
		 * thread will wait until it is notified of new records or a shutdown signal.
		 * When a shutdown signal is received, the thread will flush any remaining
		 * records and close the database connection before exiting.
		 */
		public void run() {
			try {

				while (!stop) {
					while (!stop && this.queue.isEmpty()) {
						try {
							synchronized(semaphore) {
								waiting = true;
								semaphore.wait();
							}
						} catch (Exception e) {
							stop = true;
						}
					}
					waiting = false;
					if (!stop) {
						if (!this.queue.isEmpty()) {
							flush();
						}
					}
				}
				flush();
				log.info("Closing duck db "+tableName);
				conn.close();
				log.info("Closed duck db "+tableName);
				this.waiting = true;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Flushes the queue to the database. This is called by the thread's run loop
		 */
		public void flush() {
			try {
				DuckDBAppender appender = conn.createAppender(DuckDBConnection.DEFAULT_SCHEMA, tableName);
				while (!this.queue.isEmpty()) {
					Object[] tmp = queue.poll();
					if (tmp!=null) {
						appender.beginRow();
						for (Object o: tmp) append(appender, o);
						appender.endRow();
					}
				}
				appender.flush();
				appender.close();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * Checks if the writer thread is currently waiting for new records to be added to the
		 * queue. This can be used to determine if the writer is idle or actively processing records.
		 * 
		 * @return true if the writer thread is waiting for new records, false if it is actively processing the queue
		 */
		public boolean isWaiting() {
			return waiting;
		}
	}



	

}
