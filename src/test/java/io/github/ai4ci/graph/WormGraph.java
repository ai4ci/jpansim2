package io.github.ai4ci.graph;

import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;

import io.github.ai4ci.util.PagedArray;

public class WormGraph {
	
	public static void main(String... args) {
		PagedArray<Integer> test = new PagedArray<Integer>(Integer.class, 10000);
		IntStream.range(0, 10000).parallel().forEach(
				i -> test.put(i)
		);
		test.stream().forEach(System.out::println);
		System.out.println(test.size());
		SerializationUtils.roundtrip(test);
		Integer[] arr = test.finish();
	}
	
}
