package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.statistics.distribution.GammaDistribution;
import org.immutables.value.Value;

/**
 * A delay distribution representing a probability distribution in discrete time
 * conditional on an event happening. This class is useful for generating
 * per-day conditional hazards and modelling time-to-event data.
 *
 * <p>
 * <b>Input Constraints:</b>
 * <ul>
 * <li>The profile array must contain non-negative values</li>
 * <li>The profile array should not contain NaN or infinite values</li>
 * <li>pAffected must be in the range [0, 1]</li>
 * <li>Empty profile arrays are supported but result in empty distributions</li>
 * </ul>
 *
 * <p>
 * <b>Key Mathematical Relationships:</b> \[ \text{density}_i =
 * \text{condDensity}_i \times p_{\text{affected}} \\ \text{condDensity}_i =
 * \frac{\text{profile}_i}{\sum_j \text{profile}_j} \\ S_i = 1 - \sum_{j=0}^i
 * \text{density}_j \\ h_i = \frac{\text{density}_i}{S_{i-1}} \quad (i > 0) \\
 * h_0 = \text{density}_0 \] where S denotes the survival function and h denotes
 * the hazard function.
 *
 * <p>
 * Relationships are discrete versions of:
 * https://grodri.github.io/glms/notes/c7s1
 *
 * @see Serializable
 * @see GammaDistribution
 * @see ImmutableDelayDistribution
 */
@Value.Immutable
public abstract class DelayDistribution implements Serializable {

	/**
	 * Creates a discretised gamma distribution as a delay distribution.
	 * Automatically determines length based on mean + 2 standard deviations.
	 *
	 * @param mean mean of the gamma distribution
	 * @param sd   standard deviation of the gamma distribution
	 * @return discretised gamma delay distribution
	 */
	public static DelayDistribution discretisedGamma(double mean, double sd) {
		return discretisedGamma(mean, sd, (int) Math.round(mean + 2 * sd));
	}

	/**
	 * Creates a discretised gamma distribution with specified length. Gamma
	 * distribution is discretised by integrating over unit intervals.
	 *
	 * @param mean   mean of the gamma distribution
	 * @param sd     standard deviation of the gamma distribution
	 * @param length number of time points in the discretisation
	 * @return discretised gamma delay distribution
	 */
	public static DelayDistribution discretisedGamma(
			double mean, double sd, int length
	) {
		double shape = (mean * mean) / (sd * sd);
		double scale = (sd * sd) / mean;
		GammaDistribution tmp = GammaDistribution.of(shape, scale);
		double[] out = new double[length];
		double x0 = 0D;
		double x1 = 0.5D;
		for (int i = 0; i < length; i++) {
			out[i] = tmp.probability(x0, x1);
			x0 = x1;
			x1 += 1;
		}
		return ImmutableDelayDistribution.builder().setProfile(out).build();
	}

	/**
	 * Creates an empty delay distribution with zero length.
	 *
	 * @return empty DelayDistribution instance
	 */
	public static DelayDistribution empty() {
		return ImmutableDelayDistribution.builder().setProfile(new double[0])
				.build();
	}

	/**
	 * Trims the tail of an array by removing elements until the cumulative sum
	 * reaches within epsilon of the total (either absolute or relative). Useful
	 * for reducing memory usage while preserving accuracy.
	 *
	 * @param x        array to trim
	 * @param epsilon  tolerance value
	 * @param absolute if true, epsilon is absolute; if false, epsilon is
	 *                 relative
	 * @return trimmed array
	 */
	public static double[] trimTail(
			double[] x, double epsilon, boolean absolute
	) {
		double total = 0;
		for (double element : x) {
			total += element;
		}
		double limit = absolute ? total - epsilon : total * (1 - epsilon);
		for (int i = x.length - 1; i >= 0; i--) {
			total -= x[i];
			if (total < limit) return Arrays.copyOf(x, i + 1);
		}
		return new double[0];
	}

	/**
	 * Removes trailing zeros from an array. Useful for cleaning up distributions
	 * where the tail has negligible probability.
	 *
	 * @param x array to trim
	 * @return array with trailing zeros removed
	 */
	public static double[] trimZeros(double[] x) {
		int i = x.length;
		if (x[i - 1] > 0) return x;
		while (i > 0) {
			if (x[i - 1] > 0) return Arrays.copyOf(x, i);
			i = i - 1;
		}
		return new double[0];
	}

