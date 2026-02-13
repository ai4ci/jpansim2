package io.github.ai4ci.util;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.functions.EmpiricalDistribution;

public class TestEmpirical {

	
	@Test
	void testEmpirical() {
		
		double[] samples = IntStream.range(0, 100000).mapToDouble( 
					i -> Sampler.getSampler().normal(5, 2)
		).toArray();
		
		EmpiricalDistribution dist = EmpiricalDistribution.fromData(samples);
		
		System.out.println(dist.getMean());
		System.out.println(dist.getCumulative(5));
		System.out.println(dist.getCumulative(8));
		System.out.println(dist.getQuantile(0.5));
		System.out.println(dist.getQuantile(0.95));
		
	}
}
