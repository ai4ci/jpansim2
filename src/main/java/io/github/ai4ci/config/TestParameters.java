package io.github.ai4ci.config;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.util.Sampler;

@Value.Immutable
@JsonSerialize(as = ImmutableTestParameters.class)
@JsonDeserialize(as = ImmutableTestParameters.class)
public interface TestParameters extends Serializable {
	
	/**
	 * Identifier for the test type e.g. LFT, PCR, SYMPTOMS
	 */
	String getTestName(); 
	Double getSensitivity();
	Double getSpecificity();
	/**
	 * The average delay in the result becoming available (days)
	 */
	Double getMeanTestDelay();
	/**
	 * The SD of the delay in the result. This will be a log normal
	 */
	Double getSdTestDelay();
	
	default double applyNoise(double normViralLoad, Sampler rng) {
		return applyNoise(normViralLoad,getSensitivity(),getSpecificity(),rng);
	}
	
	default double positiveLikelihoodRatio() {
		if (this.getSpecificity() == 1) return Double.POSITIVE_INFINITY;
		return this.getSensitivity() / (1-this.getSpecificity());
	}
	
	default double negativeLikelihoodRatio() {
		if (this.getSpecificity() == 0) return Double.POSITIVE_INFINITY;
		return (1-this.getSensitivity()) / (this.getSpecificity());
	}
	
//	default double postPositiveTestProbability(double prevalence) {
//		double preTestProbability = prevalence;
//		double preTestOdds = preTestProbability / (1-preTestProbability);
//		double postTestOdds = preTestOdds * this.positiveLikelihoodRatio();
//		return postTestOdds / (1+postTestOdds); 
//	}
//	
//	default double postNegativeTestProbability(double prevalence) {
//		double preTestProbability = prevalence;
//		double preTestOdds = preTestProbability / (1-preTestProbability);
//		double postTestOdds = preTestOdds * this.negativeLikelihoodRatio();
//		return postTestOdds / (1+postTestOdds); 
//	}
	
	/**
	 * This comes from the following logic. a uniform distribution of noise is
	 * added. If all the signal is zero this should be less than 1 in (spec)
	 * of the cases. If the signal is one then the sum should be less than 1 in
	 * (1-sens) of the time. The CDF of the uniform that does this is a straight
	 * line going through the points 0,(1-sens) and 1,(1-spec). This is connected
	 * to the rogan gladen estimator.
	 */
	static double applyNoise(double normalisedSignal, double sensitivity, double specificity, Sampler rng) {
		// TODO: Convert test error to use a normally distributed noise function 
		// this assumes a 0-1 range of signal. all we need to do is look at 
		// a normal with sensitivity / specificity quantiles at 0 and 1, or 
		// what ever the test limit is.
		return Math.max(0, normalisedSignal +
				(rng.uniform()-(1 - sensitivity)) / (specificity + sensitivity - 1));
	}
}
