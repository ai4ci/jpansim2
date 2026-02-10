package io.github.ai4ci.util;

import java.util.stream.IntStream;

import io.github.ai4ci.functions.EmpiricalDistribution;
import io.github.ai4ci.functions.LinkFunction;

/**
 * Utility class for generating and working with exotic distributions.
 *
 * <p>Currently includes a method for generating the distribution of distances on a square grid
 * normalised by the length of the hypoteneuse, which can be used for modelling spatial interactions
 * in a grid-based environment.
 *
 * @author Rob Challen
 */
public class ExoticDistributions {

	/**
	 * The distribution of distances on a square grid normalised by length of 
	 * hypoteneuse. This distribution arises when considering the distances between points uniformly
	 * distributed on a unit square, and normalising by the maximum distance (the length of
	 * the hypoteneuse, which is sqrt(2)). The distribution can be used for modelling spatial interactions
	 * in a grid-based environment, where the distance between points is relevant for interaction probabilities.
	 * 
	 * @return an empirical distribution representing the normalised distance distribution on a square grid
	 * 
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
