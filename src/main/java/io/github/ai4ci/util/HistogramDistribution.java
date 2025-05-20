package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.util.ImmutableHistogramDistribution.Builder;

@Value.Immutable
@JsonSerialize(as = ImmutableHistogramDistribution.class)
@JsonDeserialize(as = ImmutableHistogramDistribution.class)
public interface HistogramDistribution extends Abstraction.Distribution, Serializable {

	double[] getSamples();
	
	@JsonIgnore
	@Value.Derived default double[] getX() {
		double[] samples = getSamples();
		Arrays.sort(samples);
		return samples;
	}
	
	@JsonIgnore
	@Value.Derived default double getMin() {
		return getX()[0];
	}
	
	@JsonIgnore
	@Value.Derived default double getMax() {
		return getX()[getX().length-1];
	}
	
	private double getY(int index) {
		// minus 1 so max data value has P = 1; Y is P(X <= x)
		return ((double) index)/(getSamples().length - 1);
	}
	
	private int findIndexForX(double x) {
		// index of last x value that is smaller than x by binary search or -1 if they are all larger than x
		int top = getX().length-1;
		int bottom = 0;
		if (x < getX()[bottom]) return -1;
		if (x == getX()[bottom]) return bottom;
		if (x >= getX()[top]) return top;
		while (bottom < top-1) {
			int mid = (bottom+top)/2;
			if (x > getX()[mid]) bottom = mid;
			else if (x < getX()[mid]) top = mid;
			else return mid;
		}
		return bottom;
	}
	
	private double interpolateX(double x) {
		int index = findIndexForX(x);
		if (index == -1) return getMin();
		if (index == getX().length-1) return getMax();
		double x0 = getX()[index];
		if (x0 == x) return getY(index);
		double x1 = getX()[index+1];
		if (x1 == x) return getY(index+1); // this shoudl not happen 
		return getY(index) + (x - x0)/(x1-x0) * (getY(index+1)-getY(index));
	}
	
	private double gradientX(double x) {
		if (x < getMin()) return 0;
		if (x > getMax()) return 0;
		double dy = 1.0 / (getX().length-1);
		int index = findIndexForX(x);
		if (getX()[index] == x) {
			double x0 = getX()[Math.max(0, index-1)];
			double x1 = getX()[Math.min(getX().length-1, index+1)];
			return (x1-x0)/(2*dy);
		}
		return (getX()[index+1] - getX()[index])/dy;
	}
	
	private double interpolateY(double y) {
		if (y < Math.ulp(0) * 2) return getMin();
		if (1-y < Math.ulp(0) * 2) return getMax();
		double indexDbl = Math.floor(y*(getX().length-1));
		int index = (int) Math.floor(indexDbl);
		double mod = indexDbl - index;
		if (mod < Math.ulp(0) * 2) return getX()[index];
		return getX()[index] + (getX()[index+1]-getX()[index])*mod; 
	}
	
	@JsonIgnore
	default double sample(Sampler rng) {
		return interpolateY(rng.nextDouble());
	}
	
	@JsonIgnore
	@Value.Lazy
	default double getCentral() {
		return Arrays.stream(getX()).average().orElse(Double.NaN);
	};
	
	@JsonIgnore
	default double getCumulative(double x) {
		return interpolateX(x);
	};
	
	@JsonIgnore
	default double getMedian() {
		return interpolateY(0.5);
	}
	@JsonIgnore
	default double getDensity(double x) {
		return gradientX(x);
	};
	
	public static ImmutableHistogramDistribution fromData(double... tmp) {
		
		Builder out = ImmutableHistogramDistribution.builder();
		out.setSamples(tmp);
		return out.build();
		
	}
	
	default public double getQuantile(double p) {
		return interpolateY(p);
	}
}
