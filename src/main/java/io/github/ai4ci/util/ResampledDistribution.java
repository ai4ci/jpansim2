package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution.Builder;

@Value.Immutable
public interface ResampledDistribution extends Abstraction.Distribution,Serializable {
	
	int PRECISION = 1000;
	
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
	
	default double pLessThan(double x) {
		return ((double) Arrays.stream(getSamples()).filter(d -> d<x).count())/PRECISION;
	}
	
	@Value.Lazy
	default EmpiricalDistribution getInterpolation() {
		double[] tmp = getSamples();
		Arrays.sort(tmp);
		Builder out = ImmutableEmpiricalDistribution.builder();
		out.setMinimum(tmp[0]);
		out.setMaximum(tmp[tmp.length-1]);
		for (int i=1;i<tmp.length-1;i++) {
			out.putCumulative(tmp[i], (double) i/tmp.length);
		}
		return out.build();
	}
	
}