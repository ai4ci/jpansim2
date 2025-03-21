package io.github.ai4ci.util;

public class Conversions {

	public static double probabilityFromPeriod(double period) {
		return probabilityFromRate(1/period);
	}
	
	public static double periodFromProbability(double p) {
		return 1/rateFromProbability(p);
	}
	
	public static double probabilityFromRate(double rate) {
		return 1-Math.exp(-rate);
	}
	
	public static double rateFromProbability(double p) {
		return -Math.log(1-p);
	}
	
	public static double probabilityFromHalfLife(double halfLife) {
		return Math.exp(Math.log(0.5)/ halfLife);
	}
	
	public static double halfLifeFromProbability(double p) {
		return Math.log(0.5)/(Math.log(p));
	}
	
	public static double scaleProbability(double p, double factor) {
		if (factor == Double.POSITIVE_INFINITY) return 1;
		if (factor == 1) return p;
		if (factor == 0) return 0;
		double tmp = expit(logit(p) + Math.log(factor));
		return tmp;
	}
	
	public static double probabilityFromOdds(double odds) {
		if (Double.isInfinite(odds)) return 1.0;
		return odds / (odds + 1);
	}
	
	public static double oddsFromProbability(double probability) {
		if (probability == 1.0) return Double.POSITIVE_INFINITY;
		return probability / (1-probability);
	}
	
	public static double logit(double p) {
		if (p == 0) return Double.NEGATIVE_INFINITY;
		if (p == 1) return Double.POSITIVE_INFINITY;
		return Math.log(oddsFromProbability(p));
	}
	
	public static double expit(double logOdds) {
		if (logOdds == Double.NEGATIVE_INFINITY) return 0;
		if (logOdds == Double.POSITIVE_INFINITY) return 1;
		return probabilityFromOdds(Math.exp(logOdds));
	}
	
	public static double waneLogOdds(double logOdds, double period, double maxPeriod) {
		if (period < 0) return 0;
		if (period > maxPeriod) return 0;
		if (maxPeriod == 0) return logOdds;
		// scale by x/(x+delay) such that at end point 
		// it is 10% of original value
		// 0.1 = x / (x + maxPeriod)
		double x = maxPeriod / 9.0;
		return logOdds * x / (x + period);
	}
}
