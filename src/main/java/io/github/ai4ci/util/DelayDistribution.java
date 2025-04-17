package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.DoubleStream;

import org.apache.commons.statistics.distribution.GammaDistribution;
import org.immutables.value.Value;

/**
 * The delay distribution is a probability distribution in time
 * conditional on an event happening. It is useful to generate a per
 * day conditional hazard.
 * 
 * Relationships are discrete versions of https://grodri.github.io/glms/notes/c7s1
 */
@Value.Immutable
public abstract class DelayDistribution implements Serializable, WithDelayDistribution {
 
	abstract public double[] getProfile();
	
	@Value.Derived public double total() {
		return DoubleStream.of(getProfile()).sum();
	} 
	
	/**
	 * The probability of being affected at time infinity (1-survival_Inf)  
	 */
	@Value.Default
	public double getPAffected() {
		return 1.0;
	};
	
	/**
	 * the improper probability density - which will sum to pAffected
	 */
	@Value.Derived
	public double[] density() {
		if (getProfile().length == 0) return new double[0];
		return DoubleStream.of(condDensity()).map(d -> d*getPAffected()).toArray();
	};
	
	/**
	 * the conditional probability density - which will sum to 1
	 */
	@Value.Derived
	public double[] condDensity() {
		if (getProfile().length == 0) return new double[0];
		return DoubleStream.of(getProfile()).map(d -> d/total()).toArray();
	};
	
	/** 
	 * the unconditional survival function 
	 */
	@Value.Derived
	public double[] survival() {
		if (getProfile().length == 0) return new double[0];
		double[] survival = new double[density().length];
		//this.condHazard = new double[density.length];
		//this.condSurvival = new double[density.length];
		
		for (int i = 0; i<density().length; i++) {
			survival[i] = (i == 0 ? 1 : survival[i-1]) - this.density()[i];
		}
		return survival;
	};
	
	
	/** 
	 * the unconditional hazard function. If the affected total (i.e. the hazard
	 * at time=infinity) is known then what is the hazard on each day and this 
	 * is the density on that day divided by the number that survived to that
	 * day. 
	 */
	@Value.Derived
	public double[] hazard() {
		if (getProfile().length == 0) return new double[0];
		double[] hazard = new double[survival().length];
		for (int i = 0; i<density().length; i++) {
			hazard[i] = this.density()[i] / (i == 0 ? 1 : this.survival()[i-1]);
		}
		return hazard;
	};
	
	

	public double profile(int x) {
		if (x<0) return 0;
		if (x>=getProfile().length) return 0;
		return getProfile()[x];
	}
	
	public double density(int x) {
		if (x<0) return 0;
		if (x>=density().length) return 0;
		return density()[x];
	}
	
	public double condDensity(int x) {
		if (x<0) return 0;
		if (x>=condDensity().length) return 0;
		return condDensity()[x];
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
				.from(this)
				.setPAffected(pAffected)
				.build();
	}
	
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
	
	public double[] convolveProfile(double[] input) {
		return convolution(input, getProfile());
	}
	
	public double[] convolveDensity(double[] input) {
		return convolution(input, density());
	}
	
	private double[] convolution(double[] input, double[] by) {
		double[] output = new double[input.length];
		for (int i=0; i<input.length; i++) {
			for (int j=0; j<i && j<by.length; j++) {
				output[i] += input[i-j] * by[j];
			}
		}
		return output;
	}
	
	public static DelayDistribution unnormalised(double... profile) {
		return ImmutableDelayDistribution.builder()
			.setProfile(profile)
			.setPAffected(1)
			.build();
	}
	
	public static DelayDistribution empty() {
		return ImmutableDelayDistribution.builder()
				.setProfile(new double[0])
				.build();
	}
	
	public static DelayDistribution discretisedGamma(double mean, double sd) {
		return discretisedGamma(mean,sd,(int) Math.round(mean+2*sd));
	}
	
	public static DelayDistribution discretisedGamma(double mean, double sd, int length) {
		double shape = (mean*mean)/(sd*sd);
		double scale = (sd*sd)/mean;
		GammaDistribution tmp = GammaDistribution.of(shape,scale);
		double[] out = new double[length];
		double x0 = 0D;
		double x1 = 0.5D;
		for (int i=0; i<length; i++) {
			out[i] = tmp.probability(x0,x1);
			x0 = x1;
			x1 += 1;
		}
		return ImmutableDelayDistribution.builder()
				.setProfile(out).build();
	}
	
	/**
	 * The probability that a repeated event that occurs with pEvent on each day 
	 * happens at some point over the delay distribution, given that the 
	 * conditional probabilities on each day add to one, the result will be 
	 * less than or equal to pEvent. This is a way of calculating the 
	 * total affected when the probability of the event is on a per day basis.
	 * @param pEvent
	 * @return
	 */
	public double totalHazard(double pEvent) {
		return 1-Arrays.stream(this.condDensity())
				.map(d -> 1-d*pEvent)
				.reduce((d1, d2) -> d1*d2)
				.orElse(1);	
	}
	
//		double density[] = new double[cumulative.length];
//		for (int i=0; i<cumulative.length; i++) {
//			density[i] = cumulative[i] - (i>0 ? cumulative[i-1] : 0); 
//		}
//		return fromPDF(density);
//	}
}
