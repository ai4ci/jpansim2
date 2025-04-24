package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.util.PagedArray;

public class TestMultiThreadList {

	@Test
	void testMulti() {
		PagedArray<Integer> test = new PagedArray<Integer>(Integer.class, 10000);
		IntStream.range(0, 10000).parallel().forEach(
				i -> test.put(i)
		);
		test.stream().forEach(System.out::println);
		System.out.println(test.size());
		SerializationUtils.roundtrip(test);
		Integer[] arr = test.finish();
		System.out.println(Arrays.toString(arr));
	}
	
}
