package io.github.ai4ci.util;

public class Conversions {

	//EXPONENTIAL DISTRIBUTION
	
	/**
	 * The per unit time probability of transition when the average time
	 * to transition is given (in an exponential distribution model). The 
	 * period is longer than the half life.
	 * @param period an average time to event in days.  
	 * @return
	 */
	public static double probabilityFromPeriod(double period) {
		if (period <= 0) return 0;
		return probabilityFromRate(1/period);
	}
	
	public static double periodFromProbability(double p) {
		return 1/rateFromProbability(p);
	}
	
	/**
	 * The per unit time probability of occurrence of a single event 
	 * equivalent to a normalised rate of events per unit time, given the event 
	 * has not already occurred. 
	 * @param rate a beta parameter in a state space model is a rate (units per day)
	 * it is also the lambda parameter of the exponential distribution
	 */
	public static double probabilityFromRate(double rate) {
		if (rate <= 0) return 0;
		return 1-Math.exp(-rate);
	}
	
	public static double rateFromProbability(double p) {
		return -Math.log(1-p);
	}
	
	
	/**
	 * The per until time rate of events, given a single quantile of the 
	 * exponetial distribution. A 95% quantile of 10 days might be typical of
	 * an infectious disease duration for example. 
	 * @param period time in days
	 * @param quantile probability event before this.
	 * @return
	 */
	public static double rateFromQuantile(double period, double quantile) {
		if (period <= 0) return Double.POSITIVE_INFINITY;
		return -Math.log(1-quantile)/period;
	}
	
	public static double probabilityFromQuantile(double period, double quantile) {
		return probabilityFromRate(rateFromQuantile(period,quantile));
	}
	
	/**
	 * The probability of an event per unit time from the half life of many 
	 * samples. 
	 * @param halfLife a time in days
	 * @return
	 */
	public static double probabilityFromHalfLife(double halfLife) {
		if (halfLife <= 0) return 0;
		return probabilityFromQuantile(halfLife, 0.5);
	}
	
	public static double halfLifeFromProbability(double p) {
		return Math.log(0.5)/(Math.log(p));
	}
	
	// LOGISTIC AND ODDS
	
	public static double scaleRateByOR(double rate, double oddsRatio) {
		return rateFromProbability(
				scaleProbabilityByOR(
						probabilityFromRate(rate),
						oddsRatio
						)
		);
	}
	
	public static double oddsRatio(double p1, double p2) {
		return (p1/(1-p1)) / (p2/(1-p2));
	}
	
	public static double scaleProbabilityByOR(double p, double oddsRatio) {
		if (oddsRatio == Double.POSITIVE_INFINITY) return 1;
		if (oddsRatio == 1) return p;
		if (oddsRatio == 0) return 0;
		double tmp = expit(logit(p) + Math.log(oddsRatio));
		return tmp;
	}
	
	public static double scaleProbabilityByRR(double p, double rateRatio) {
		return p*rateRatio;
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
	
	public static double multiplyLogit(double logOdds1, double logOdds2) {
		return logit(expit(logOdds1)*expit(logOdds2));
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

	public static double rateRatio(double p1, double p2) {
		return p1/p2;
	}
}
