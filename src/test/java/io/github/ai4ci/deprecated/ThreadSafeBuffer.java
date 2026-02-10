package io.github.ai4ci.deprecated;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.NotImplementedException;

/**
 * A circular buffer that can be written to by many threads and read from
 * primarily by one thread. Will block putting thread if the buffer is full. 
 * @param <X> the buffer type
 */
@Deprecated
public class ThreadSafeBuffer<X> implements Serializable, BlockingQueue<X> {
	
	Class<X> type;
	X[] data = null;
	int capacity;
	AtomicInteger next;
	AtomicInteger tail;
	
	@SuppressWarnings("unchecked")
	public ThreadSafeBuffer(Class<X> type, int size) {
		this.data = (X[]) Array.newInstance(type, size);
		this.capacity = size;
		this.type = type;
		this.next = new AtomicInteger(0);
		this.tail = new AtomicInteger(0);
	}
	
	public void put(X value) {
		int p = next.getAndIncrement();
		while (p-tail.get() == capacity) Thread.onSpinWait();
		this.data[p % capacity] = value;
		if (tail.get() > capacity*100) {
			int a = tail.get();
			int diff = a - (a % capacity);
			synchronized(tail) {
				tail.updateAndGet(t -> t-diff);
				next.updateAndGet(h -> h-diff);
			}
		}
	}
	
	public boolean isEmpty() {
		return next.get() == tail.get();
	}
	
	public X poll() {
		if (isEmpty()) return null;
		int p = tail.getAndIncrement();
		return this.data[p % capacity];
	}
	
	public X next() {
		if (isEmpty()) throw new NoSuchElementException();
		int p = tail.getAndIncrement();
		return this.data[p % capacity];
	}
	
	public int size() {
		return next.get() - tail.get();
	}

	public X[] toArray() {
		int b = next.get();
		int a = tail.get();
		return toArray(a,b);
	}
	
	private X[] toArray(int a, int b) {
		@SuppressWarnings("unchecked")
		X[] out = (X[]) Array.newInstance(type, b - a);
		for (int i = a; i < b; i++) {
			out[i-a] = data[i % capacity];
		}
		return out;
	}
	
	@Override
	public boolean contains(Object o) {
		throw new NotImplementedException();
	}

	@Override
	public Iterator<X> iterator() {
		return Arrays.asList(toArray()).iterator();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new NotImplementedException();
	}

	@Override
	public boolean remove(Object o) {
		throw new NotImplementedException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new NotImplementedException();
	}

	@Override
	public boolean addAll(Collection<? extends X> c) {
		c.forEach(this::add);
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new NotImplementedException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new NotImplementedException();
	}

	@Override
	public void clear() {
		synchronized(this) {
			this.next.set(0);
			this.tail.set(0);
		}
	}

	@Override
	public boolean add(X e) {
		return offer(e);
	}

	@Override
	public boolean offer(X e) {
		synchronized(next) {
			if (next.get() - tail.get() < capacity) {
				int p = next.getAndIncrement();
				this.data[p % capacity] = e;
				next.notify(); // notify one
				return true;
			}
			return false;
		}
	}

	@Override
	public X remove() {
		if (this.isEmpty()) throw new NoSuchElementException();
		return poll();
	}

	@Override
	public X element() {
		if (this.isEmpty()) throw new NoSuchElementException();
		int p = tail.get();
		return data[p % capacity];
	}

	@Override
	public X peek() {
		if (this.isEmpty()) return null;
		int p = tail.get();
		return data[p % capacity];
	}

	@Override
	public boolean offer(X e, long timeout, TimeUnit unit) throws InterruptedException {
		return offer(e, System.currentTimeMillis()+unit.toMillis(timeout) );
	}
		
	private boolean offer(X e, long before) throws InterruptedException {
		if (System.currentTimeMillis() > before) return false;
		if (offer(e)) return true;
		Thread.sleep(1);
		return offer(e,before);
	}

	@Override
	public X take() throws InterruptedException {
		while(isEmpty()) Thread.onSpinWait();
		return poll();
	}

	@Override
	public X poll(long timeout, TimeUnit unit) throws InterruptedException {
		return poll(System.currentTimeMillis()+unit.toMillis(timeout));
	}
	
	private X poll(long before) throws InterruptedException {
		while (this.isEmpty() && System.currentTimeMillis() < before) Thread.onSpinWait();
		return poll();
	}

	@Override
	public int remainingCapacity() {
		return capacity-size();
	}

	@Override
	public int drainTo(Collection<? super X> c) {
		int b = next.get();
		int a = tail.getAndSet(b);
		X[] tmp = toArray(a,b);
		c.addAll(Arrays.asList(tmp));
		return 0;
	}

	@Override
	public int drainTo(Collection<? super X> c, int maxElements) {
		int b = next.get();
		int a = tail.getAndUpdate(a2 -> Math.min(a2+maxElements, b));
		X[] tmp = toArray(a, Math.min(a+maxElements, b) );
		c.addAll(Arrays.asList(tmp));
		return maxElements;
	}
	
}