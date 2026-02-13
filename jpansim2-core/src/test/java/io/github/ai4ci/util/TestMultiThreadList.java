package io.github.ai4ci.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class TestMultiThreadList {

	@Test
	void testMulti() {
		ThreadSafeArray<Integer> test = ThreadSafeArray.empty(Integer.class);
		IntStream.range(0, 1000).parallel().forEach(
				i -> {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					test.put(i);
				}
		);
		Integer[] arr = test.finish();
		// test.stream().forEach(System.out::println);
		// System.out.println(test.size());
		// SerializationUtils.roundtrip(test);
		
		for (Integer i: arr) if(i==null) throw new RuntimeException("Null i");
		System.out.println(arr.length);
		assertEquals(test.size(), arr.length);
		//System.out.println(Arrays.toString(arr));
	}

	private static final int THREAD_COUNT = 16;
	private static final int ELEMENTS_PER_THREAD = 1000;

	@Test
	void testSingleThreaded() {
		ThreadSafeArray<Integer> arr = ThreadSafeArray.empty(Integer.class);
		for (int i = 0; i < 100; i++) {
			arr.put(i);
		}
		Integer[] result = arr.finish();
		assertEquals(100, result.length);
		for (int i = 0; i < 100; i++) {
			assertEquals(i, result[i]);
		}
	}
	
//	@Test
//	void testSingleThreadedSet() {
//		ThreadSafeArray<Integer> arr = ThreadSafeArray.empty(Integer.class);
//		arr.set(99, 99, i -> i);
//		Integer[] result = arr.finish();
//		assertEquals(100, result.length);
//		for (int i = 0; i < 100; i++) {
//			assertEquals(i, result[i]);
//		}
//	}

	@Test
	void testConcurrentWritesFixedSize() throws Exception {
		int total = THREAD_COUNT * ELEMENTS_PER_THREAD;
		ThreadSafeArray<Integer> arr = new ThreadSafeArray<>(Integer.class, total);

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		List<Future<?>> futures = new ArrayList<>();

		for (int t = 0; t < THREAD_COUNT; t++) {
			int offset = t * ELEMENTS_PER_THREAD;
			futures.add(executor.submit(() -> {
				for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
					arr.put(offset + i);
				}
			}));
		}

		for (Future<?> f : futures) {
			f.get(); // wait
		}

		executor.shutdown();
		assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

		Integer[] result = arr.finish();
		assertEquals(total, result.length);

		Set<Integer> resultSet = new HashSet<>(Arrays.asList(result));
		assertEquals(total, resultSet.size()); // No duplicates
	}

	@Test
	void testConcurrentResize() throws Exception {
		ThreadSafeArray<Integer> arr = ThreadSafeArray.empty(Integer.class);

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		List<Future<?>> futures = new ArrayList<>();

		for (int t = 0; t < THREAD_COUNT; t++) {
			futures.add(executor.submit(() -> {
				for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
					arr.put(ThreadLocalRandom.current().nextInt());
				}
			}));
		}

		for (Future<?> f : futures) {
			f.get();
		}

		executor.shutdown();
		assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));

		Integer[] result = arr.finish();
		assertEquals(THREAD_COUNT * ELEMENTS_PER_THREAD, result.length);
	}

	@Test
	void testFinishWaitsForAllWrites() throws Exception {
	    ThreadSafeArray<Integer> arr = ThreadSafeArray.empty(Integer.class);

	    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
	    List<Future<?>> writerFutures = new ArrayList<>();

	    // Start all writers
	    for (int t = 0; t < THREAD_COUNT; t++) {
	        writerFutures.add(executor.submit(() -> {
	            for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
	                arr.put(ThreadLocalRandom.current().nextInt());
	            }
	        }));
	    }

	    // Wait for all writers to finish
	    for (Future<?> f : writerFutures) {
	        f.get(); // ensures no exceptions were thrown
	    }

	    // Now call finish()
	    Integer[] result = arr.finish();

	    // Validate
	    int expectedTotal = THREAD_COUNT * ELEMENTS_PER_THREAD;
	    assertEquals(expectedTotal, result.length);

	    Set<Integer> uniqueValues = new HashSet<>(Arrays.asList(result));
	    assertEquals(expectedTotal, uniqueValues.size()); // No duplicates or lost writes
	}

	@Test
	void testStreamConsistency() throws Exception {
		ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		for (int t = 0; t < THREAD_COUNT; t++) {
			executor.submit(() -> {
				for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
					arr.put("value-" + Thread.currentThread().getName() + "-" + i);
				}
			});
		}

		executor.shutdown();
		assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

		String[] result = arr.finish();
		Stream<String> stream = arr.stream();
		long count = stream.count();
		assertEquals(result.length, count);
	}
	
	@Test
	void testPutAfterFinishThrows() {
	    ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);
	    arr.finish(); // lock it

	    assertThrows(ThreadSafeArray.ReadOnlyException.class, () -> {
	        arr.put("should fail");
	    });
	}
	
