package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution.Builder;

@Value.Immutable
public interface ResampledDistribution extends Abstraction.Distribution,Serializable {
	
	int PRECISION = 10000;
	int KNOTS = 50;
	
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
		int step = tmp.length / KNOTS;
		double[] x = new double[KNOTS-1];
		double[] y = new double[KNOTS-1];
		for (int i = 1; i<=KNOTS-1; i++) {
			x[i-1] = tmp[i*step];
			y[i-1] = ((double) i*step)/tmp.length;
		}
		out.setX(x);
		out.setCumulativeProbability(y);
		return out.build();
	}
	
}