	/**
	 * Creates a DelayDistribution from raw unnormalised profile values.
	 * Automatically trims trailing zeros and sets pAffected to 1.
	 *
	 * @param profile raw counts or unnormalised probabilities
	 * @return normalised DelayDistribution instance
	 */
	public static ImmutableDelayDistribution unnormalised(double... profile) {
		return ImmutableDelayDistribution.builder().setProfile(trimZeros(profile))
				.setPAffected(1).build();
	}

	/**
	 * Computes the proportion of individuals expected to be affected by day x.
	 * Equivalent to the cumulative distribution function at time x.
	 *
	 * @param intValue time index (day number)
	 * @return proportion affected by time x
	 */
	public double affected(int intValue) {
		return 1 - this.survival()[intValue];
	}

	/**
	 * Computes the conditional probability density function (PDF). Conditional
	 * on the event occurring, the sum equals 1.
	 *
	 * @return array of conditional probabilities
	 */
	@Value.Derived
	public double[] condDensity() {
		if (this.getProfile().length == 0) return new double[0];
		return DoubleStream.of(this.getProfile()).map(d -> d / this.total())
				.toArray();
	}

	/**
	 * Gets the conditional density value at the specified time index. Returns 0
	 * for indices outside the valid range.
	 *
	 * @param x time index (must be non-negative)
	 * @return conditional density value at time x, or 0 if out of bounds
	 */
	public double condDensity(int x) {
		if ((x < 0) || (x >= this.condDensity().length)) return 0;
		return this.condDensity()[x];
	}

	/**
	 * Creates a new delay distribution with the specified ultimate affected
	 * probability. Useful for sensitivity analysis or modelling different
	 * scenarios.
	 *
	 * @param pAffected new ultimate affected probability in range [0, 1]
	 * @return new DelayDistribution instance with updated pAffected
	 */
	public DelayDistribution conditionedOn(double pAffected) {
		return ImmutableDelayDistribution.builder().from(this)
				.setPAffected(pAffected).build();
	}

