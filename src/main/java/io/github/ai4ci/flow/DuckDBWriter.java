package io.github.ai4ci.flow;

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

public class DuckDBWriter<X extends CSVWriter.Writeable> implements OutputWriter<X> {

	QueueConnection queue;
	CSVUtil<X> converter;
	static Logger log = LoggerFactory.getLogger(DuckDBWriter.class);

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
		// TODO Auto-generated method stub

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

	public class QueueConnection extends Thread implements CSVWriter.Queue<Object[]> {
		ConcurrentLinkedQueue<Object[]> queue;
		DuckDBConnection conn;
		String tableName;
		volatile boolean stop = false;
		volatile boolean waiting = false;
		volatile private Object semaphore = new Object();

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

		public void submit(Object[] item) {
			if (queue.offer(item) && waiting) {
				synchronized(semaphore) { semaphore.notifyAll(); };
			}
		}

		public void halt() {
			this.stop = true;
			synchronized(semaphore) { semaphore.notifyAll(); };
		}

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

		public boolean isWaiting() {
			return waiting;
		}
	}



	

}
