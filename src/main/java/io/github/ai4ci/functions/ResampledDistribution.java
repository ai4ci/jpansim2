package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.flow.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface ResampledDistribution extends Distribution,Serializable {
	
	int PRECISION = 10000;
	// int KNOTS = 50;
	
	Distribution getFirst();
	Distribution getSecond();
	BiFunction<Double,Double,Double> getCombiner();
	
	@Value.Derived default double[] getSamples() {
		double[] out = new double[PRECISION];
		for (int i=0;i<PRECISION;i++) {
			out[i] = sample();
		}
		return out;
	}
	
	@Value.Default @JsonIgnore
	default double getMinSupport() {return 
			Arrays.stream(getSamples()).min().orElse(Double.NEGATIVE_INFINITY);}
	@Value.Default @JsonIgnore
	default double getMaxSupport() {return 
			Arrays.stream(getSamples()).min().orElse(Double.POSITIVE_INFINITY);}
	 
	@Value.Derived default double getMean() {
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