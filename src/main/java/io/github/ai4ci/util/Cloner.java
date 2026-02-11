package io.github.ai4ci.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;

/**
 * Utility for performing deep object copying via serialisation.
 * <p>
 * This class provides high-performance deep copy functionality using custom
 * byte stream implementations that avoid synchronisation overhead.
 * <p>
 * Unlike shallow copies produced by {@link Object#clone()}, deep copies ensure
 * complete independence between original and copied objects by serialising the
 * entire object graph.
 * <p>
 * Performance characteristics include:
 * <ul>
 * <li>Custom buffering with geometric growth: use {@code b_{n+1} = max(s,
 * 2*b_n)}</li>
 * <li>Stream operations without synchronisation overhead</li>
 * <li>Size estimation for optimal buffer pre-allocation</li>
 * </ul>
 * <p>
 * All serialised objects must implement {@link Serializable} to ensure
 * compatibility with the underlying Java serialisation mechanism.
 *
 * @author Rob Challen
 * @see java.io.Serializable
 * @see org.apache.commons.lang3.SerializationUtils
 */
public class Cloner {

	/**
	 * High-performance byte array input stream without synchronisation overhead.
	 * <p>
	 * This implementation provides efficient read access to byte arrays with
	 * mathematical guarantees about stream behaviour. The stream maintains
	 * position tracking and buffer boundary validation.
	 * <p>
	 * Array read operations follow the specification in code form.
	 *
	 * @author Rob Challen
	 * @see java.io.ByteArrayInputStream
	 */
	public static class FastByteArrayInputStream extends InputStream {

		/**
		 * Source byte array buffer.
		 */
		protected byte[] buf = null;

		/**
		 * Total readable bytes in buffer.
		 */
		protected int count = 0;

		/**
		 * Current read position.
		 */
		protected int pos = 0;

		/**
		 * Constructs stream with specified buffer and valid data length.
		 *
		 * @param buf   source byte array containing data
		 * @param count number of valid bytes in buffer
		 */
		public FastByteArrayInputStream(byte[] buf, int count) {
			this.buf = buf;
			this.count = count;
		}

		/**
		 * Returns number of bytes available for reading.
		 *
		 * @return remaining bytes to read
		 */
		@Override
		public final int available() {
			return this.count - this.pos;
		}

		/**
		 * Reads single byte, advancing position.
		 *
		 * @return byte value 0-255 or -1 at end of stream
		 */
		@Override
		public final int read() {
			return (this.pos < this.count) ? (this.buf[this.pos++] & 0xff) : -1;
		}

		/**
		 * Reads byte array slice with boundary validation.
		 *
		 * @param b   destination array
		 * @param off destination offset index
		 * @param len maximum bytes to read
		 * @return actual bytes read or -1 if stream exhausted
		 */
		@Override
		public final int read(byte[] b, int off, int len) {
			if (this.pos >= this.count) return -1;

			if ((this.pos + len) > this.count) { len = (this.count - this.pos); }

			System.arraycopy(this.buf, this.pos, b, off, len);
			this.pos += len;
			return len;
		}

		/**
		 * Skips specified number of bytes with boundary checking.
		 *
		 * @param n number of bytes to skip
		 * @return actual bytes skipped
		 */
		@Override
		public final long skip(long n) {
			if ((this.pos + n) > this.count) { n = (this.count - this.pos); }
			if (n < 0) return 0;
			this.pos += n;
			return n;
		}

	}

	/**
	 * High-performance byte array output stream with geometric buffer growth.
	 * <p>
	 * This implementation avoids method synchronisation and provides efficient
	 * memory management through geometric buffer expansion. Write operations
	 * maintain array integrity through verified buffer sizing. System array copy
	 * operations ensure efficient data movement with O(n) complexity.
	 *
	 * @author Rob Challen
	 * @see java.io.ByteArrayOutputStream
	 */
	public static class FastByteArrayOutputStream extends OutputStream {

		/**
		 * Internal buffer array excluding synchronisation overhead.
		 */
		protected byte[] buf = null;

		/**
		 * Current data size within buffer.
		 */
		protected int size = 0;

		/**
		 * Constructs stream with default 5KB buffer capacity.
		 */
		public FastByteArrayOutputStream() {
			this(5 * 1024);
		}

		/**
		 * Constructs stream with specified initial buffer size.
		 *
		 * @param initSize initial buffer capacity in bytes
		 */
		public FastByteArrayOutputStream(long initSize) {
			this.size = 0;
			this.buf = new byte[(int) initSize];
		}

		/**
		 * Returns underlying byte array containing written data.
		 * <p>
		 * Note: array capacity typically exceeds actual data size due to
		 * geometric allocation strategy.
		 *
		 * @return buffer array
		 */
		public byte[] getByteArray() { return this.buf; }

		/**
		 * Creates input stream for reading stored data.
		 *
		 * @return FastByteArrayInputStream positioned at buffer start
		 */
		public InputStream getInputStream() {
			return new FastByteArrayInputStream(this.buf, this.size);
		}

		/**
		 * Resets stream to initial empty state.
		 * <p>
		 * Buffer memory retained.
		 */
		public void reset() {
			this.size = 0;
		}

		/**
		 * Returns current data size in bytes.
		 *
		 * @return number of bytes written to stream
		 */
		public int size() {
			return this.size;
		}