//	@Test
//    void testConcurrentSetAndPut() throws Exception {
//        ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);
//
//        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
//        List<Future<?>> futures = new ArrayList<>();
//
//        // Writers using put()
//        for (int t = 0; t < THREAD_COUNT / 2; t++) {
//            futures.add(executor.submit(() -> {
//                for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
//                    arr.put("put-" + i);
//                }
//            }));
//        }
//
//        // Writers using set()
//        for (int t = 0; t < THREAD_COUNT / 2; t++) {
//            final int threadNum = t;
//            futures.add(executor.submit(() -> {
//                for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
//                    int idx = threadNum * ELEMENTS_PER_THREAD + i + 1000;
//                    arr.set(idx, "set-" + idx, j -> "fill-" + j);
//                }
//            }));
//        }
//
//        for (Future<?> f : futures) {
//            f.get(); // wait
//        }
//
//        executor.shutdown();
//        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
//
//        String[] result = arr.finish();
//
//        // Verify put values exist
//        for (int i = 0; i < (THREAD_COUNT / 2) * ELEMENTS_PER_THREAD; i++) {
//        	if (result[i] == null) {
//        		System.out.println(i+": "+Arrays.toString(Arrays.copyOfRange(result, i-5,i+5)));
//        	}
//            //assertNotNull(result[i]);
//            //assertTrue(result[i].startsWith("put-"));
//        }
//
//        // Verify set values exist
//        for (int t = 0; t < THREAD_COUNT / 2; t++) {
//            for (int i = 0; i < ELEMENTS_PER_THREAD; i++) {
//                int idx = t * ELEMENTS_PER_THREAD + i + 1000;
//                assertNotNull(result[idx]);
//                assertTrue(result[idx].startsWith("set-") || result[idx].startsWith("fill-"));
//            }
//        }
//    }
//
//    @Test
//    void testSetAfterPutOnSameIndex() throws Exception {
//        ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);
//
//        int index = 10;
//
//        CountDownLatch latch = new CountDownLatch(2);
//
//        Thread t1 = new Thread(() -> {
//            arr.put("first");
//            latch.countDown();
//        });
//
//        Thread t2 = new Thread(() -> {
//            while (arr.size() < index) {} // wait until pointer reaches index
//            arr.set(index, "second", j -> "filler");
//            latch.countDown();
//        });
//
//        t1.start();
//        t2.start();
//
//        latch.await();
//        String[] result = arr.finish();
//
//        // Depending on timing, either value may be present
//        assertTrue("first".equals(result[index]) || "second".equals(result[index]));
//    }
//
//    @Test
//    void testSetWithLowerIndexAfterPut() throws Exception {
//        ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);
//
//        arr.put("init");
//
//        // Set at index 0 — should overwrite safely
//        arr.set(0, "overwrite", j -> "filler");
//
//        String[] result = arr.finish();
//        assertEquals("overwrite", result[0]);
//    }
//
//    @Test
//    void testFinishReturnsAllWrittenValues() throws Exception {
//        ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);
//
//        arr.put("a");
//        arr.set(5, "b", j -> "fill-" + j);
//        arr.put("c");
//
//        String[] result = arr.finish();
//
//        assertEquals("a", result[0]);
//        assertEquals("fill-1", result[1]);
//        assertEquals("fill-2", result[2]);
//        assertEquals("fill-3", result[3]);
//        assertEquals("fill-4", result[4]);
//        assertEquals("b", result[5]);
//        assertEquals("c", result[6]);
//    }

//    @Test
//    void testSparseSetAndGet() throws Exception {
//        ThreadSafeArray<String> arr = ThreadSafeArray.empty(String.class);
//
//        arr.set(100, "value", j -> "default-" + j);
//
//        assertEquals("default-0", arr.get(0));
//        assertEquals("default-99", arr.get(99));
//        assertEquals("value", arr.get(100));
//
//        String[] result = arr.finish();
//        assertEquals("value", result[100]);
//    }
}


