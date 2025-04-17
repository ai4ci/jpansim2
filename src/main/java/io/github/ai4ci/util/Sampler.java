package io.github.ai4ci.util;


import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.rng.UniformRandomProvider;
import org.apache.commons.statistics.distribution.BetaDistribution;
import org.apache.commons.statistics.distribution.BinomialDistribution;
import org.apache.commons.statistics.distribution.GammaDistribution;
import org.apache.commons.statistics.distribution.LogNormalDistribution;
import org.apache.commons.statistics.distribution.PascalDistribution;
import org.apache.commons.statistics.distribution.PoissonDistribution;


public class Sampler implements UniformRandomProvider {

	private static ThreadLocal<Sampler> INSTANCE = ThreadLocal.withInitial(() -> new Sampler()); 
	
	public static Sampler getSampler() {
		return INSTANCE.get();
	}
	
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
	
	public synchronized double uniform() {
		return random.nextDouble();
	}
	
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
	
	public synchronized double logNormal(double mean, double sd) {
		if (sd==0) return mean;
		return logNormalfromMeanAndSd(mean, sd).createSampler(this).sample();
		
	}
	
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
	
	public synchronized int poisson(double mean) {
		if (mean == 0) return 0;
		return PoissonDistribution.of(mean).createSampler(this).sample();
	}
	
	public synchronized int zeroInflatedPoisson(double probabilityZero, double poissonMean) {
		if (this.uniform() < probabilityZero) return 0;
		return poisson(poissonMean);
	}
	
	public synchronized int negBinom(double mean, double sd) {
		int r = (int) Math.round((mean*mean) / (sd*sd - mean));
		double p = mean / (sd*sd);
		return PascalDistribution.of(r,p).createSampler(this).sample();
	}
	
	public synchronized int binom(int count, double probability) {
		if (count < 0) 
			throw new OutOfRangeException("Binomial count is <= 0");
		return BinomialDistribution.of(count,probability).createSampler(this).sample();
	}
	
	public synchronized int binom(double mean, double sd) {
		// TODO: do something if var>mean
		int n = (int) Math.round(mean/(1-(sd*sd)/mean));
		double p = mean/n;
		return BinomialDistribution.of(n,p).createSampler(this).sample();
	}
	
	public synchronized double gamma(double mean) {
		return GammaDistribution.of(mean,1).createSampler(this).sample();
	}
	
	public synchronized double beta(double mean, double sd, boolean convex) {
		if (convex) {
			// This constraint makes beta distribution convex and therefore unimodal
			// without this beta samples tend to diverge to bernoiulla 
			double sigma = Math.sqrt(Math.min(
					  mean*mean*(1-mean)/(1+mean),
					  (1-mean)*(1-mean)*(mean)/(2-mean)
					));
			if (sd > sigma) sd = sigma;
			// TODO: emit warning?
		}
		double tmp = mean*(1-mean)/(sd*sd)-1;
		double alpha = tmp*mean;
		double beta = tmp*(1-mean);
		return BetaDistribution.of(alpha, beta).createSampler(this).sample();
	}
	
	public synchronized double gamma(double mean, double sd) {
		double shape = (mean*mean)/(sd*sd);
		double scale = (sd*sd)/mean;
		return GammaDistribution.of(shape,scale).createSampler(this).sample();
	}

	@Override
	public synchronized long nextLong() {
		return random.nextLong();
	}

	public boolean bern(double jointDetect) {
		return random.nextDouble() < jointDetect;
	}
	
	public boolean rateTrigger(double rate) {
		return bern(Conversions.probabilityFromRate(rate));
	}
	
	public boolean periodTrigger(double period) {
		return bern(Conversions.probabilityFromRate(1.0/period));
	}
	
	public double sample(SimpleDistribution dist) {
		return dist.sample(this);
	}
	
	public int sampleInt(SimpleDistribution dist) {
		return (int) Math.floor(dist.sample(this));
	}
}
