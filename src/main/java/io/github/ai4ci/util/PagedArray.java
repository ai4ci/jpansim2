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
 * assumes they happen in different phases.  
 * @param <X>
 */
public class PagedArray<X> implements Serializable {
	
	public static class OutOfSpaceException extends RuntimeException {};
	
	X[] pages = null;
	AtomicInteger pointer;
	
	public static <Y> PagedArray<Y> empty(Class<Y> type) {
		return new PagedArray<Y>(type,0);
	}
	
	@SuppressWarnings("unchecked")
	public PagedArray(Class<X> type, int size) {
		this.pages = (X[]) Array.newInstance(type, size);
		this.pointer = new AtomicInteger(0);
	}
	
	public int put(X value) {
		int p = pointer.getAndIncrement();
		this.pages[p] = value;
		return p;
	}
	
	public X[] finish() {
		int p = this.pointer.get();
		return Arrays.copyOfRange(pages, 0, p);
	}
	
	public X get(int p) {
		if (p > pointer.get()) throw new ArrayIndexOutOfBoundsException();
		return this.pages[p];
	}
	
	public Stream<X> stream() {
		int p = this.pointer.get();
		return IntStream.range(0, p).mapToObj(i -> get(i));
	}
	
	public int size() {
		return pointer.get();
	}
	
}