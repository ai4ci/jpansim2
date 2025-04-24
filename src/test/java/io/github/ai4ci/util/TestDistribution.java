package io.github.ai4ci.util;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class TestDistribution {

	@Test
	void test() {
		System.out.println(
				SimpleDistribution.unimodalBeta(0.99, 0.01).sample()
				);
	}

	
	@Test
	void testConversion() {
		System.out.println(
				Conversions.probabilityFromRate(1.0/10)
				);
	}
	
	@Test
	void testEmpirical() {
		ImmutableEmpiricalDistribution dist = ImmutableEmpiricalDistribution.builder()
				.setMinimum(0)
				.setMaximum(120)
				.putCumulative(18, 0.1)
				.putCumulative(45, 0.5)
				.putCumulative(65, 0.75)
				.putCumulative(85, 0.9)	
				.build();
		System.out.println(dist.getCentral());
		System.out.println(dist.sample());
		System.out.println(dist.getMedian());
		System.out.println(dist.pLessThan(25));
		
		IntStream.range(0, 10000).mapToDouble(i -> dist.sample()).average().ifPresent(System.out::println);
	}
}