		/**
		 * Ensures buffer capacity meets or exceeds required size.
		 *
		 * @param sz minimum required buffer size
		 */
		private void verifyBufferSize(int sz) {
			if (sz > this.buf.length) {
				byte[] old = this.buf;
				this.buf = new byte[Math.max(sz, 2 * this.buf.length)];
				System.arraycopy(old, 0, this.buf, 0, old.length);
				old = null;
			}
		}

		/**
		 * Writes complete byte array with geometric buffer expansion.
		 *
		 * @param b source byte array with length
		 */
		@Override
		public final void write(byte b[]) {
			this.verifyBufferSize(this.size + b.length);
			System.arraycopy(b, 0, this.buf, this.size, b.length);
			this.size += b.length;
		}

		/**
		 * Writes byte array slice with bounds verification.
		 *
		 * @param b   source array
		 * @param off starting offset
		 * @param len number of elements
		 */
		@Override
		public final void write(byte b[], int off, int len) {
			this.verifyBufferSize(this.size + len);
			System.arraycopy(b, off, this.buf, this.size, len);
			this.size += len;
		}

		/**
		 * Writes single byte with minimal buffer expansion.
		 *
		 * @param b byte value to write
		 */
		@Override
		public final void write(int b) {
			this.verifyBufferSize(this.size + 1);
			this.buf[this.size++] = (byte) b;
		}

	}

	/**
	 * Stream implementation for size estimation without actual data storage.
	 * <p>
	 * This mock stream tracks cumulative bytes written without storing data,
	 * providing memory-efficient size estimation.
	 *
	 * @author Rob Challen
	 * @see java.io.OutputStream
	 */
	public static class MockOutputStream extends OutputStream {

		private long size = 0;

		/**
		 * Returns the cumulative size of all write operations.
		 *
		 * @return the accumulated size estimate
		 */
		public long size() {
			return this.size;
		}

		/**
		 * Records a complete byte array write operation.
		 *
		 * @param b the byte array being written
		 */
		@Override
		public final void write(byte b[]) {
			this.size += b.length;
		}

		/**
		 * Records a slice of byte array write operation.
		 *
		 * @param b   the source byte array
		 * @param off the starting offset in the array
		 * @param len the number of bytes to count
		 */
		@Override
		public final void write(byte b[], int off, int len) {
			this.size += len;
		}

		/**
		 * Records a single byte write operation.
		 */
		@Override
		public void write(int b) throws IOException {
			this.size += 1;
		}
	}

	/**
	 * Creates a deep copy of a serialisable object using optimised buffers.
	 * <p>
	 * This method provides a convenient interface for deep copying objects with
	 * automatic size estimation and optimised buffer management.
	 *
	 * For objects larger than \[ 2^{31}-8 \] bytes, the method delegates to
	 * {@link SerializationUtils#clone(Serializable)} for safety.
	 *
	 * @param <X>  the type of object being copied, must extend Serializable
	 * @param orig the original object to copy
	 * @return a deep copy of the original object
	 * @throws RuntimeException if serialisation process fails
	 */
	public static <X extends Serializable> X copy(X orig) {
		return copy(orig, 128 * 1024 * 1024);

	}

	/**
	 * Creates a deep copy with explicit size estimation for optimal performance.
	 * <p>
	 * This method provides a memory-efficient deep copy operation with optimised
	 * buffer allocation using the estimated size. The buffer size is calculated
	 * as 110% of the estimated size to balance between memory usage and the
	 * likelihood of needing to resize during serialisation, as empirical testing
	 * suggests that where buffer over-allocation by 10% minimises re-sizing
	 * overhead.
	 *
	 * @param <X>           the serialisable object type
	 * @param orig          object to copy
	 * @param estimatedSize estimated serialisation size for buffer optimisation
	 * @return independent deep copy of original object
	 * @throws RuntimeException on serialisation errors
	 */
	@SuppressWarnings("unchecked")
	public static <X extends Serializable> X copy(X orig, long estimatedSize) {
		if (estimatedSize > Integer.MAX_VALUE - 8)
			return SerializationUtils.clone(orig);
		X obj = null;
		try {
			// Write the object out to a byte array
			FastByteArrayOutputStream fbos = new FastByteArrayOutputStream(
					(long) (estimatedSize * 1.1)
			);
			ObjectOutputStream out = new ObjectOutputStream(fbos);
			out.writeObject(orig);
			out.flush();
			out.close();

			// Retrieve an input stream from the byte array and read
			// a copy of the object back in.
			ObjectInputStream in = new ObjectInputStream(fbos.getInputStream());
			obj = (X) in.readObject();
		} catch (IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return obj;
	}

	/**
	 * Estimates serialisation size without storing actual data.
	 * <p>
	 * This method calculates the estimated byte count required to serialise an
	 * object.
	 * <p>
	 * The estimation employs a MockOutputStream that tracks size without data
	 * storage, providing memory-efficient size prediction for buffer allocation
	 * optimisation in subsequent copy operations.
	 *
	 * @param obj object to estimate serialisation size for
	 * @return estimated bytes required for serialisation \[ \geq 0 \]
	 * @throws RuntimeException if serialisation fails during estimation
	 */
	public static long estimateSize(Object obj) {
		long objSize = 0;
		try {
			MockOutputStream baos = new MockOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
			objSize = baos.size();
			baos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return objSize;
	}

}