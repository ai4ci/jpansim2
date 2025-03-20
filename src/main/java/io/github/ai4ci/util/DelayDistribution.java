package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.immutables.value.Value;

/**
 * The delay distribution is a probability distribution in time
 * conditional on an event happening. It is useful to generate a per
 * day conditional hazard.
 * 
 * Relationships are discrete versions of https://grodri.github.io/glms/notes/c7s1
 */
@Value.Immutable
public abstract class DelayDistribution implements Serializable {
 
	/**
	 * The probability of being affected at time infinity (1-survival_Inf)  
	 */
	@Value.Default
	public double getPAffected() {return 1.0;};
	
	/**
	 * the improper probability density - which will sum to pAffected
	 */
	@Value.Derived
	protected double[] density() {
		return DoubleStream.of(condDensity()).map(d -> d*getPAffected()).toArray();
	};
	
	/**
	 * the conditional probability density - which will sum to 1
	 */
	abstract protected double[] condDensity();
	
	@Value.Check
	protected DelayDistribution normalize() {
		double sum = DoubleStream.of(condDensity()).sum();
		if (sum==1) return this;
		double[] norm = DoubleStream.of(condDensity()).map(d -> d/sum).toArray();
		return ImmutableDelayDistribution.builder()
			.setPAffected(getPAffected())
			.setCondDensity(norm)
			.build();
	}
	
	/** 
	 * the unconditional survival function 
	 */
	@Value.Derived
	protected double[] survival() {
		double[] survival = new double[density().length];
		//this.condHazard = new double[density.length];
		//this.condSurvival = new double[density.length];
		
		for (int i = 0; i<density().length; i++) {
			survival[i] = (i == 0 ? 1 : survival[i-1]) - this.density()[i];
		}
		return survival;
	};
	
	
	/** 
	 * the unconditional hazard function 
	 */
	@Value.Derived
	protected double[] hazard() {
		double[] hazard = new double[survival().length];
		
		for (int i = 0; i<density().length; i++) {
			hazard[i] = this.density()[i] / (i == 0 ? 1 : this.survival()[i-1]);
		}
		return hazard;
	};
	
	

	
	
	public double density(int x) {
		if (x<0) return 0;
		if (x>=density().length) return 0;
		return density()[x];
	}
	
	public double cumulative(int x) {
		if (x<0) return 0;
		if (x>=survival().length) return 1;
		return 1-survival()[x];
	}
	
	public double hazard(int x) {
		if (x<0) return 0;
		if (x>=hazard().length) return 0;
		return hazard()[x];
	}
	
	public double expected() {
		return expected(1);
	}
	
	public double expected(double sampleSize) {
		double out = 0.0D;
		for (int i = 0; i<density().length; i++) {
			out += i*density()[i];
		}
		return out*sampleSize;
	}
	
	public DelayDistribution conditionedOn(double pAffected) {
		return ImmutableDelayDistribution.builder()
				.from(this).setPAffected(pAffected)
				.build();
	}
//	
//	public static DelayDistribution fromPDF(double... probabilities) {
//		double conditionalProbability = DoubleStream.of(probabilities).sum();
//		return new DelayDistribution(
//				DoubleStream.of(probabilities).map(p -> p/conditionalProbability).toArray(), 
//				conditionalProbability);
//	}
//	
//	public static DelayDistribution fromCDF(double... cumulative) {
//		double density[] = new double[cumulative.length];
//		for (int i=0; i<cumulative.length; i++) {
//			density[i] = cumulative[i] - (i>0 ? cumulative[i-1] : 0); 
//		}
//		return fromPDF(density);
//	}
	
	public long size() {
		return (long) density().length;
	}
	
	public String toString() {
		return "P("+Arrays.toString(density())+"|"+getPAffected()+")";
	}

	
	/**
	 * What proportion of individuals expected to have been affected by
	 * day X. (i.e. 1-prob(survived to day X)) 
	 * @param intValue
	 * @return
	 */
	public double affected(int intValue) {
		return 1-this.survival()[intValue];
	}
	
	public static DelayDistribution unnormalised(double... density) {
		return ImmutableDelayDistribution.builder()
			.setCondDensity(density)
			.setPAffected(1)
			.build();
	}
//		double density[] = new double[cumulative.length];
//		for (int i=0; i<cumulative.length; i++) {
//			density[i] = cumulative[i] - (i>0 ? cumulative[i-1] : 0); 
//		}
//		return fromPDF(density);
//	}
}
