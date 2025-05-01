package io.github.ai4ci.abm;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.util.ThreadSafeBuffer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestThreadSafeBuffer {

    private BlockingQueue<Integer> queue;

    @BeforeEach
    void setUp() {
        // queue = new ArrayBlockingQueue<>(2); // Capacity of 2
        queue = new ThreadSafeBuffer<Integer>(Integer.class,2); // Capacity of 2
    }

    @Test
    void testOfferAndPoll() throws InterruptedException {
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertFalse(queue.offer(3)); // Should fail due to capacity

        assertEquals(1, queue.poll());
        assertEquals(2, queue.poll());
        assertNull(queue.poll()); // Should return null if empty
    }

    @Test
    void testPutAndTake() throws InterruptedException {
        queue.put(10);
        queue.put(20);

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500);
                System.out.println(queue.take()); // Take one to allow room
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();

        // This will block until there is room
        queue.put(30);
        t.join();

        assertEquals(20, queue.take());
        assertEquals(30, queue.take());
    }

    @Test
    void testBlockingTake() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(500);
                queue.put(42); // Put after some delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();

        long start = System.currentTimeMillis();
        Integer result = queue.take();
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 500);
        assertEquals(42, result);
        t.join();
    }

    @Test
    void testTimedOffer() throws InterruptedException {
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertFalse(queue.offer(3, 1, TimeUnit.SECONDS)); // Should timeout
    }

    @Test
    void testTimedPoll() throws InterruptedException {
        assertNull(queue.poll(500, TimeUnit.MILLISECONDS)); // Should timeout and return null

        queue.put(99);
        assertEquals(99, queue.poll(500, TimeUnit.MILLISECONDS));
    }
}

