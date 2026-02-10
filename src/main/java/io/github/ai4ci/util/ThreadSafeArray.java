package io.github.ai4ci.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A thread-safe, dynamically growing array designed for a write-once-read-many (WORM) pattern.
 *
 * <p>Writes can be performed concurrently from multiple threads using {@link #put(Object)}.
 * Once all writes are complete, call {@link #finish()} to seal the array and obtain an immutable,
 * fixed-size snapshot for reading.</p>
 *
 * <p>This structure assumes two distinct phases:
 * <ol>
 *   <li><strong>Writing phase:</strong> Multiple threads may call {@link #put(Object)} or {@link #update(int,Object)} concurrently.</li>
 *   <li><strong>Reading phase:</strong> After calling {@link #finish()}, only read operations are allowed via
 *       {@link #get(int)}, or streaming methods like {@link #stream()} and {@link #parallelStream()}.</li>
 * </ol>
 *
 * <p>The array is dynamically resized if needed during writes. Resizing is rare and synchronized internally,
 * but has minimal impact on throughput when the final size is preallocated.</p>
 *
 * <p>{@link #finish()} blocks until all pending writes have completed before returning the final array.</p>
 *
 * @param <X> the type of elements stored in this array
 * @author Rob Challen
 */
public class ThreadSafeArray<X> implements Serializable {

	/**
	 * Thrown when attempting to write to a {@link ThreadSafeArray} after it has been finalized with
	 * a call to {@link #finish()}.
	 */
	public static class ReadOnlyException extends RuntimeException {}

	/**
	 * Thrown when attempting to write to a {@link ThreadSafeArray} while it is being resized or finalized.
	 * This exception is not currently used, but reserved for future internal use.
	 */
	public static class WriteOnlyException extends RuntimeException {}

	//volatile boolean[] writtenItems;
	volatile X[] data = null;
	// points at the first empty space
	volatile AtomicInteger pointer;
	// counts the number of written items
	volatile AtomicInteger written;
	// the array is cannot be appended to
	volatile boolean locked = false;
	// the array is busy doing a bulk operation. Appends or updates will be onSpinWaited.
	volatile AtomicBoolean updating = new AtomicBoolean();

	/**
	 * Creates an empty, thread-safe array of the given type and zero initial capacity.
	 *
	 * @param type the runtime class of the array elements
	 * @param <Y>  the element type
	 * @return a new empty ThreadSafeArray
	 */
	public static <Y> ThreadSafeArray<Y> empty(Class<Y> type) {
		return new ThreadSafeArray<Y>(type,0);
	}

	/**
	 * Constructs a new thread-safe array with the given element type and initial capacity.
	 *
	 * <p>If the final size is known ahead of time, it is recommended to preallocate the array
	 * to minimize resizing overhead during concurrent writes.</p>
	 *
	 * @param type the runtime class of the array elements
	 * @param size the initial capacity of the array
	 */
	@SuppressWarnings("unchecked")
	public ThreadSafeArray(Class<X> type, int size) {
		this.updating.set(true);
		this.pointer = new AtomicInteger(0);
		this.written = new AtomicInteger(0);
		this.data = (X[]) Array.newInstance(type, size);
		//this.writtenItems = new boolean[size];
		this.updating.set(false);
	}

	/**
	 * Adds an element to the array.
	 *
	 * <p>This method is safe to call from multiple threads concurrently.
	 * If the internal array needs resizing, only one thread will perform the resize operation
	 * while others wait.</p>
	 *
	 * @param value the element to add
	 * @return the index at which the element was inserted
	 * @throws ReadOnlyException if called after {@link #finish()}
	 */
	public int put(X value) {
		if (locked) throw new ReadOnlyException();
		while (updating.get()) Thread.onSpinWait();
		if (locked) throw new ReadOnlyException();
		int p = pointer.getAndIncrement();
		ensureCapacityOrWait(p);
		this.data[p] = value;
		//this.writtenItems[p] = true;
		written.getAndIncrement();
		return p;
	}

	// internal utility method
	// p must be from an atomicInteger.incrementAndGet() this means it is 
	// definitely unique across all threads:
	// This may be a cause for contention as multiple things can be waiting for
	// increase in size. We mitigate this a bit by doubling the largest needed 
	// size so hopefully not too much waiting will be incurred. Potentially
	// we could think about a wait/notify on "updating".
	private void ensureCapacityOrWait(int p) {
		
		while (p >= data.length) {
			// many threads may be longer than data.length:
			// try and grab updating flag:	
			if (updating.compareAndSet(false, true)) {
				// OK. just one thread can be here. 
				if (locked) throw new ReadOnlyException();
				// Updating flag is set to true so no writes will occur whilst we
				// update.
				int newCapacity = Math.max(data.length * 2, pointer.get() * 2 + 16);
				// debug System.out.println(newCapacity);
				//writtenItems = Arrays.copyOf(writtenItems, newCapacity);
				data = Arrays.copyOf(data, newCapacity);
				updating.set(false);
				// updating.notifyAll();
				
			} else {
				// park other threads that are trying to ensure capacity
				while (p >= data.length && updating.get()) Thread.onSpinWait();
			}
			// capacity is large enough or updates have finished (and capacity is still too small).
		}
		// capacity if large enough
	}
	
	/**
	 * Finalizes the array size, preventing further additions and returning a 
	 * read / update only snapshot of the contents.
	 *
	 * <p>This method blocks until all concurrent writes in progress have completed.</p>
	 *
	 * <p>After calling this method, any further calls to {@link #put(Object)} will throw
	 * a {@link ReadOnlyException}.</p>
	 *
	 * @return a fixed-size copy of the array containing all elements
	 */
	public X[] finish() {
		while (!updating.compareAndSet(false, true)) Thread.onSpinWait();
		// This thread is updating. No more additions should be made.
		// waits for any lagging writer threads to catch up
		while (this.written.get() < this.pointer.get()) Thread.onSpinWait();
		
		// Ensure all threads see the 'locked' update before proceeding
		synchronized (this) {
			
			int p = this.pointer.get();
			data = Arrays.copyOfRange(data, 0, p);
			//writtenItems = Arrays.copyOfRange(writtenItems, 0, p);
			locked = true; // prevent further writes
			updating.set(false);
			// Writer threads that were added after finish was called will 
			// now throw a ReadOnlyException.
			return data;
		}
		
	}

	/**
	 * Retrieves the element at the specified index. Waiting if necessary until 
	 * it is available.
	 *
	 * <p>If called before {@link #finish()}, this method throws an
	 * {@link ArrayIndexOutOfBoundsException} if the element does not yet exist.</p>
	 *
	 * <p>Callers should ensure that the index is within bounds as reported by
	 * current write progress (e.g., via {@link #size()}).</p>
	 *
	 * @param p index of the element to retrieve
	 * @return the element at the given index
	 */
	public X get(int p) {
		if (!locked) throw new WriteOnlyException();
		if (p >= pointer.get()) throw new ArrayIndexOutOfBoundsException();
		return this.data[p];
	}

	/**
	 * Returns a sequential stream of the elements currently visible for reading.
	 *
	 * <p>The stream reflects the ideal state of the array at the time of the call.
	 * It may have to wait for all items to become available.</p>
	 *
	 * @return a sequential stream of the array contents
	 */
	public Stream<X> stream() {
		if (!locked) throw new WriteOnlyException();
		int p = this.pointer.get();
		return IntStream.range(0, p).mapToObj(i -> get(i));
	}

	/**
	 * Returns a parallel stream of the elements currently visible for reading.
	 *
	 * <p>Like {@link #stream()}, this reflects the ideal state of the array at the time of the call.
	 * It may have to wait for items to become fully available.</p>
	 *
	 * @return a parallel stream of the array contents
	 */
	public Stream<X> parallelStream() {
		if (!locked) throw new WriteOnlyException();
		int p = this.pointer.get();
		return IntStream.range(0, p).parallel().mapToObj(i -> get(i));
	}

	/**
	 * Returns the number of elements successfully written so far.
	 *
	 * <p>This value may be less than the actual array capacity due to dynamic resizing.</p>
	 *
	 * @return the current count of written elements
	 */
	public int size() {
		return pointer.get();
	}

	/**
	 * Checks whether no elements have been written yet.
	 *
	 * @return true if size() == 0
	 */
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Updates the value at the specified index, assuming the value already 
	 * exists, and the initial loading phase is complete but the 
	 * array has not been finalised. 
	 *
	 * <p>This method is thread-safe and supports concurrent writes to different indices.</p>
	 *
	 * @param index the index at which to update the element
	 * @param value the element to update
	 * @throws ReadOnlyException if called after the array has been finalized
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
	public void update(int index, X value) {
		if (locked) throw new ReadOnlyException();
		// Wait for any ongoing resize
		while (updating.get()) Thread.onSpinWait();
		// Write the value, will throw index out of bounds if index invalid
		if (index >= pointer.get()) throw new IndexOutOfBoundsException();
		//while (!writtenItems[index]) Thread.onSpinWait();
		// Array update is atomic, so no need for additional synchronization here
		this.data[index] = value;
		//redundant writtenItems.set(index);
	}


//	/**
//	 * Sets the value at the specified index, filling all missing intermediate indices
//	 * using the provided function. This method blocks other writes while executing.
//	 *
//	 * <p>If called after {@link #finish()} has been invoked, this method throws
//	 * a {@link ReadOnlyException}.</p>
//	 *
//	 * @param i the index at which to insert the value
//	 * @param value the element to insert
//	 * @param fillMissing function used to populate any gaps between current size and i
//	 * @throws ReadOnlyException if called after the array has been finalized
//	 */
//	public boolean set(int i, X value, Function<Integer, X> fillMissing) {
//		if (locked) throw new ReadOnlyException();
//		while (updating.get()) Thread.onSpinWait();
//		if (i < pointer.get()) {
//			// something else has already written or is about to write to this index
//			// wait for initial value to be written. This set value will not 
//			// be overwritten by a delayed put() call from another thread.
//			
//			// THIS CHECK CREATES A DEADLOCK IT SEEMS:
//			while (!writtenItems[i]) Thread.onSpinWait(); 
//			data[i] = value;
//			writtenItems[i]=true;
//			return true;
//		}
//		
//		// we are trying to write to somewhere that does not / did not yet 
//		// exist. but in the meantime it might have been created. We
//		// can make sure there is enough space anyway.
//		ensureCapacityOrWait(i);
//		
//		// check the index we are setting has still not been set:
//		while (!writtenItems[i]) {
//			
//			// We need to find the start of the range we are going to fill.
//			// bearing in mind that other threads may be trying to do the same
//			// thing:
//			int p = pointer.get();
//			// this will keep trying until the pointer can be safely be got
//			// and guarantees that only this one thread has bagged this block of numbers
//			// we do an last check to make sure we need to fill 
//			if (p<i+1 && pointer.compareAndSet(p, i+1)) {
//				// this thread owns the range p->i inclusive
//				// we can fill it and exit the function
//				for (int j = p; j < i; j++) {
//					data[j] = fillMissing.apply(j);
//					writtenItems[j]=true;
//					written.incrementAndGet();
//				}
//				data[i] = value;
//				writtenItems[i]=true;
//				written.incrementAndGet();
//				return true;
//			}
//			// else we failed to reserve the range p->i. Possibly something else
//			// has and is about to write to it, alternatively something else
//			// may have got p->p+10 and we need to reserve p+11->i. We can 
//			// try again but we need to make sure i was not written to by 
//			// something else in the meantime;
//		}
//		
//		// if we got here something else wrote to i before us, expected behaviour 
//		// is undefined. 
//		return false;
//	}
}