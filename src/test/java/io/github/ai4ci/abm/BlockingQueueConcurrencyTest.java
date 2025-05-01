// ChatGOT special

//package io.github.ai4ci.abm;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//import java.util.Set;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//class BlockingQueueConcurrencyTest {
//
//    private BlockingQueue<Integer> queue;
//    private final int NUM_PRODUCERS = 10;
//    private final int NUM_CONSUMERS = 2;
//    private final int ITEMS_PER_PRODUCER = 1000;
//
//    @BeforeEach
//    void setUp() {
//        queue = new ArrayBlockingQueue<>(100); // smaller capacity to force blocking
//    }
//
//    @Test
//    void testHighConcurrency() throws InterruptedException {
//        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS + NUM_CONSUMERS);
//        Set<Integer> consumedItems = ConcurrentHashMap.newKeySet();
//        final int totalItems = NUM_PRODUCERS * ITEMS_PER_PRODUCER;
//        final int POISON_PILL = -1;
//
//        // Start Consumers
//        for (int i = 0; i < NUM_CONSUMERS; i++) {
//            executor.submit(() -> {
//                try {
//                    while (true) {
//                        Integer item = queue.take();
//                        if (item == POISON_PILL) {
//                            break;
//                        }
//                        consumedItems.add(item);
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            });
//        }
//
//        // Start Producers
//        for (int i = 0; i < NUM_PRODUCERS; i++) {
//            final int producerId = i;
//            executor.submit(() -> {
//                for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
//                    int value = producerId * ITEMS_PER_PRODUCER + j;
//                    try {
//                        queue.put(value);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                    }
//                }
//                for (int k = 0; k < NUM_CONSUMERS; k++) {
//                	 try {
//                         queue.put(POISON_PILL);
//                     } catch (InterruptedException e) {
//                         Thread.currentThread().interrupt();
//                     }
//                }
//            });
//        }
//
//        
//        executor.shutdown();
//        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
//        assertTrue(finished, "Executor did not terminate in time");
//
//        
//
//        // Validation
//        assertEquals(totalItems, consumedItems.size(), "Some items were lost or duplicated");
//    }
//
//
//}
