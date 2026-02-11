package io.github.ai4ci.util;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.BinomialDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.apache.commons.statistics.distribution.LogNormalDistribution;
import org.apache.commons.statistics.distribution.PascalDistribution;
import org.apache.commons.statistics.distribution.PoissonDistribution;

import io.github.ai4ci.functions.SimpleDistribution;

/**
 * A thread local random number generator provider with convenience sampling
 * methods.
 *
 * <p>
 * This class offers a thread local instance of a Mersenne Twister generator and
 * convenience methods to sample common distributions. Methods are synchronised
 * where the underlying generator is used to ensure thread safety when an
 * instance is shared across code that does not manage concurrency externally.
 * </p>
 *
 * <p>
 * Downstream uses: this class is used across the project wherever one-shot
 * samples or small stochastic simulations are required.
 * </p>
 *
 * @author Rob Challen
 */
public class Sampler implements UniformRandomProvider {

	/**
	 * Exception thrown when a distribution parameter is out of range.
	 */
	public static class OutOfRangeException extends RuntimeException {
		OutOfRangeException(String s) {
			super(s);
		}
	}

	private static ThreadLocal<Sampler> INSTANCE = ThreadLocal
			.withInitial(() -> new Sampler());

	/**
	 * Obtain the thread local sampler instance.
	 *
	 * @return the thread local sampler
	 */
	public static Sampler getSampler() { return INSTANCE.get(); }

	/**
	 * This provides access to a thread local RNG with a given seed. It sets that
	 * seed for the thread which will affect future users of the sampler.
	 *
	 * @param urn an identifier string used to set the seed
	 *
	 * @return the thread local sampler with the seed set for this thread
	 */
	public static Sampler getSampler(String urn) {
		return INSTANCE.get().withSeed(urn);
	}

