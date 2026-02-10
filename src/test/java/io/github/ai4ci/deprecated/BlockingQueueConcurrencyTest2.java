package io.github.ai4ci.deprecated;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

@Deprecated
class BlockingQueueConcurrencyTest2 {

	static BlockingQueue<Integer> newQueue(int size) {
		//return new ArrayBlockingQueue<>(size);
		return new ThreadSafeBuffer<Integer>(Integer.class,size);
	}
	
    @Test
    void testTakeBlocksUntilElementIsAdded() {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);
        CountDownLatch takeStarted = new CountDownLatch(1);
        CountDownLatch elementTaken = new CountDownLatch(1);
        AtomicReference<Integer> takenValue = new AtomicReference<>();

        Thread takerThread = new Thread(() -> {
            try {
                takeStarted.countDown();
                Integer value = queue.take();
                takenValue.set(value);
                elementTaken.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        takerThread.start();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertTrue(takeStarted.await(1, TimeUnit.SECONDS));
            queue.put(123);
            assertTrue(elementTaken.await(1, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(123), takenValue.get());
        });
    }

    @Test
    void testPutBlocksUntilSpaceIsAvailable() throws Exception {
        BlockingQueue<Integer> queue = newQueue(1);
        queue.put(0); // Fill the queue

        CountDownLatch putStarted = new CountDownLatch(1);
        CountDownLatch putCompleted = new CountDownLatch(1);

        Thread putterThread = new Thread(() -> {
            try {
                putStarted.countDown();
                queue.put(999);
                putCompleted.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        putterThread.start();

        assertTrue(putStarted.await(1, TimeUnit.SECONDS));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            assertEquals(Integer.valueOf(0), queue.take());
            assertTrue(putCompleted.await(1, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(999), queue.take());
        });
    }

    @Test
    void testPollWithTimeoutReturnsNullAfterTimeout() throws Exception {
        BlockingQueue<Integer> queue = newQueue(1);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Integer result = queue.poll(200, TimeUnit.MILLISECONDS);
            assertNull(result);
        });
    }

    @Test
    void testPollWithTimeoutReturnsElementIfAddedBeforeTimeout() throws Exception {
        BlockingQueue<Integer> queue = newQueue(1);

        Thread adderThread = new Thread(() -> {
            try {
                Thread.sleep(100);
                queue.put(456);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        adderThread.start();

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Integer result = queue.poll(500, TimeUnit.MILLISECONDS);
            assertEquals(Integer.valueOf(456), result);
        });

        adderThread.join(1000);
        assertFalse(adderThread.isAlive());
    }

    @Test
    void testOfferWithTimeoutReturnsFalseWhenFull() throws Exception {
        BlockingQueue<Integer> queue = newQueue(1);
        queue.put(0); // Fill the queue

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            boolean added = queue.offer(1, 200, TimeUnit.MILLISECONDS);
            assertFalse(added);
            assertEquals(Integer.valueOf(0), queue.take());
        });
    }

    @Test
    void testOfferWithTimeoutReturnsTrueWhenSpaceBecomesAvailable() throws Exception {
        BlockingQueue<Integer> queue = newQueue(1);
        queue.put(0); // Fill the queue

        Thread offererThread = new Thread(() -> {
            try {
                boolean added = queue.offer(789, 500, TimeUnit.MILLISECONDS);
                assertTrue(added);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        offererThread.start();

        Thread.sleep(100); // Let offerer thread block
        assertEquals(Integer.valueOf(0), queue.take());

        offererThread.join(1000);
        assertFalse(offererThread.isAlive());
        assertEquals(Integer.valueOf(789), queue.take());
    }

    @Test
    void testRemainingCapacityReflectsQueueState() throws Exception {
        BlockingQueue<Integer> queue = newQueue(3);
        assertEquals(3, queue.remainingCapacity());

        queue.put(1);
        assertEquals(2, queue.remainingCapacity());

        queue.put(2);
        assertEquals(1, queue.remainingCapacity());

        queue.take();
        assertEquals(2, queue.remainingCapacity());
    }
}