package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleUnaryOperator;

/**
 * A small atomic holder for a double value.
 *
 * <p>This class provides atomic get/set and update operations for a
 * double value by storing the raw long bits used by {@link Double} and
 * delegating to an {@link java.util.concurrent.atomic.AtomicLong}.
 * It mirrors the behaviour of atomic numeric holders but for double
 * primitives and is useful when a single concurrently updated double is
 * required.</p>
 *
 * <p>Downstream uses: see {@link io.github.ai4ci.util.AtomicDoubleArray} for
 * an array based variant and {@link io.github.ai4ci.util.SparseDecomposition.SparseMatrix}
 * which interacts with atomic double arrays in matrix multiplication.</p>
 *
 * @author Rob Challen
 */
public class AtomicDouble implements Serializable {

    private static final long serialVersionUID = 0L;

    private transient AtomicLong bits;
    
    private AtomicDouble(AtomicLong bits) {
		this.bits = bits;
	}

    /**
     * Create an AtomicDouble initialised to zero.
     */
    public AtomicDouble() {
        this(0.0);
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
     * Get the current value.
     *
     * @return the current double value
     */
    public final double get() {
        return Double.longBitsToDouble(bits.get());
    }

    /**
     * Set to the given value.
     *
     * @param newValue the new double value
     */
    public final void set(double newValue) {
        bits.set(Double.doubleToRawLongBits(newValue));
    }
    
    /**
     * Eventually set to the given value.
     *
     * <p>This is a weaker form of {@link #set(double)} that may not be
     * immediately visible to other threads.</p>
     *
     * @param newValue the new double value
     */
    public final void lazySet(double newValue) {
        bits.lazySet(Double.doubleToRawLongBits(newValue));
    }
    
    /**
     * Atomically set to the given value and return the old value.
     *
     * @param newValue the new double value
     * @return the previous value
     */
    public final double getAndSet(double newValue) {
        return Double.longBitsToDouble(bits.getAndSet(Double.doubleToRawLongBits(newValue)));
    }
    
    /**
     * Atomically compare and set the value.
     *
     * @param expect the expected current value
     * @param update the new value to set if the current value equals expect
     * @return true if successful, false otherwise
     */
    public final boolean compareAndSet(double expect, double update) {
        return bits.compareAndSet(Double.doubleToRawLongBits(expect), Double.doubleToRawLongBits(update));
    }
    
    /**
     * Atomically add the given delta to the current value and return
     * the previous value.
     *
     * @param delta the amount to add
     * @return the previous value before the addition
     */
    public final double getAndAdd(double delta) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = currentVal + delta;
            long next = Double.doubleToRawLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return currentVal;
            }
        }
    }
    
    /**
     * Atomically add the given delta to the current value and return
     * the updated value.
     *
     * @param delta the amount to add
     * @return the updated value after the addition
     */
    public final double addAndGet(double delta) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = currentVal + delta;
            long next = Double.doubleToRawLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return nextVal;
            }
        }
    }
    
    /**
     * Atomically apply the provided update function and return the
     * previous value.
     *
     * <p>The update function is applied to the current value and its
     * result is swapped in when the underlying bits have not changed
     * concurrently.</p>
     *
     * @param updateFunction function to compute the new value from the current value
     * @return the previous value
     */
    public final double getAndUpdate(DoubleUnaryOperator updateFunction) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = updateFunction.applyAsDouble(currentVal);
            long next = Double.doubleToRawLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return currentVal;
            }
        }
    }
    
    /**
     * Atomically apply the provided update function and return the
     * updated value.
     *
     * @param updateFunction function to compute the new value from the current value
     * @return the updated value
     */
    public final double updateAndGet(DoubleUnaryOperator updateFunction) {
        while (true) {
            long current = bits.get();
            double currentVal = Double.longBitsToDouble(current);
            double nextVal = updateFunction.applyAsDouble(currentVal);
            long next = Double.doubleToRawLongBits(nextVal);
            if (bits.compareAndSet(current, next)) {
                return nextVal;
            }
        }
    }
    
    /**
     * Return a string representation of the current value.
     *
     * @return the string form of the current double value
     */
    public String toString() {
        return Double.toString(get());
    }
    
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        bits = new AtomicLong(Double.doubleToRawLongBits(s.readDouble()));
    }
    
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        s.defaultWriteObject();
        s.writeDouble(get());
    }

	protected static AtomicDouble fromLong(long l) {
		AtomicLong bits = new AtomicLong(l);
		return new AtomicDouble(bits);
	}
}