	private static double invLogit(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	private static double logit(double p) {
		return Math.log(p / (1 - p));
	}

	private static LogNormalDistribution logNormalfromMeanAndSd(
			double mean, double sd
	) {
		if (mean <= 0) {
			throw new OutOfRangeException("Log normal mean is <= 0");
		}
		if (sd <= 0) { throw new OutOfRangeException("Log normal sd is < 0"); }
		var mu = Math.log(mean / (Math.sqrt(Math.pow(sd / mean, 2) + 1)));
		var sigma = Math.sqrt(Math.log(Math.pow(sd / mean, 2) + 1));
		return LogNormalDistribution.of(mu, sigma);
	}

	MersenneTwister random;

	long seed;

	private Sampler() {
		this.seed = System.currentTimeMillis();
		this.random = new MersenneTwister(this.seed);
	}

	private Sampler(String urn) {
		this();
		this.withSeed(urn);
	}

	/**
	 * Bernoulli trial returning true with probability jointDetect.
	 *
	 * @param jointDetect probability of success
	 * @return true if event occurs
	 */
	public boolean bern(double jointDetect) {
		return this.random.nextDouble() < jointDetect;
	}

	/**
	 * Bernoulli trial returning value with probability p, otherwise empty.
	 *
	 * @param <X>   the type of the value to return on success
	 * @param p     probability of success
	 * @param value the value to return if success
	 * @return Optional containing value if event occurs, otherwise empty
	 */
	public <X> Optional<X> bern(Double p, X value) {
		if (this.bern(p)) { return Optional.of(value); }
		return Optional.empty();
	}

	/**
	 * Draw a Beta sample parameterised by mean and sd. If convex is true the sd
	 * is scaled to ensure a unimodal beta distribution.
	 *
	 * @param mean   the mean in (0,1)
	 * @param sd     the standard deviation or a fraction when convex is true
	 * @param convex whether to constrain sd for unimodality
	 * @return a Beta sample in (0,1)
	 */
	public synchronized double beta(double mean, double sd, boolean convex) {
		if (convex) {
			// This constraint makes beta distribution convex and therefore
			// unimodal
			var sigma = Math.sqrt(
					Math.min(
							mean * mean * (1 - mean) / (1 + mean),
							(1 - mean) * (1 - mean) * (mean) / (2 - mean)
					)
			);
			sd = sd * sigma;
		}
		var tmp = mean * (1 - mean) / (sd * sd) - 1;
		if (tmp <= 0) { tmp = Double.MIN_NORMAL; }
		var alpha = tmp * mean;
		var beta = tmp * (1 - mean);
		return BetaDistribution.of(alpha, beta).createSampler(this).sample();
	}

	/**
	 * Draw a Binomial sample parameterised by mean and sd. The implied count is
	 * derived from the supplied mean and sd.
	 *
	 * @param mean the mean
	 * @param sd   the standard deviation
	 * @return a Binomial sample
	 */
	public synchronized int binom(double mean, double sd) {
		var n = (int) Math.round(mean / (1 - (sd * sd) / mean));
		if (n < 0) {
			throw new OutOfRangeException(
					"SD is too big and implied binomial count is <= 0"
			);
		}
		var p = mean / n;
		return BinomialDistribution.of(n, p).createSampler(this).sample();
	}

	/**
	 * Draw a Binomial sample with fixed count and probability.
	 *
	 * @param count       number of trials (non-negative)
	 * @param probability success probability
	 * @return Binomial sample
	 */
	public synchronized int binom(int count, double probability) {
		if (count < 0) {
			throw new OutOfRangeException("Binomial count is <= 0");
		}
		return BinomialDistribution.of(count, probability).createSampler(this)
				.sample();
	}

	/**
	 * Draw a Gamma( mean, 1 ) sample.
	 *
	 * @param mean the mean
	 * @return a Gamma sample
	 */
	public synchronized double gamma(double mean) {
		return GammaDistribution.of(mean, 1).createSampler(this).sample();
	}

	/**
	 * Draw a Gamma sample parameterised by mean and sd.
	 *
	 * @param mean the mean
	 * @param sd   the standard deviation
	 * @return a Gamma sample
	 */
	public synchronized double gamma(double mean, double sd) {
		var shape = (mean * mean) / (sd * sd);
		var scale = (sd * sd) / mean;
		return GammaDistribution.of(shape, scale).createSampler(this).sample();
	}

	/**
	 * Draw a logit-normal sample using median and scale.
	 *
	 * @param median the median probability
	 * @param scale  a scale parameter controlling dispersion
	 * @return a sample in (0,1)
	 */
	public synchronized double logitNormal(double median, double scale) {
		var mu = logit(median);
		// double sigma = scale*(1+Math.abs(mu));
		var sigma = scale * (2 + Math.pow(Math.abs(mu), (7.0 / 4.0)));
		return invLogit(this.random.nextGaussian() * sigma + mu);
	}

	/**
	 * Draw a log-normal sample parameterised by mean and sd.
	 *
	 * @param mean the mean (>0)
	 * @param sd   the standard deviation (>0)
	 * @return a log-normal sample
	 */
	public synchronized double logNormal(double mean, double sd) {
		if (sd == 0) { return mean; }
		return logNormalfromMeanAndSd(mean, sd).createSampler(this).sample();

	}

	/**
	 * Draw a sample from a multinomial distribution defined by the provided
	 * probabilities and associated values. The probabilities should sum to 1 or
	 * less, with any shortfall representing the probability of an empty outcome.
	 *
	 * @param <X>           the type of the values to return on success
	 * @param probabilities array of pairs of (probability, value)
	 * @return Optional containing the sampled value if an outcome occurs,
	 *         otherwise empty
	 */
	@SafeVarargs
	public final <X> Optional<X> multinom(Pair<Double, X>... probabilities) {
		var tmp = this.uniform();
		for (Pair<Double, X> element : probabilities) {
			if (tmp < element.getKey()) { return Optional.of(element.getValue()); }
			tmp = tmp - element.getKey();
		}
		return Optional.empty();
	}

	/**
	 * Draw a negative binomial sample parameterised by mean and sd.
	 *
	 * @param mean the mean
	 * @param sd   the standard deviation
	 * @return a Pascal (negative binomial) sample
	 */
	public synchronized int negBinom(double mean, double sd) {
		var r = (int) Math.round((mean * mean) / (sd * sd - mean));
		var p = mean / (sd * sd);
		return PascalDistribution.of(r, p).createSampler(this).sample();
	}

	@Override
	public synchronized long nextLong() {
		return this.random.nextLong();
	}

	/**
	 * Draw a normal sample with specified mean and standard deviation.
	 *
	 * @param mean the mean
	 * @param sd   the standard deviation
	 * @return a normal sample
	 */
	public synchronized double normal(double mean, double sd) {
		if (sd == 0) { return mean; }
		return this.random.nextGaussian() * sd + mean;
	}

	/**
	 * Random exponential model trigger with average period.
	 *
	 * @param period average period
	 * @return true if event occurs
	 */
	public boolean periodTrigger(double period) {
		return this.bern(Conversions.probabilityFromPeriod(period));
	}

	/**
	 * Random exponential model trigger using a specified quantile.
	 *
	 * @param period   average period
	 * @param quantile quantile used to derive probability
	 * @return true if event occurs
	 */
	public boolean periodTrigger(double period, double quantile) {
		return this.bern(Conversions.probabilityFromQuantile(period, quantile));
	}

	/**
	 * Draw a Poisson sample with given mean.
	 *
	 * @param mean the mean (>=0)
	 * @return a Poisson sample
	 */
	public synchronized int poisson(double mean) {
		if (mean == 0) { return 0; }
		return PoissonDistribution.of(mean).createSampler(this).sample();
	}

	/**
	 * Convert a per-unit rate to a Bernoulli outcome using
	 * Conversions.probabilityFromRate(rate).
	 *
	 * @param rate the rate per unit time
	 * @return true if event occurs
	 */
	public boolean rateTrigger(double rate) {
		return this.bern(Conversions.probabilityFromRate(rate));
	}

	/**
	 * Sample from a provided SimpleDistribution using this sampler.
	 *
	 * @param dist the distribution to sample from
	 * @return a sample
	 */
	public double sample(SimpleDistribution dist) {
		return dist.sample(this);
	}

	/**
	 * Sample an integer by flooring the continuous sample from the given
	 * distribution.
	 *
	 * @param dist the distribution to sample from
	 * @return an integer sample
	 */
	public int sampleInt(SimpleDistribution dist) {
		return (int) Math.floor(dist.sample(this));
	}

	/**
	 * Draw a uniform sample on [0,1).
	 *
	 * @return a uniform double in [0,1)
	 */
	public synchronized double uniform() {
		return this.random.nextDouble();
	}

	/**
	 * Draw a uniform sample on [min, max).
	 *
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (exclusive)
	 * @return a uniform double in [min, max)
	 */
	public double uniform(double min, double max) {
		return this.uniform() * (max - min) + min;
	}

	/**
	 * Set the sampler seed derived from the provided string and return this
	 * instance for fluent use.
	 *
	 * @param urn an identifier string used to set the seed
	 * @return this sampler instance
	 */
	public Sampler withSeed(String urn) {
		long tmp = (urn + ":thread:" + Thread.currentThread().getId()).hashCode();
		if (this.seed != tmp) {
			this.seed = tmp;
			this.random.setSeed(tmp);
		}
		return this;
	}

	/**
	 * Draw from a zero-inflated Poisson distribution.
	 *
	 * @param probabilityZero the mass at zero
	 * @param poissonMean     the Poisson mean
	 * @return the sample
	 */
	public synchronized int zeroInflatedPoisson(
			double probabilityZero, double poissonMean
	) {
		if (this.uniform() < probabilityZero) { return 0; }
		return this.poisson(poissonMean);
	}

}