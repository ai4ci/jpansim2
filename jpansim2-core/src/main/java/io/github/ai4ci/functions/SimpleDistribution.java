package io.github.ai4ci.functions;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.util.Sampler;

/**
 * Specify distributions and parameters in a way that makes configuring them in
 * a JSON file is relatively straightforward. All distributions defined by a
 * central and dispersion parameter which in most cases will be Mean and SD.
 */

public interface SimpleDistribution extends Distribution, Serializable {

	/**
	 * Configuration for a Beta Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableBetaDistribution.class)
	@JsonDeserialize(as = ImmutableBetaDistribution.class)
	public static interface BetaDistribution
			extends SimpleDistribution, FromMoments {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOGIT; }

		@Override
		double getMean();

		@Override
		double getSd();

		@Override
		default double sample(Sampler s) {
			return s.beta(this.getMean(), this.getSd(), false);
		}
	}

	/**
	 * Configuration for a Binomial Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableBinomialDistribution.class)
	@JsonDeserialize(as = ImmutableBinomialDistribution.class)
	public static interface BinomialDistribution
			extends SimpleDistribution, FromMoments {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOGIT; }

		@Override
		double getMean();

		@Override
		double getSd();

		@Override
		default double sample(Sampler s) {
			return s.binom(this.getMean(), this.getSd());
		}
	}

	/**
	 * Interface for distributions that can be parameterized by their mean and
	 * standard deviation.
	 */
	public static interface FromMoments {

		/**
		 * The expected value of the distribution.
		 *
		 * @return the expected value
		 */
		double getMean();
		/**
		 * The standard deviation of the distribution.
		 *
		 * @return the standard deviation
		 */
		double getSd();
	}

	/**
	 * Configuration for a Gamma Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableGammaDistribution.class)
	@JsonDeserialize(as = ImmutableGammaDistribution.class)
	public static interface GammaDistribution
			extends SimpleDistribution, FromMoments {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOG; }

		@Override
		double getMean();

		@Override
		double getSd();

		@Override
		default double sample(Sampler s) {
			return s.gamma(this.getMean(), this.getSd());
		}
	}

	/**
	 * Configuration for a Logit-Normal Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableLogitNormalDistribution.class)
	@JsonDeserialize(as = ImmutableLogitNormalDistribution.class)
	public static interface LogitNormalDistribution extends SimpleDistribution {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOGIT; }

		/**
		 * The median of the distribution
		 *
		 * @return the median of the distribution
		 */
		double getMedian();

		/**
		 * The scale parameter of the distribution, which controls the spread of
		 * the distribution around the median.
		 *
		 * @return the scale parameter
		 */
		double getScale();

