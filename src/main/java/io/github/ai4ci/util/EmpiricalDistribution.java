package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalDistribution.class)
@JsonDeserialize(as = ImmutableEmpiricalDistribution.class)
public interface EmpiricalDistribution extends Abstraction.Distribution, Serializable {

	double getMinimum();
	double getMaximum();
	double[] getX();
	double[] getCumulativeProbability();
	
	private Map<Double,Double> getCumulativeData() {
		Map<Double,Double> tmp = new HashMap<>();
		if (getX().length != getCumulativeProbability().length) throw new RuntimeException("Mismatched inputs"); 
		for (int i = 0; i < getX().length; i++) {
			tmp.put(getX()[i], getCumulativeProbability()[i]);
		}
		return tmp;
	};
	
	@JsonIgnore
	default double[] getSortedX() {
		int count = getCumulativeData().size();
		double[] x = new double[count+2];
		x[0] = getMinimum();
		Double[] ix = getCumulativeData().keySet().toArray(new Double[count]);
		Arrays.sort(ix);
		for (int i = 0;i<count;i++) {
				x[i+1] = ix[i];
		}
		x[count+1]=getMaximum();
		return x;
	}
	
	@JsonIgnore
	default double[] getY() {
		int count = getCumulativeData().size();
		double[] y = new double[count+2];
		y[0] = 0;
		Double[] ix = getCumulativeData().keySet().toArray(new Double[count]);
		Arrays.sort(ix);
		for (int i = 0;i<count;i++) {
			y[i+1] += getCumulativeData().get(ix[i]);
		}
		if (y[count] > 1) throw new RuntimeException("Cumulative density exceeds 1");
		y[count+1]=1;
		return y;
	}
	
	@JsonIgnore
	default SplineInterpolator getCDF() {
		return SplineInterpolator.createMonotoneCubicSpline(this.getSortedX(), this.getY());
	}
	
	@JsonIgnore
	@Value.Derived default SplineInterpolator getQuantile() {
		return SplineInterpolator.createMonotoneCubicSpline(this.getY(), this.getSortedX());
	}
	
	@JsonIgnore
	public default double sample() {
		Sampler rng = Sampler.getSampler();
		return sample(rng);
	}
	
	@JsonIgnore
	default double sample(Sampler rng) {
		return getQuantile().interpolate(rng.nextDouble());
	}
	
	@JsonIgnore
	@Value.Lazy
	default double getCentral() {
		RombergIntegrator tmp = new RombergIntegrator(0.001, 0.001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
		return tmp.integrate(100000, x -> x*this.getDensity(x), getMinimum(), getMaximum());
//		double x[] = getSortedX();
//		double p[] = getY();
//		double mass = 0;
//		for (int i=1; i<x.length; i++) {
//			double dp = p[i] - p[i-1];
//			mass += (x[i-1]+x[i])/2 * dp;
//		}
//		return mass;
	};
	
	@JsonIgnore
	default double pLessThan(double x) {
		return getCDF().interpolate(x);
	};
	
	@JsonIgnore
	default double getMedian() {
		return getQuantile().interpolate(0.5);
	}
	@JsonIgnore
	default double getDensity(double x) {
		return getCDF().interpolateDifferential(x);
	};
	@JsonIgnore
	default double getCumulative(double x) {
		return getCDF().interpolate(x);
	};
}
