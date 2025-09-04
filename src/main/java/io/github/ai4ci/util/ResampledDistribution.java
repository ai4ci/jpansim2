package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.ModelOperation.BiFunction;

@Value.Immutable
public interface ResampledDistribution extends Abstraction.Distribution,Serializable {
	
	int PRECISION = 10000;
	// int KNOTS = 50;
	
	Abstraction.Distribution getFirst();
	Abstraction.Distribution getSecond();
	BiFunction<Double,Double,Double> getCombiner();
	
	@Value.Derived default double[] getSamples() {
		double[] out = new double[PRECISION];
		for (int i=0;i<PRECISION;i++) {
			out[i] = sample();
		}
		return out;
	}
	 
	@Value.Derived default double getCentral() {
		return Arrays.stream(getSamples()).average().getAsDouble();
	}
	
	@Value.Derived default double getMedian() {
		double[] tmp = getSamples();
		Arrays.sort(tmp);
		return tmp[PRECISION / 2];
	}
	
	public default double sample(Sampler rng) {
		return 
				getCombiner().apply(
					getFirst().sample(rng),
					getSecond().sample(rng)
				);
	}
	
	default double getCumulative(double x) {
		return ((double) Arrays.stream(getSamples()).filter(d -> d<x).count() + 1)/(PRECISION+1);
	}
	
	@Value.Lazy
	default EmpiricalDistribution getInterpolation() {
		return EmpiricalDistribution.fromData(this.getLink(), getSamples());
	}
	
}