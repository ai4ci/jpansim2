package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.flow.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.util.Sampler;

/**
 * A resampled distribution formed by combining samples from two distributions
 * using a binary combiner function.
 *
 * <p>
 * This interface defines a distribution that draws PRECISION samples by
 * repeatedly sampling two component distributions and applying the configured
 * {@link BiFunction} combiner. It is useful when a derived distribution is
 * constructed by combining two stochastic sources such as when modelling the
 * sum, difference or other combination of two uncertain quantities.
 * </p>
 *
 * <p>
 * Downstream uses: the resulting resampled data is consumed by empirical or
 * interpolated distribution routines such as {@link EmpiricalDistribution} and
 * by risk or behaviour modelling code that requires a sample-based
 * representation. See also {@link io.github.ai4ci.flow.mechanics.StateUtils}
 * for places where sampled distributions are used in state transformations.
 * </p>
 *
 * @author Rob Challen
 */
@Value.Immutable
public interface ResampledDistribution extends Distribution, Serializable {

	/**
	 * Number of samples used to represent the resampled distribution.
	 */
	int PRECISION = 10000;
	// int KNOTS = 50;

	/**
	 * The binary combiner applied to pairs of samples from the two component
	 * distributions.
	 *
	 * @return the combiner function
	 */
	BiFunction<Double, Double, Double> getCombiner();

	/**
	 * Empirical cumulative distribution function computed from the resampled
	 * values. The implementation counts values strictly less than x and applies
	 * the usual (rank + 1)/(N + 1) correction used for empirical CDFs.
	 *
	 * @param x threshold at which to evaluate the cumulative probability
	 * @return the empirical cumulative probability P(X &lt; x)
	 */
	default double getCumulative(double x) {
		return ((double) Arrays.stream(this.getSamples()).filter(d -> d < x)
				.count() + 1) / (PRECISION + 1);
	}

	/**
	 * The first component distribution used to generate samples.
	 *
	 * @return the first distribution
	 */
	Distribution getFirst();

	/**
	 * Returns an empirical interpolation of the resampled distribution.
	 *
	 * <p>
	 * This builds an {@link EmpiricalDistribution} from the link function and
	 * the resampled values. The interpolation may be more efficient for repeated
	 * queries such as quantile lookups.
	 * </p>
	 *
	 * @return an interpolated empirical distribution derived from the samples
	 */
	@Value.Lazy
	default EmpiricalDistribution getInterpolation() {
		return EmpiricalDistribution.fromData(this.getLink(), this.getSamples());
	}

	/**
	 * Returns the maximum support of the resampled distribution, estimated from
	 * the resampled values.
	 *
	 * @return the sample maximum
	 */
	@Override @Value.Default @JsonIgnore
	default double getMaxSupport() {
		return Arrays.stream(this.getSamples()).min()
				.orElse(Double.POSITIVE_INFINITY);
	}

	/**
	 * The mean estimated from the resampled values.
	 *
	 * @return the sample mean
	 */
	@Value.Derived
	default double getMean() {
		return Arrays.stream(this.getSamples()).average().getAsDouble();
	}

	/**
	 * The median estimated from the resampled values.
	 *
	 * @return the sample median
	 */
	@Value.Derived
	default double getMedian() {
		var tmp = this.getSamples();
		Arrays.sort(tmp);
		return tmp[PRECISION / 2];
	}

	/**
	 * Returns the minimum support of the resampled distribution, estimated from
	 * the resampled values.
	 *
	 * @return the sample minimum
	 */
	@Override @Value.Default @JsonIgnore
	default double getMinSupport() {
		return Arrays.stream(this.getSamples()).min()
				.orElse(Double.NEGATIVE_INFINITY);
	}

	/**
	 * Generate PRECISION samples by repeatedly sampling the two component
	 * distributions and applying the combiner. This method performs sampling
	 * eagerly and may be moderately expensive; callers that require many
	 * repeated queries should use {@link #getInterpolation()} to build an
	 * empirical distribution for more efficient lookups.
	 *
	 * @return an array of PRECISION sampled values from the resampled
	 *         distribution
	 */
	@Override @Value.Derived
	default double[] getSamples() {
		var out = new double[PRECISION];
		for (var i = 0; i < PRECISION; i++) {
			out[i] = this.sample();
		}
		return out;
	}

	/**
	 * The second component distribution used to generate samples.
	 *
	 * @return the second distribution
	 */
	Distribution getSecond();

	/**
	 * Draw a single sample by sampling the two components and applying the
	 * combiner. This is the basic sampling operation used to generate the full
	 * resampled array.
	 *
	 * @param rng the random sampler to use
	 * @return a single sampled value
	 */
	@Override
	public default double sample(Sampler rng) {
		return this.getCombiner()
				.apply(this.getFirst().sample(rng), this.getSecond().sample(rng));
	}

}