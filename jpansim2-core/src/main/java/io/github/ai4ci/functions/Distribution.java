package io.github.ai4ci.functions;

import java.util.stream.IntStream;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import io.github.ai4ci.flow.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.util.Sampler;

/**
 * Interface for probability distributions used in the modeling framework.
 *
 * <p>
 * Supports a wide variety of statistical distributions including binomial,
 * Poisson, negative binomial, gamma, normal, log-normal, beta, uniform, and
 * empirical distributions. Provides sampling capabilities and statistical
 * transformations.
 */
@JsonTypeInfo(
		use = Id.NAME,
		requireTypeIdForSubtypes = OptBoolean.TRUE
)
@JsonSubTypes(
	{ @Type(
			value = ImmutableBinomialDistribution.class,
			name = "binomial"
	), @Type(value = ImmutablePoissonDistribution.class,
			name = "poisson"
	), @Type(value = ImmutableNegBinomialDistribution.class,
			name = "neg-binomial"
	), @Type(value = ImmutableGammaDistribution.class,
			name = "gamma"
	), @Type(value = ImmutableNormalDistribution.class,
			name = "normal"
	), @Type(value = ImmutableLogNormalDistribution.class,
			name = "log-normal"
	), @Type(value = ImmutableLogitNormalDistribution.class,
			name = "logit-normal"
	), @Type(value = ImmutableBetaDistribution.class,
			name = "beta"
	), @Type(value = ImmutableUnimodalBetaDistribution.class,
			name = "unimodal-beta"
	), @Type(value = ImmutableUniformDistribution.class,
			name = "uniform"
	), @Type(value = ImmutablePointDistribution.class,
			name = "uniform"
	), @Type(value = ImmutableEmpiricalDistribution.class,
			name = "empirical"
	)

	}
)
public interface Distribution {

	/** Number of samples used for empirical statistics */
	int PRECISION = 10000;

	/** Increment used for numerical differentiation */
	double DX = 0.00001;

	/**
	 * Combines this distribution with another using a specified combination
	 * function.
	 *
	 * @param with  the distribution to combine with
	 * @param using the function to combine the two distributions
	 * @return a resampled distribution representing the combination
	 */
	default ResampledDistribution combine(
			Distribution with, BiFunction<Double, Double, Double> using
	) {
		return ImmutableResampledDistribution.builder().setFirst(this)
				.setSecond(with).setCombiner(using).setLink(LinkFunction.NONE)
				.build();
	}

	/**
	 * Applies the link function to a value, transforming it according to the
	 * distribution's link.
	 *
	 * @return the link function used for distribution transformations
	 */
	LinkFunction getLink();

	/**
	 * The maximum value in the distribution's support. This is used for sampling
	 * and statistical calculations.
	 *
	 * @return the maximum value in the distribution's support
	 */
	@Value.NonAttribute
	double getMaxSupport();

	/**
	 * The minimum value in the distribution's support. This is used for sampling
	 * and statistical calculations.
	 *
	 * @return the minimum value in the distribution's support
	 */
	@Value.NonAttribute
	double getMinSupport();

	/**
	 * Generates multiple samples from the distribution for empirical analysis.
	 *
	 * @return array of PRECISION samples from the distribution
	 */
	@JsonIgnore @Value.Redacted @Value.Derived
	public default double[] getSamples() {
		return IntStream.range(0, PRECISION).mapToDouble(i -> this.sample())
				.toArray();
	}

	/**
	 * Generates a random sample using the default sampler.
	 *
	 * @return a random sample from the distribution
	 */
	@JsonIgnore
	default double sample() {
		return this.sample(Sampler.getSampler());
	}

	/**
	 * Generates a random sample from the distribution.
	 *
	 * @param rng the random number generator to use
	 * @return a random sample from the distribution
	 */
	double sample(Sampler rng);

}