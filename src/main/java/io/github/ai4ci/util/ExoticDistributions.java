package io.github.ai4ci.util;

import java.util.stream.IntStream;

public class ExoticDistributions {

	/**
	 * The distribution of distances on a square grid normalised by length of 
	 * hypoteneuse.
	 * @return
	 */
	public static EmpiricalDistribution getEuclidianDistanceDistribution() {
		Sampler rng = Sampler.getSampler();
		int s = 100000;
		double[] data = new double[s];
		IntStream.range(0, s).parallel().forEach(i -> {
			data[i] = Math.sqrt(
				Math.pow(rng.uniform()-rng.uniform(), 2)+Math.pow(rng.uniform()-rng.uniform(), 2)
			) / Math.sqrt(2);
		});
		return EmpiricalDistribution.fromData(LinkFunction.LOGIT, data);
	}
	
}
