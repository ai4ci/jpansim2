package io.github.ai4ci.flow.output;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A thread responsible for writing a single CSV file with a buffer. Multiple
 * threads can submit data to this writer without blocking which will buffer and
 * then write them to disk. If nothing is being written the thread will sleep
 * until more data is provided.
 */
public class QueueWriter extends Thread {

	/** Logger for this class. */
	static Logger log = LoggerFactory.getLogger(QueueWriter.class);

	private static byte[] line(String s) {
		return (s + System.lineSeparator()).getBytes();
	}

	ConcurrentLinkedQueue<String> queue;
	// ThreadSafeBuffer<String> queue;
	private OutputStream seqW;
	volatile boolean stop = false;
	volatile boolean waiting = false;
	private Object semaphore = new Object();

	/**
	 * Create a new QueueWriter which will write to the given file with a buffer
	 * of the given size (in blocks). The thread will be named with the given
	 * name for logging purposes. The thread will start immediately and will be
	 * ready to accept data for writing.
	 *
	 * @param file the file to write to
	 * @param size the size of the buffer in blocks
	 * @param name the name of the thread for logging purposes
	 * @throws IOException if an I/O error occurs while opening the file or
	 *                     getting its block size
	 */
	public QueueWriter(File file, int size, String name) throws IOException {
		var bs = Files.getFileStore(file.toPath().getRoot()).getBlockSize();
		this.seqW = new BufferedOutputStream(
				new FileOutputStream(file), (int) (size * bs)
		);
		this.queue = new ConcurrentLinkedQueue<>();
		this.setPriority(9);
		this.setName(name);
		// this.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				QueueWriter.this.halt();
			}
		});
		this.start();
		// this.queue = new ArrayBlockingQueue<String>(size*16);
		// this.queue = new ThreadSafeBuffer<String>(String.class, size);
	}

	/**
	 * This is not strictly necessary but it allows for a manual flush to ensure
	 * all data is written to disk. It will not interrupt the writing thread and
	 * will not block other threads from submitting data.
	 */
	public void flush() {
		synchronized (this.seqW) {
			try {
				this.seqW.flush();
			} catch (IOException e) {
				// we tried
			}
		}
	}

	/**
	 * Halts the writing thread. This will cause the thread to finish writing any
	 * remaining data in the queue and then exit. Once halted, the thread cannot
	 * be restarted.
	 */
	public void halt() {
		this.stop = true;
		synchronized (this.semaphore) {
			this.semaphore.notifyAll();
		}
	}

	/**
	 * Is the queue currently empty and the writing thread is waiting for data to
	 * write?
	 *
	 * @return true if the writing thread is currently waiting for data to write,
	 *         false otherwise. This can be used to determine if the queue is
	 *         currently empty and the thread is idle.
	 */
	public boolean isWaiting() { return this.waiting; }

	/**
	 * Report the current status of the writing thread. This will return "empty"
	 * if the writing thread is currently waiting for data to write, and
	 * "writing" otherwise. This can be used to monitor the status of the writing
	 * thread.
	 *
	 * @return a string representing the current status of the writing thread
	 */
	public String report() {
		return this.isWaiting() ? "empty" : "writing";
	}

	@Override
	public void run() {
		try {

			while (!this.stop) {
				while (!this.stop && this.queue.isEmpty()) {
					try {
						synchronized (this.semaphore) {
							this.waiting = true;
							this.semaphore.wait();
						}
					} catch (Exception e) {
						this.stop = true;
					}
				}
				this.waiting = false;
				if (!this.stop) {
					while (!this.queue.isEmpty()) {
						this.seqW.write(line(this.queue.poll()));
					}
					this.seqW.flush();
				}
			}
			while (!this.queue.isEmpty()) {
				this.seqW.write(line(this.queue.poll()));
			}
			this.seqW.flush();
			this.seqW.close();
			log.info("Closed csv file: " + this.getName());
			this.waiting = true;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Submit a string to be written to the file. This will add the string to the
	 * queue and notify the writing thread if it is currently waiting for data.
	 *
	 * @param item the string to be written to the file
	 */
	public void submit(String item) {
		if (this.queue.offer(item) && this.waiting) {
			synchronized (this.semaphore) {
				this.semaphore.notifyAll();
			}
		}

	}
}