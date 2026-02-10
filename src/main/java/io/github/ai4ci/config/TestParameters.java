package io.github.ai4ci.config;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.util.Sampler;

/**
 * Defines parameters for medical diagnostic tests used in epidemiological simulations.
 * 
 * <p>Models key characteristics of diagnostic tests including:
 * <ul>
 *   <li>Test sensitivity (true positive rate)</li>
 *   <li>Test specificity (true negative rate)</li>
 *   <li>Result reporting delays</li>
 *   <li>Limit of detection</li>
 * </ul>
 * 
 * The testing assumes a viral load signal that is normalised but unbounded.
 * The limit of detection is used to determine the point at which the test can reliably detect infection. 
 * A true viral load at the limit of detection will produce a true positive rate equal to the test sensitivity, and a true viral load of zero will produce a false positive rate equal to (1 - specificity).
 * Very high viral loads will produce a true positive rate approaching 1, but the false positive rate will remain at (1 - specificity) regardless of viral load.
 * 
 *
 * <p>Used to simulate test accuracy and results processing in disease transmission models.
 * Implements statistical methods for calculating likelihood ratios and applying test noise.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableTestParameters.class)
@JsonDeserialize(as = ImmutableTestParameters.class)
public interface TestParameters extends Serializable {
	
	/**
	 * Identifier for the test type (e.g. "LFT", "PCR", "SYMPTOMS").
	 * @return The name/type of the diagnostic test
	 */
	String getTestName(); 
	
	/**
	 * Gets the test's sensitivity (true positive rate).
	 * @return Value between 0 and 1 representing probability of positive result when infected
	 */
	Double getSensitivity();

	/**
	 * Gets the test's specificity (true negative rate).
	 * @return Value between 0 and 1 representing probability of negative result when not infected
	 */
	Double getSpecificity();
	
	/**
	 * Gets the mean delay for test results to become available.
	 * @return Average delay in days
	 */
	Double getMeanTestDelay();
	
	/**
	 * Gets the standard deviation of test result delay.
	 * @return Standard deviation in days (log-normal distribution assumed)
	 */
	Double getSdTestDelay();
	
	/**
	 * Gets the test's limit of detection.
	 * @return Minimum detectable signal level
	 */
	Double getLimitOfDetection();
	
	/**
	 * Applies test noise to a normalized viral load signal.
	 * @param normViralLoad Normalized viral load (0-1 scale)
	 * @param rng Random number generator for noise application
	 * @return Noisy signal value after applying test characteristics
	 * @see #applyNoise(double, double, double, double, Sampler)
	 */
	default double applyNoise(double normViralLoad, Sampler rng) {
		return applyNoise(normViralLoad,getSensitivity(),getSpecificity(), getLimitOfDetection(),rng);
	}
	
	/**
	 * Calculates the positive likelihood ratio (LR+).
	 * LR+ = Sensitivity / (1 - Specificity)
	 * @return Ratio indicating how much to increase probability of disease with positive result
	 * @throws ArithmeticException if specificity equals 1 (division by zero)
	 */
	default double positiveLikelihoodRatio() {
		if (this.getSpecificity() == 1) return Double.POSITIVE_INFINITY;
		return this.getSensitivity() / (1-this.getSpecificity());
	}
	
	/**
	 * Calculates the negative likelihood ratio (LR-). 
	 * LR- = (1 - Sensitivity) / Specificity
	 * @return Ratio indicating how much to decrease probability of disease with negative result
	 * @throws ArithmeticException if specificity equals 0 (division by zero)
	 */
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
	 * Applies diagnostic test noise to a normalized signal based on test characteristics.
	 * 
	 * <p>Implements a uniform noise model where:
	 * <ul>
	 *   <li>Output for zero signal should be &lt; 1 in (specificity) cases</li>
	 *   <li>Output for signal at limit of detection should be 1 in (1-sensitivity) cases</li>
	 * </ul>
	 * 
	 * <p>The noise follows a linear cumulative distribution between:
	 * - Point 0: (1-sensitivity)
	 * - Point 1: (1-specificity)
	 * 
	 * @param normalisedSignal Input signal (0-1 scale)
	 * @param sensitivity Test sensitivity
	 * @param specificity Test specificity
	 * @param limitOfDetection Minimum detectable signal
	 * @param rng Random number generator
	 * @return Noisy output signal bounded [0, ∞)
	 */
	static double applyNoise(double normalisedSignal, double sensitivity, double specificity, double limitOfDetection, Sampler rng) {
		// TODO: Convert test error to use a normally distributed noise function 
		// this assumes a 0-1 range of signal. all we need to do is look at 
		// a normal with sensitivity / specificity quantiles at 0 and 1, or 
		// what ever the test limit is.
		return Math.max(0, normalisedSignal + limitOfDetection *
				(rng.uniform()-(1 - sensitivity)) / (specificity + sensitivity - 1));
	}
}