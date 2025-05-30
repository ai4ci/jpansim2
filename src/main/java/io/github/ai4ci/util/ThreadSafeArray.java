package io.github.ai4ci.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A write once read many times list like data structure. This allows
 * multi-threaded writing to the array and multi-threaded reading and 
 * largely assumes they happen in different phases.  
 * @param <X> the array type
 */
public class ThreadSafeArray<X> implements Serializable {
	
	public static class OutOfSpaceException extends RuntimeException {};
	
	X[] data = null;
	AtomicInteger pointer;
	
	public static <Y> ThreadSafeArray<Y> empty(Class<Y> type) {
		return new ThreadSafeArray<Y>(type,0);
	}
	
	@SuppressWarnings("unchecked")
	public ThreadSafeArray(Class<X> type, int size) {
		synchronized(this) {
			this.pointer = new AtomicInteger(0);
			this.data = (X[]) Array.newInstance(type, size);
		}
	}
	
	public int put(X value) {
		int p = pointer.getAndIncrement();
		this.data[p] = value;
		return p;
	}
	
	public X[] finish() {
		int p = this.pointer.get();
		return Arrays.copyOfRange(data, 0, p);
	}
	
	public X get(int p) {
		if (p > pointer.get()) throw new ArrayIndexOutOfBoundsException();
		return this.data[p];
	}
	
	public Stream<X> stream() {
		int p = this.pointer.get();
		return IntStream.range(0, p).mapToObj(i -> get(i));
	}
	
	public Stream<X> parallelStream() {
		int p = this.pointer.get();
		return IntStream.range(0, p).parallel().mapToObj(i -> get(i));
	}
	
	public int size() {
		return pointer.get();
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
}