	/**
	 * Computes the discrete convolution of two arrays. output[i] = ∑(input[i-j]
	 * × by[j]) for j < i and j < by.length
	 */
	private double[] convolution(double[] input, double[] by) {
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			for (int j = 0; j < i && j < by.length; j++) {
				output[i] += input[i - j] * by[j];
			}
		}
		return output;
	}

	/**
	 * Computes the convolution of an input array with the density array. Useful
	 * for modelling probability-weighted delayed effects.
	 *
	 * @param input array to convolve
	 * @return convolved result
	 */
	public double[] convolveDensity(double[] input) {
		return this.convolution(input, this.density());
	}

	/**
	 * Computes the convolution of an input array with the profile array. Useful
	 * for modelling delayed effects over time.
	 *
	 * @param input array to convolve
	 * @return convolved result
	 */
	public double[] convolveProfile(double[] input) {
		return this.convolution(input, this.getProfile());
	}

	/**
	 * Computes the cumulative distribution function (CDF) up to time x.
	 * Represents the probability of being affected by time x.
	 *
	 * @param x time index (must be non-negative)
	 * @return cumulative probability in range [0, 1]
	 */
	public double cumulative(int x) {
		if (x < 0) return 0;
		if (x >= this.survival().length) return 1;
		return 1 - this.survival()[x];
	}

	/**
	 * Computes the unconditional probability density function (PDF). The sum of
	 * densities equals pAffected (not 1, unless pAffected = 1).
	 *
	 * @return array of unconditional probabilities
	 */
	@Value.Derived
	public double[] density() {
		if (this.getProfile().length == 0) return new double[0];
		return DoubleStream.of(this.condDensity())
				.map(d -> d * this.getPAffected()).toArray();
	}

	/**
	 * Gets the unconditional density value at the specified time index. Returns
	 * 0 for indices outside the valid range.
	 *
	 * @param x time index (must be non-negative)
	 * @return density value at time x, or 0 if out of bounds
	 */
	public double density(int x) {
		if ((x < 0) || (x >= this.density().length)) return 0;
		return this.density()[x];
	}

	/**
	 * Computes the expected number of events for sample size 1. Equivalent to
	 * the mean of the unconditional distribution.
	 *
	 * @return expected number of events
	 */
	public double expected() {
		return this.expected(1);
	}

	/**
	 * Computes the expected number of events for the given sample size.
	 *
	 * @param sampleSize number of individuals in the population
	 * @return expected number of events in the population
	 */
	public double expected(double sampleSize) {
		double out = 0.0D;
		for (int i = 0; i < this.density().length; i++) {
			out += i * this.density()[i];
		}
		return out * sampleSize;
	}

	/**
	 * Returns the probability of being affected by time infinity (1 - survival
	 * at infinity). Defaults to 1.0 (all individuals will eventually be
	 * affected).
	 *
	 * @return probability in range [0, 1] representing ultimate affected
	 *         proportion
	 */
	@Value.Default
	public double getPAffected() { return 1.0; }

	/**
	 * Returns the raw unnormalised counts or probabilities supporting the delay
	 * distribution. This array represents the raw observations or probability
	 * mass before normalisation.
	 *
	 * @return non-empty array of raw values (counts or unnormalised
	 *         probabilities)
	 */
	abstract public double[] getProfile();

	/**
	 * Finds the smallest time index where cumulative probability exceeds the
	 * given threshold. Equivalent to quantile function for the discrete
	 * distribution.
	 *
	 * @param d probability threshold in range [0, 1]
	 * @return smallest time index where cumulative > d
	 */
	public int getQuantile(double d) {
		for (int i = 0; i < this.size(); i++) {
			if (this.cumulative(i) > d) return i;
		}
		return (int) this.size();
	}

	/**
	 * Computes the unconditional hazard function. Conditional probability of
	 * being affected at time i given survival to time i-1.
	 *
	 * @return array of hazard rates
	 */
	@Value.Derived
	public double[] hazard() {
		if (this.getProfile().length == 0) return new double[0];
		double[] hazard = new double[this.survival().length];
		for (int i = 0; i < this.density().length; i++) {
			hazard[i] = this.density()[i] / (i == 0 ? 1 : this.survival()[i - 1]);
		}
		return hazard;
	}

	/**
	 * Gets the hazard rate at the specified time index. Returns 0 for indices
	 * outside the valid range.
	 *
	 * @param x time index (must be non-negative)
	 * @return hazard rate at time x, or 0 if out of bounds
	 */
	public double hazard(int x) {
		if ((x < 0) || (x >= this.hazard().length)) return 0;
		return this.hazard()[x];
	}

	/**
	 * Computes the mean of the conditional distribution. Represents the expected
	 * time until event occurrence among those affected.
	 *
	 * @return mean time to event (conditional on event occurrence)
	 */
	public double mean() {
		double[] density = this.condDensity();
		return IntStream.range(0, density.length).mapToDouble(i -> i * density[i])
				.sum();
	}

	/**
	 * Gets the raw profile value at the specified time index. Returns 0 for
	 * indices outside the valid range.
	 *
	 * @param x time index (must be non-negative)
	 * @return profile value at time x, or 0 if out of bounds
	 */
	public double profile(int x) {
		if ((x < 0) || (x >= this.getProfile().length)) return 0;
		return this.getProfile()[x];
	}

	/**
	 * Returns the length of the delay distribution (number of time points).
	 *
	 * @return number of time points in the distribution
	 */
	public long size() {
		return this.density().length;
	}

	/**
	 * Computes the unconditional survival function. Probability of surviving
	 * (not being affected) up to each time point.
	 *
	 * @return array of survival probabilities
	 */
	@Value.Derived
	public double[] survival() {
		if (this.getProfile().length == 0) return new double[0];
		double[] survival = new double[this.density().length];

		for (int i = 0; i < this.density().length; i++) {
			survival[i] = (i == 0 ? 1 : survival[i - 1]) - this.density()[i];
		}
		return survival;
	}

	/**
	 * Returns a string representation showing the density array and pAffected.
	 *
	 * @return string representation of the distribution
	 */
	@Override
	public String toString() {
		return "P(" + Arrays.toString(this.density()) + "|" + this.getPAffected()
				+ ")";
	}

	/**
	 * Computes the sum of all values in the profile array. Used as a normalising
	 * constant for converting to conditional probability.
	 *
	 * @return sum of all profile values
	 */
	@Value.Derived
	public double total() {
		return DoubleStream.of(this.getProfile()).sum();
	}
}
