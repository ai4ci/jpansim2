package io.github.ai4ci.deprecated;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Work in progress. This tends to cause crashes and I think has got a memory
 * leak somewhere. Its supposed to be a single thread drop in replacement
 * for a BufferedOutput stream that uses java.nio which is supposed to be
 * faster. The only funky thing it is doing is chunking writes to be whole
 * block sizes, before writing.
 * 
 * There appears to be an upper limit on allocating byte buffers in a sensible
 * amoutn of time. Large buffers are very slow to allocate.
 */
@Deprecated
public class FastWriteOnlyOutputStream extends OutputStream {

	static Logger log = LoggerFactory.getLogger(FastWriteOnlyOutputStream.class);
	
	FileChannel channel;
	ByteBuffer buffer;
	FileLock lock;
	
	public FastWriteOnlyOutputStream(Path file, int blocks) throws IOException {
		long bs = Files.getFileStore(file.getRoot()).getBlockSize();
		buffer = ByteBuffer.allocateDirect((int) (blocks*bs));
		if (Files.exists(file)) Files.delete(file);
		channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		lock = channel.lock();
		log.debug("File: "+file+" opened for writing. Buffer is "+blocks+"*"+bs);
	}
	
	@Override
	public synchronized void write(byte[] b) throws IOException {
		int start = 0;
		while (b.length >= buffer.remaining() + start) {
			int split = buffer.remaining();
			buffer.put(b, start, split);
			start = start+split;
			flush();
		}
		buffer.put(b, start, b.length-start);
	}

	@Override
	public synchronized void flush() throws IOException {
		buffer.flip();
		channel.write(buffer);
		buffer.clear();
	}

	@Override
	public synchronized void close() throws IOException {
		flush();
		lock.release();
		channel.close();
	}

	@Override
	public synchronized void write(int b) throws IOException {
		if (buffer.remaining() == 0) {
			flush();
		}
		buffer.put((byte) (b & 0xFF));
	}

}
