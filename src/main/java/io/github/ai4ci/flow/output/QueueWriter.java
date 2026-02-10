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
 * A thread responsible for writing a single CSV file with a buffer.
 * Multiple threads can submit data to this writer without blocking which will
 * buffer and then write them to disk. If nothing is being written the
 * thread will sleep until more data is provided.     
 */
public class QueueWriter extends Thread { //implements QueueWriter.Queue<String> {
		
	/** this was abstracted to allow for swapping concurrentModified queue for
	 * a custom version. It may not be strictly required any more. */
//	public static interface Queue<X> {
//		public void submit(X item) throws InterruptedException;
//		public void halt() throws InterruptedException;
//		public void flush() throws InterruptedException;
//		public boolean isWaiting();
//		//public void purge();
//		public void join() throws InterruptedException;
		public String report() {
			return isWaiting() ? "empty" : "writing";
		};
//	}

		static Logger log = LoggerFactory.getLogger(QueueWriter.class);
		ConcurrentLinkedQueue<String> queue;
		// ThreadSafeBuffer<String> queue;
		private OutputStream seqW;
		volatile boolean stop = false;
		volatile boolean waiting = false;
		private Object semaphore = new Object();
		
		public QueueWriter(File file, int size, String name) throws IOException {
			long bs = Files.getFileStore(file.toPath().getRoot()).getBlockSize();
			this.seqW = new BufferedOutputStream(new FileOutputStream(file), (int) (size*bs));
			this.queue = new ConcurrentLinkedQueue<String>();
			this.setPriority(9);
			this.setName(name);
			// this.setDaemon(true);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					QueueWriter.this.halt();
				}
			});
			this.start();
			// this.queue = new ArrayBlockingQueue<String>(size*16);
			// this.queue = new ThreadSafeBuffer<String>(String.class, size);
		}
		
		public void submit(String item) {
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
						while (!this.queue.isEmpty()) {
							seqW.write(line(queue.poll()));
						}
						this.seqW.flush();
					}
				}
				while (!this.queue.isEmpty()) {
					seqW.write(line(queue.poll()));
				}
				this.seqW.flush();
				this.seqW.close();
				log.info("Closed csv file: "+this.getName());
				this.waiting = true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void flush() {
			synchronized(seqW) {
				try {
					this.seqW.flush();
				} catch (IOException e) {
					// we tried
				}
			}
		}
		
		private static byte[] line(String s) {
			return (s+System.lineSeparator()).getBytes();
		}
		
		public boolean isWaiting() {
			return waiting;
		}
	}