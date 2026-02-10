package io.github.ai4ci.util;


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

import java.util.Optional;

/**
 * A thread local random number generator provider with convenience
 * sampling methods.
 *
 * <p>This class offers a thread local instance of a Mersenne Twister
 * generator and convenience methods to sample common distributions.
 * Methods are synchronised where the underlying generator is used to
 * ensure thread safety when an instance is shared across code that does
 * not manage concurrency externally.</p>
 *
 * <p>Downstream uses: this class is used across the project wherever
 * one-shot samples or small stochastic simulations are required.</p>
 *
 * @author Rob Challen
 */
public class Sampler implements UniformRandomProvider {

    private static ThreadLocal<Sampler> INSTANCE = ThreadLocal.withInitial(() -> new Sampler()); 
    
    /**
     * Obtain the thread local sampler instance.
     *
     * @return the thread local sampler
     */
    public static Sampler getSampler() {
        return INSTANCE.get();
    }
    
    /**
     * Exception thrown when a distribution parameter is out of range.
     */
    public static class OutOfRangeException extends RuntimeException {
        OutOfRangeException(String s) {
            super(s);
        }
    }
    
    /*
     * This provides access to a thread local RNG with a given seed. It sets 
     * that seed for the thread which will affect future users of the sampler.
     * 
     */
    public static Sampler getSampler(String urn) {
        return INSTANCE.get().withSeed(urn);
    }
    
    MersenneTwister random;
    long seed;
    
    private Sampler() {
        seed = System.currentTimeMillis();
        random = new MersenneTwister(seed);
    }
    
    /**
     * Set the sampler seed derived from the provided string and return
     * this instance for fluent use.
     *
     * @param urn an identifier string used to set the seed
     * @return this sampler instance
     */
    public Sampler withSeed(String urn) {
        long tmp = (urn+":thread:"+Thread.currentThread().getId()).hashCode(); 
        if (seed != tmp) {
            seed = tmp;
            random.setSeed(tmp);
        }
        return this;
    }
    
    private Sampler(String urn) {
        this();
        withSeed(urn);
    }
    
    /**
     * Draw a uniform sample on [0,1).
     *
     * @return a uniform double in [0,1)
     */
    public synchronized double uniform() {
        return random.nextDouble();
    }
    
    /**
     * Draw a normal sample with specified mean and standard deviation.
     *
     * @param mean the mean
     * @param sd the standard deviation
     * @return a normal sample
     */
    public synchronized double normal(double mean, double sd) {
        if (sd==0) return mean;
        return random.nextGaussian()*sd+mean;
    }
    
    private static LogNormalDistribution logNormalfromMeanAndSd(double mean, double sd) {
        if (mean <= 0) throw new OutOfRangeException("Log normal mean is <= 0");
        if (sd <= 0) throw new OutOfRangeException("Log normal sd is < 0");
        double mu = Math.log(mean/(Math.sqrt(Math.pow(sd/mean,2)+1)));
        double sigma = Math.sqrt(Math.log(Math.pow(sd/mean,2)+1));
        return LogNormalDistribution.of(mu, sigma);
    }
    
    /**
     * Draw a log-normal sample parameterised by mean and sd.
     *
     * @param mean the mean (>0)
     * @param sd the standard deviation (>0)
     * @return a log-normal sample
     */
    public synchronized double logNormal(double mean, double sd) {
        if (sd==0) return mean;
        return logNormalfromMeanAndSd(mean, sd).createSampler(this).sample();
        
    }
    
    /**
     * Draw a logit-normal sample using median and scale.
     *
     * @param median the median probability
     * @param scale a scale parameter controlling dispersion
     * @return a sample in (0,1)
     */
    public synchronized double logitNormal(double median, double scale) {
        double mu = logit(median);
        // double sigma = scale*(1+Math.abs(mu));
        double sigma = scale*(2+Math.pow(Math.abs(mu),(7.0/4.0)));
        return invLogit(random.nextGaussian()*sigma+mu);
    }
    
    private static double logit(double p) {
        return Math.log(p/(1-p));
    }
    
    private static double invLogit(double x) {
        return 1/(1+Math.exp(-x));
    }
    
    /**
     * Draw a Poisson sample with given mean.
     *
     * @param mean the mean (>=0)
     * @return a Poisson sample
     */
    public synchronized int poisson(double mean) {
        if (mean == 0) return 0;
        return PoissonDistribution.of(mean).createSampler(this).sample();
    }
    
    /**
     * Draw from a zero-inflated Poisson distribution.
     *
     * @param probabilityZero the mass at zero
     * @param poissonMean the Poisson mean
     * @return the sample
     */
    public synchronized int zeroInflatedPoisson(double probabilityZero, double poissonMean) {
        if (this.uniform() < probabilityZero) return 0;
        return poisson(poissonMean);
    }
    
