package io.github.ai4ci.util;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import io.github.ai4ci.flow.CSVWriter.Queue;

@Deprecated
public class SimpleAsyncFileWriter implements Queue {
    private final BlockingQueue<String> queue;
    private final Thread writerThread;
    private final BufferedOutputStream outputStream;
    private volatile boolean running = true;

    // Config
    private int size;
    private static final int FLUSH_INTERVAL_MS = 2000;

    public SimpleAsyncFileWriter(File file, int size, String name, String headers) throws IOException {
        // this.queue = new ArrayBlockingQueue<>(size*1024);
        this.queue = new ThreadSafeBuffer<String>(String.class, size*1024);
        this.size = size;
        this.outputStream = new BufferedOutputStream(new FileOutputStream(file, true), size * 1024);
        this.outputStream.write(line(headers));
        this.writerThread = new Thread(this::runWriter, "AsyncFileWriter");
        this.writerThread.setDaemon(true);
        this.writerThread.setName(name);
        this.writerThread.start();
    }

    public void submit(String line) throws InterruptedException {
        queue.put(line); // blocks if queue is full (backpressure)
    }

    private void writeBatch(List<String> batch) throws IOException {
    	for (String line : batch) {
            outputStream.write(line(line));
        }
    }
    
    private byte[] line(String s) {
    	return (s + System.lineSeparator()).getBytes();
    }
    
    private void runWriter() {
        List<String> batch = new ArrayList<>(size);
        long lastFlushTime = System.currentTimeMillis();

        try {
            while (running) {
            	
            	long now = System.currentTimeMillis();
            	
            	while (running && queue.size() < size && (now - lastFlushTime) > FLUSH_INTERVAL_MS)
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						running = false;
					}
            
                queue.drainTo(batch, size);
                writeBatch(batch);
                
                if (!batch.isEmpty() || (now - lastFlushTime) > FLUSH_INTERVAL_MS) {
                    outputStream.flush(); // ensure data is flushed periodically
                    lastFlushTime = now;
                }
                
                batch.clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void halt() throws InterruptedException {
        running = false;
        writerThread.join();
    }

	@Override
	public void flush() throws InterruptedException {
		try {
			outputStream.flush();
		} catch (IOException e) {
			
		}
	}
	
	public boolean isWaiting() {
		return queue.isEmpty() || queue.size() < size;
	}

	@Override
	public void join() throws InterruptedException {
		writerThread.join();
	}
	
//	public void purge() {
//		synchronized(queue) {
//			List<String> batch = new ArrayList<>();
//			queue.drainTo(batch, size);
//            try {
//				writeBatch(batch);
//				flush();
//			} catch (IOException | InterruptedException e) {
//				throw new RuntimeException(e);
//			}
//		}
//	}
}

