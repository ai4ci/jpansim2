package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleUnaryOperator;

/**
 * A small atomic holder for a double value.
 *
 * <p>
 * This class provides atomic get/set and update operations for a double value
 * by storing the raw long bits used by {@link Double} and delegating to an
 * {@link java.util.concurrent.atomic.AtomicLong}. It mirrors the behaviour of
 * atomic numeric holders but for double primitives and is useful when a single
 * concurrently updated double is required.
 * </p>
 *
 * <p>
 * Downstream uses: see {@link io.github.ai4ci.util.AtomicDoubleArray} for an
 * array based variant and
 * {@link io.github.ai4ci.util.SparseDecomposition.SparseMatrix} which interacts
 * with atomic double arrays in matrix multiplication.
 * </p>
 *
 * @author Rob Challen
 */
public class AtomicDouble implements Serializable {

	private static final long serialVersionUID = 0L;

	/**
	 * Create an AtomicDouble from the given long bits. This is used when
	 * deserialising
	 *
	 * @param l the long bits representing the double value
	 * @return an AtomicDouble initialized with the double value represented by
	 *         the given long bits
	 */
	protected static AtomicDouble fromLong(long l) {
		var bits = new AtomicLong(l);
		return new AtomicDouble(bits);
	}

	private transient AtomicLong bits;

	/**
	 * Create an AtomicDouble initialised to zero.
	 */
	public AtomicDouble() {
		this(0.0);
	}

	private AtomicDouble(AtomicLong bits) {
		this.bits = bits;
	}

	/**
	 * Create an AtomicDouble with the given initial value.
	 *
	 * @param initialValue the initial double value
	 */
	public AtomicDouble(double initialValue) {
		this.bits = new AtomicLong(Double.doubleToRawLongBits(initialValue));
	}

	/**
	 * Atomically add the given delta to the current value and return the updated
	 * value.
	 *
	 * @param delta the amount to add
	 * @return the updated value after the addition
	 */
	public final double addAndGet(double delta) {
		while (true) {
			var current = this.bits.get();
			var currentVal = Double.longBitsToDouble(current);
			var nextVal = currentVal + delta;
			var next = Double.doubleToRawLongBits(nextVal);
			if (this.bits.compareAndSet(current, next)) { return nextVal; }
		}
	}

	/**
	 * Atomically compare and set the value.
	 *
	 * @param expect the expected current value
	 * @param update the new value to set if the current value equals expect
	 * @return true if successful, false otherwise
	 */
	public final boolean compareAndSet(double expect, double update) {
		return this.bits.compareAndSet(
				Double.doubleToRawLongBits(expect),
				Double.doubleToRawLongBits(update)
		);
	}

	/**
	 * Get the current value.
	 *
	 * @return the current double value
	 */
	public final double get() {
		return Double.longBitsToDouble(this.bits.get());
	}

	/**
	 * Atomically add the given delta to the current value and return the
	 * previous value.
	 *
	 * @param delta the amount to add
	 * @return the previous value before the addition
	 */
	public final double getAndAdd(double delta) {
		while (true) {
			var current = this.bits.get();
			var currentVal = Double.longBitsToDouble(current);
			var nextVal = currentVal + delta;
			var next = Double.doubleToRawLongBits(nextVal);
			if (this.bits.compareAndSet(current, next)) { return currentVal; }
		}
	}

	/**
	 * Atomically set to the given value and return the old value.
	 *
	 * @param newValue the new double value
	 * @return the previous value
	 */
	public final double getAndSet(double newValue) {
		return Double.longBitsToDouble(
				this.bits.getAndSet(Double.doubleToRawLongBits(newValue))
		);
	}

	/**
	 * Atomically apply the provided update function and return the previous
	 * value.
	 *
	 * <p>
	 * The update function is applied to the current value and its result is
	 * swapped in when the underlying bits have not changed concurrently.
	 * </p>
	 *
	 * @param updateFunction function to compute the new value from the current
	 *                       value
	 * @return the previous value
	 */
	public final double getAndUpdate(DoubleUnaryOperator updateFunction) {
		while (true) {
			var current = this.bits.get();
			var currentVal = Double.longBitsToDouble(current);
			var nextVal = updateFunction.applyAsDouble(currentVal);
			var next = Double.doubleToRawLongBits(nextVal);
			if (this.bits.compareAndSet(current, next)) { return currentVal; }
		}
	}

	/**
	 * Eventually set to the given value.
	 *
	 * <p>
	 * This is a weaker form of {@link #set(double)} that may not be immediately
	 * visible to other threads.
	 * </p>
	 *
	 * @param newValue the new double value
	 */
	public final void lazySet(double newValue) {
		this.bits.lazySet(Double.doubleToRawLongBits(newValue));
	}

	private void readObject(java.io.ObjectInputStream s)
			throws java.io.IOException, ClassNotFoundException {
		s.defaultReadObject();
		this.bits = new AtomicLong(Double.doubleToRawLongBits(s.readDouble()));
	}

	/**
	 * Set to the given value.
	 *
	 * @param newValue the new double value
	 */
	public final void set(double newValue) {
		this.bits.set(Double.doubleToRawLongBits(newValue));
	}

	/**
	 * Return a string representation of the current value.
	 *
	 * @return the string form of the current double value
	 */
	@Override
	public String toString() {
		return Double.toString(this.get());
	}

	/**
	 * Atomically apply the provided update function and return the updated
	 * value.
	 *
	 * @param updateFunction function to compute the new value from the current
	 *                       value
	 * @return the updated value
	 */
	public final double updateAndGet(DoubleUnaryOperator updateFunction) {
		while (true) {
			var current = this.bits.get();
			var currentVal = Double.longBitsToDouble(current);
			var nextVal = updateFunction.applyAsDouble(currentVal);
			var next = Double.doubleToRawLongBits(nextVal);
			if (this.bits.compareAndSet(current, next)) { return nextVal; }
		}
	}

	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		s.defaultWriteObject();
		s.writeDouble(this.get());
	}
}