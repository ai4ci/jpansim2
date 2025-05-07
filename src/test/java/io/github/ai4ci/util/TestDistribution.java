package io.github.ai4ci.util;

import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;

class TestDistribution {

	static EmpiricalDistribution testAgeDist = ImmutableEmpiricalDistribution.builder()
			.setMinimum(0)
			.setMaximum(120)
			.setX(18,45,65,85)
			.setCumulativeProbability(0.1,0.5,0.75,0.9)
			.build(); 
	
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
				.setX(18,45,65,85)
				.setCumulativeProbability(0.1,0.5,0.75,0.9)
				.build();
		System.out.println(dist.getCentral());
		System.out.println(dist.sample());
		System.out.println(dist.getMedian());
		System.out.println(dist.pLessThan(25));
		
		IntStream.range(0, 120).mapToObj(i -> dist.getDensity(i)+" "+dist.getCumulative(i)).forEach(System.out::println);
		
		IntStream.range(0, 10000).mapToDouble(i -> dist.sample()).average().ifPresent(System.out::println);
		
		RombergIntegrator tmp = new RombergIntegrator(0.0001, 0.0001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
		System.out.println("integral of density:\n"+tmp.integrate(1000, x -> dist.getDensity(x), 0, 120));
		// System.out.println(IntStream.range(0, 120).mapToDouble(i -> dist.getDensity(i)).sum());
	}
	
	@Test
	void testResample() {
		
		ResampledDistribution dist = SimpleDistribution.gamma(5D, 2D).combine(
				SimpleDistribution.beta(0.5, 0.2), (d1,d2) -> d1+d2);
		
		System.out.println(dist.getCentral());
		System.out.println(dist.sample());
		System.out.println(dist.getMedian());
		System.out.println(dist.getInterpolation().getCentral());
		System.out.println(dist.getInterpolation().getMedian());
		System.out.println(dist.pLessThan(2.5));
		
		IntStream.range(0, 10000).mapToDouble(i -> dist.getInterpolation().sample()).average().ifPresent(System.out::println);
	}
	
	@Test
	void testInterp() {
		ImmutableEmpiricalFunction fn = ImmutableEmpiricalFunction.builder()
			.setX(0,10,25,40,60,70)
			.setY(2,0.5,1.5,0.5,1.0,0.5)
			.build();
		
		EmpiricalDistribution testAgeDiffDist = testAgeDist.combine(testAgeDist, (d1,d2) -> Math.abs(d1-d2)).getInterpolation();
		
		EmpiricalFunction fn2 = fn.normalise(testAgeDiffDist);
		
		/*
		 * IntStream.range(0, 80).mapToDouble(i -> fn.interpolate((double) i))
		 * .forEach(System.out::println);
		 */
		
		IntStream.range(0, 80).mapToObj(i -> fn.interpolate((double) i)+" "+fn2.interpolate((double) i))
		.forEach(System.out::println);
	}
}
