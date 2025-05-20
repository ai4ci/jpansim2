package io.github.ai4ci.flow;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueWriter extends Thread implements CSVWriter.Queue {
		
		ConcurrentLinkedQueue<String> queue;
		// ThreadSafeBuffer<String> queue;
		OutputStream seqW;
		volatile boolean stop = false;
		volatile boolean waiting = false;
		private Object semaphore = new Object();
		
		public QueueWriter(File file, int size, String name, String headers) throws IOException {
			long bs = Files.getFileStore(file.toPath().getRoot()).getBlockSize();
			this.seqW = new BufferedOutputStream(new FileOutputStream(file), (int) (size*bs));
			//this.seqW = new FastWriteOnlyOutputStream(file.toPath(), size);
			try {
				seqW.write(line(headers));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.queue = new ConcurrentLinkedQueue<String>();
			this.setPriority(9);
			this.setName(name);
			this.setDaemon(true);
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
			// this.queue.add(item.row());
//			try {
//				queue.offer(item.row(), Long.MAX_VALUE, TimeUnit.SECONDS);
//			} catch (InterruptedException e) {
//				throw new RuntimeException(e);
//			}
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
					while (this.queue.isEmpty()) {
						// Thread.onSpinWait();
//						try {
//							Thread.sleep(1);
//						} catch (InterruptedException e) {
//							stop = true;
//						}
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
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
//		public void purge() {
//			synchronized(queue) {
//				try {
//					while (!this.queue.isEmpty()) {
//						seqW.write(line(queue.poll()));
//					}
//					this.seqW.flush();
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//			}
//		}

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
		
		public String report() {
			return waiting ? "empty" : "writing";
		}
	}