    /**
     * Draw a negative binomial sample parameterised by mean and sd.
     *
     * @param mean the mean
     * @param sd the standard deviation
     * @return a Pascal (negative binomial) sample
     */
    public synchronized int negBinom(double mean, double sd) {
        int r = (int) Math.round((mean*mean) / (sd*sd - mean));
        double p = mean / (sd*sd);
        return PascalDistribution.of(r,p).createSampler(this).sample();
    }
    
    /**
     * Draw a Binomial sample with fixed count and probability.
     *
     * @param count number of trials (non-negative)
     * @param probability success probability
     * @return Binomial sample
     */
    public synchronized int binom(int count, double probability) {
        if (count < 0) 
            throw new OutOfRangeException("Binomial count is <= 0");
        return BinomialDistribution.of(count,probability).createSampler(this).sample();
    }
    
    /**
     * Draw a Binomial sample parameterised by mean and sd. The implied
     * count is derived from the supplied mean and sd.
     *
     * @param mean the mean
     * @param sd the standard deviation
     * @return a Binomial sample
     */
    public synchronized int binom(double mean, double sd) {
        int n = (int) Math.round(mean/(1-(sd*sd)/mean));
        if (n < 0)
            throw new OutOfRangeException("SD is too big and implied binomial count is <= 0");
        double p = mean/n;
        return BinomialDistribution.of(n,p).createSampler(this).sample();
    }
    
    /**
     * Draw a Gamma( mean, 1 ) sample.
     *
     * @param mean the mean
     * @return a Gamma sample
     */
    public synchronized double gamma(double mean) {
        return GammaDistribution.of(mean,1).createSampler(this).sample();
    }
    
    /**
     * Draw a Beta sample parameterised by mean and sd. If convex is
     * true the sd is scaled to ensure a unimodal beta distribution.
     *
     * @param mean the mean in (0,1)
     * @param sd the standard deviation or a fraction when convex is true
     * @param convex whether to constrain sd for unimodality
     * @return a Beta sample in (0,1)
     */
    public synchronized double beta(double mean, double sd, boolean convex) {
        if (convex) {
            // This constraint makes beta distribution convex and therefore unimodal  
            double sigma = Math.sqrt(Math.min(
                      mean*mean*(1-mean)/(1+mean),
                      (1-mean)*(1-mean)*(mean)/(2-mean)
                    ));
            sd = sd*sigma;
        }
        double tmp = mean*(1-mean)/(sd*sd)-1;
        if (tmp <= 0) tmp = Double.MIN_NORMAL;
        double alpha = tmp*mean;
        double beta = tmp*(1-mean);
        return BetaDistribution.of(alpha, beta).createSampler(this).sample();
    }
    
    /**
     * Draw a Gamma sample parameterised by mean and sd.
     *
     * @param mean the mean
     * @param sd the standard deviation
     * @return a Gamma sample
     */
    public synchronized double gamma(double mean, double sd) {
        double shape = (mean*mean)/(sd*sd);
        double scale = (sd*sd)/mean;
        return GammaDistribution.of(shape,scale).createSampler(this).sample();
    }

    @Override
    public synchronized long nextLong() {
        return random.nextLong();
    }

    /**
     * Bernoulli trial returning true with probability jointDetect.
     *
     * @param jointDetect probability of success
     * @return true if event occurs
     */
    public boolean bern(double jointDetect) {
        return random.nextDouble() < jointDetect;
    }
    
    /**
     * Convert a per-unit rate to a Bernoulli outcome using
     * Conversions.probabilityFromRate(rate).
     *
     * @param rate the rate per unit time
     * @return true if event occurs
     */
    public boolean rateTrigger(double rate) {
        return bern(Conversions.probabilityFromRate(rate));
    }
    
    /**
     * Random exponential model trigger with average period.
     *
     * @param period average period
     * @return true if event occurs
     */
    public boolean periodTrigger(double period) {
        return bern(Conversions.probabilityFromPeriod(period));
    }
    
    /**
     * Random exponential model trigger using a specified quantile.
     *
     * @param period average period
     * @param quantile quantile used to derive probability
     * @return true if event occurs
     */
    public boolean periodTrigger(double period, double quantile) {
        return bern(Conversions.probabilityFromQuantile(period,quantile));
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
     * Sample an integer by flooring the continuous sample from the given distribution.
     *
     * @param dist the distribution to sample from
     * @return an integer sample
     */
    public int sampleInt(SimpleDistribution dist) {
        return (int) Math.floor(dist.sample(this));
    }
    
    @SafeVarargs
    public final <X> Optional<X> multinom(Pair<Double,X>... probabilities) {
        double tmp = this.uniform();
        for (int i=0; i<probabilities.length; i++) {
            if (tmp < probabilities[i].getKey()) return 
                    Optional.of(probabilities[i].getValue());
            tmp = tmp - probabilities[i].getKey();
        }
        return Optional.empty();
    }
    
    public <X> Optional<X> bern(Double p, X value) {
        if (bern(p)) return Optional.of(value);
        return Optional.empty();
    }

    public double uniform(double min, double max) {
        return uniform() * (max-min) + min;
    }
    
}