		@Override
		default double sample(Sampler s) {
			return s.logitNormal(this.getMedian(), this.getScale());
		}
	}

	/**
	 * Configuration for a Log-Normal Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableLogNormalDistribution.class)
	@JsonDeserialize(as = ImmutableLogNormalDistribution.class)
	public static interface LogNormalDistribution
			extends SimpleDistribution, FromMoments {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOG; }

		@Override
		double getMean();

		@Override
		double getSd();

		@Override
		default double sample(Sampler s) {
			return s.logNormal(this.getMean(), this.getSd());
		}
	}

	/**
	 * Configuration for a Negative Binomial Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableNegBinomialDistribution.class)
	@JsonDeserialize(as = ImmutableNegBinomialDistribution.class)
	public static interface NegBinomialDistribution
			extends SimpleDistribution, FromMoments {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOG; }

		@Override
		double getMean();

		@Override
		double getSd();

		@Override
		default double sample(Sampler s) {
			return s.negBinom(this.getMean(), this.getSd());
		}
	}

	/**
	 * Configuration for a Normal Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableNormalDistribution.class)
	@JsonDeserialize(as = ImmutableNormalDistribution.class)
	public static interface NormalDistribution
			extends SimpleDistribution, FromMoments {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.NONE; }

		@Override
		double getMean();

		@Override
		double getSd();

		@Override
		default double sample(Sampler s) {
			return s.normal(this.getMean(), this.getSd());
		}
	}

	/**
	 * Configuration for a Point Distribution, which always returns the same
	 * value.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutablePointDistribution.class)
	@JsonDeserialize(as = ImmutablePointDistribution.class)
	public static interface PointDistribution extends SimpleDistribution {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.NONE; }

		/**
		 * The single value of the distribution.
		 *
		 * @return the value of the distribution
		 */
		double getMean();

		@Override
		default double sample(Sampler s) {
			return this.getMean();
		}
	}

	/**
	 * Configuration for a Poisson Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutablePoissonDistribution.class)
	@JsonDeserialize(as = ImmutablePoissonDistribution.class)
	public static interface PoissonDistribution extends SimpleDistribution {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOG; }

		/**
		 * The expected value of the distribution, which is also the variance.
		 *
		 * @return the expected value
		 */
		double getMean();

		@Override
		default double sample(Sampler s) {
			return s.poisson(this.getMean());
		}
	}

	/**
	 * Configuration for a Uniform Distribution.
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableUniformDistribution.class)
	@JsonDeserialize(as = ImmutableUniformDistribution.class)
	public static interface UniformDistribution extends SimpleDistribution {

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.NONE; }

		/**
		 * The maximum value of the distribution.
		 *
		 * @return the maximum value
		 */
		@Value.Default

		default double getMax() { return 1; }

		@Override
		default double getMaxSupport() { return this.getMax(); }

		/**
		 * The mean of the distribution, which is the midpoint between the minimum
		 * and maximum values.
		 *
		 * @return the mean value
		 */
		@Value.Default @JsonIgnore
		default double getMean() {
			return (this.getMax() - this.getMin()) / 2.0 + this.getMin();
		}

		/**
		 * The minimum value of the distribution.
		 *
		 * @return the minimum value
		 */
		@Value.Default
		default double getMin() { return 0; }

		@Override
		default double getMinSupport() { return this.getMin(); }

		@Override
		default double sample(Sampler s) {
			return s.uniform(this.getMin(), this.getMax());
		}
	}

	/**
	 * Configuration for a Unimodal Beta Distribution
	 */
	@Value.Immutable
	@JsonSerialize(as = ImmutableUnimodalBetaDistribution.class)
	@JsonDeserialize(as = ImmutableUnimodalBetaDistribution.class)
	public static interface UnimodalBetaDistribution extends SimpleDistribution {

		/**
		 * The dispersion parameter of the distribution, which controls how
		 * tightly the distribution is concentrated around the mean.
		 *
		 * It is a number between 0 and 1 where 1 is the most dispersed that can
		 * be achieved for a given mean, extreme means (close to 0 or 1) will have
		 * lower maximum standard deviation.
		 *
		 * @return the dispersion parameter
		 */
		double getDispersion();

		@Override @Value.Default @JsonIgnore
		default LinkFunction getLink() { return LinkFunction.LOGIT; }

		/**
		 * The mean of the distribution, which is the expected value.
		 *
		 * @return the mean value
		 */
		double getMean();

		@Override
		default double sample(Sampler s) {
			return s.beta(this.getMean(), this.getDispersion(), true);
		}
	}

	/**
	 * Factory methods for beta distribution
	 *
	 * @param mean the mean of the distribution
	 * @param sd   the standard deviation of the distribution
	 * @return a BetaDistribution instance with the specified mean and standard
	 *         deviation
	 */
	public static BetaDistribution beta(Double mean, Double sd) {
		return ImmutableBetaDistribution.builder().setMean(mean).setSd(sd)
				.build();
	}

	/**
	 * Factory methods for binomial distribution
	 *
	 * @param mean the mean of the distribution
	 * @param sd   the standard deviation of the distribution
	 * @return a BinomialDistribution instance with the specified mean and
	 *         standard deviation
	 */
	public static BinomialDistribution binom(double mean, double sd) {
		return ImmutableBinomialDistribution.builder().setMean(mean).setSd(sd)
				.build();
	}

	/**
	 * Factory methods for binomial distribution
	 *
	 * @param n the number of trials
	 * @param p the probability of success on each trial
	 * @return a BinomialDistribution instance with the specified parameters
	 */
	public static BinomialDistribution binom(int n, double p) {
		return ImmutableBinomialDistribution.builder().setMean(n * p)
				.setSd(n * p * (1 - p)).build();
	}

	/**
	 * Factory methods for gamma distribution
	 *
	 * @param mean the mean of the distribution
	 * @param sd   the standard deviation of the distribution
	 * @return a GammaDistribution instance with the specified mean and standard
	 *         deviation
	 */
	public static GammaDistribution gamma(Double mean, Double sd) {
		return ImmutableGammaDistribution.builder().setMean(mean).setSd(sd)
				.build();
	}

	/**
	 * Factory methods for log-normal distribution
	 *
	 * @param mean the mean of the distribution
	 * @param sd   the standard deviation of the distribution
	 * @return a LogNormalDistribution instance with the specified mean and
	 *         standard deviation
	 */
	public static LogNormalDistribution logNorm(Double mean, Double sd) {
		return ImmutableLogNormalDistribution.builder().setMean(mean).setSd(sd)
				.build();
	}

	/**
	 * Factory methods for negative binomial distribution
	 *
	 * @param mean the mean of the distribution
	 * @param sd   the standard deviation of the distribution
	 * @return a NegBinomialDistribution instance with the specified mean and
	 *         standard deviation
	 */
	public static NegBinomialDistribution negBinom(double mean, double sd) {
		return ImmutableNegBinomialDistribution.builder().setMean(mean).setSd(sd)
				.build();
	}

	/**
	 * Factory methods for normal distribution
	 *
	 * @param mean the mean of the distribution
	 * @param sd   the standard deviation of the distribution
	 * @return a NormalDistribution instance with the specified mean and standard
	 *         deviation
	 */
	public static NormalDistribution norm(Double mean, Double sd) {
		return ImmutableNormalDistribution.builder().setMean(mean).setSd(sd)
				.build();
	}

	/**
	 * Factory methods for point distribution
	 *
	 * @param mean the value of the distribution
	 * @return a PointDistribution instance with the specified value
	 */
	public static PointDistribution point(Double mean) {
		return ImmutablePointDistribution.builder().setMean(mean).build();
	}

	/**
	 * Factory methods for Poisson distribution
	 *
	 * @param rate the expected value (mean) of the distribution, which is also
	 *             the variance
	 * @return a PoissonDistribution instance with the specified mean
	 */
	public static PoissonDistribution pois(double rate) {
		return ImmutablePoissonDistribution.builder().setMean(rate).build();
	}

	/**
	 * Factory methods for uniform distribution
	 *
	 * @return a UniformDistribution instance with the default parameters (min=0,
	 *         max=1)
	 */
	public static UniformDistribution uniform() {
		return ImmutableUniformDistribution.builder().build();
	}

	/**
	 * Factory methods for uniform distribution with specified upper bound
	 *
	 * @param upper the maximum value of the distribution (min is fixed at 0)
	 * @return a UniformDistribution instance with the specified upper bound
	 */
	public static UniformDistribution uniform0(Double upper) {
		return ImmutableUniformDistribution.builder().setMax(upper).build();
	}

	/**
	 * Factory methods for unimodal beta distribution
	 *
	 * @param mean       the mean of the distribution
	 * @param dispersion the dispersion parameter of the distribution, which
	 *                   controls how tightly the distribution is concentrated
	 *                   around the mean. It is a number between 0 and 1 where 1
	 *                   is the most dispersed that can be achieved for a given
	 *                   mean, extreme means (close to 0 or 1) will have lower
	 *                   maximum standard deviation.
	 * @return a UnimodalBetaDistribution instance with the specified mean and
	 *         dispersion
	 */
	public static UnimodalBetaDistribution unimodalBeta(
			Double mean, Double dispersion
	) {
		return ImmutableUnimodalBetaDistribution.builder().setMean(mean)
				.setDispersion(dispersion).build();
	}

	@Override @Value.Default @JsonIgnore
	default double getMaxSupport() { return this.getLink().getMaxSupport(); }

	@Override @Value.Default @JsonIgnore
	default double getMinSupport() { return this.getLink().getMinSupport(); }

}
