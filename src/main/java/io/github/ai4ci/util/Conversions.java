package io.github.ai4ci.util;

/**
 * Utility class providing mathematical conversions for probability, rates, and
 * odds transformations. This class implements conversions commonly used in
 * epidemiological and statistical modelling.
 *
 * <p>
 * <b>Key Mathematical Relationships:</b> \[ \newcommand{\prob}{p}
 * \newcommand{\rate}{\lambda} \newcommand{\period}{T} \newcommand{\odds}{o} \]
 * <ul>
 * <li><b>Exponential Distribution:</b> Probability from rate: \[ \prob = 1 -
 * e^{-\rate} \]</li>
 * <li><b>Logistic Transformation:</b> Logit function: \[ \text{logit}(\prob) =
 * \ln\left(\frac{\prob}{1-\prob}\right) \]</li>
 * <li><b>Odds Ratio Scaling:</b> Adjusted probability: \[
 * \text{logit}(\prob_{\text{adj}}) = \text{logit}(\prob) + \ln(OR) \]</li>
 * </ul>
 *
 * <p>
 * <b>Domain Constraints:</b>
 * <ul>
 * <li>Probabilities must be in range [0, 1]</li>
 * <li>Rates must be non-negative</li>
 * <li>Periods must be positive</li>
 * <li>Odds ratios must be positive finite values</li>
 * </ul>
 *
 * <p>
 * All methods handle edge cases appropriately, returning appropriate limits for
 * boundary inputs.
 *
 * @see Math#exp(double)
 * @see Math#log(double)
 */
public class Conversions {

	// EXPONENTIAL DISTRIBUTION

	/**
	 * Inverse logistic transformation (expit function). Converts log-odds to
	 * probability. \[ \text{expit}(x) = \frac{1}{1 + e^{-x}} \]
	 *
	 * @param logOdds log-odds value
	 * @return equivalent probability
	 */
	public static double expit(double logOdds) {
		if (logOdds == Double.NEGATIVE_INFINITY) return 0;
		if (logOdds == Double.POSITIVE_INFINITY) return 1;
		return probabilityFromOdds(Math.exp(logOdds));
	}

	/**
	 * Calculates half-life from probability. Inverse of
	 * {@link #probabilityFromHalfLife(double)}. \[ T_{1/2} =
	 * \frac{\ln(0.5)}{\ln(1-\prob)} \]
	 *
	 * @param p daily probability
	 * @return half-life
	 */
	public static double halfLifeFromProbability(double p) {
		return Math.log(0.5) / (Math.log(1 - p));
	}

	/**
	 * Logistic transformation (logit function). Converts probability to
	 * log-odds. \[ \text{logit}(p) = \ln\left(\frac{p}{1-p}\right) \]
	 *
	 * @param p probability in range (0, 1)
	 * @return log-odds
	 */
	public static double logit(double p) {
		if (p == 0) return Double.NEGATIVE_INFINITY;
		if (p == 1) return Double.POSITIVE_INFINITY;
		return Math.log(oddsFromProbability(p));
	}

	/**
	 * Multiplies two probabilities on the log-odds scale. Equivalent to
	 * independent probability multiplication. \[ \text{logit}(p_1 \cdot p_2) =
	 * \text{logit}(p_1) + \text{logit}(p_2) \]
	 *
	 * @param logOdds1 first log-odds
	 * @param logOdds2 second log-odds
	 * @return combined log-odds
	 */
	public static double multiplyLogit(double logOdds1, double logOdds2) {
		return logit(expit(logOdds1) * expit(logOdds2));
	}

	/**
	 * Converts probability to odds. Inverse of
	 * {@link #probabilityFromOdds(double)}. \[ \odds = \frac{p}{1-p} \]
	 *
	 * @param probability probability value
	 * @return equivalent odds
	 */
	public static double oddsFromProbability(double probability) {
		if (probability == 1.0) return Double.POSITIVE_INFINITY;
		return probability / (1 - probability);
	}

	/**
	 * Calculates the odds ratio between two probabilities. \[ OR =
	 * \frac{\odds_1}{\odds_2} = \frac{p_1/(1-p_1)}{p_2/(1-p_2)} \]
	 *
	 * @param p1 first probability
	 * @param p2 second probability
	 * @return odds ratio
	 */
	public static double oddsRatio(double p1, double p2) {
		return (p1 / (1 - p1)) / (p2 / (1 - p2));
	}

	/**
	 * Converts a probability to the equivalent average time period. Inverse of
	 * {@link #probabilityFromPeriod(double)}. \[ \period =
	 * -\frac{1}{\ln(1-\prob)} \]
	 *
	 * @param p probability in range [0, 1)
	 * @return average time period
	 * @throws IllegalArgumentException if probability is 1 (infinite period)
	 */
	public static double periodFromProbability(double p) {
		return 1 / rateFromProbability(p);
	}

	/**
	 * Calculates probability from half-life using the median (50th percentile).
	 * \[ \prob = 1 - \exp\left(-\frac{\ln(2)}{T_{1/2}}\right) \]
	 *
	 * @param halfLife median time to event
	 * @return daily probability
	 */
	public static double probabilityFromHalfLife(double halfLife) {
		if (halfLife <= 0) return 0;
		return probabilityFromQuantile(halfLife, 0.5);
	}

	// LOGISTIC AND ODDS TRANSFORMATIONS

	/**
	 * Converts odds to probability. \[ p = \frac{\odds}{1 + \odds} \]
	 *
	 * @param odds odds value
	 * @return equivalent probability
	 */
	public static double probabilityFromOdds(double odds) {
		if (Double.isInfinite(odds)) return 1.0;
		return odds / (odds + 1);
	}

	/**
	 * Converts an average time period to the per-unit-time probability of
	 * transition under an exponential distribution model. \[ \prob = 1 -
	 * e^{-1/\period} \]
	 *
	 * @param period average time to event (must be positive)
	 * @return probability of event occurring in one time unit
	 * @throws IllegalArgumentException if period is negative
	 */
	public static double probabilityFromPeriod(double period) {
		if (period <= 0) return 1;
		return probabilityFromRate(1 / period);
	}

	/**
	 * Calculates the probability from a specified quantile and period. \[ \prob
	 * = 1 - \exp\left(-\frac{\ln(1-q)}{\period}\right) \]
	 *
	 * @param period   time period
	 * @param quantile probability threshold
	 * @return daily probability
	 */
	public static double probabilityFromQuantile(
			double period, double quantile
	) {
		return probabilityFromRate(rateFromQuantile(period, quantile));
	}

	/**
	 * Converts a rate parameter to the per-unit-time probability of event
	 * occurrence under an exponential distribution assumption. \[ \prob = 1 -
	 * e^{-\rate} \] Commonly used to convert transmission rates to daily
	 * probabilities.
	 *
	 * @param rate event rate (lambda parameter of exponential distribution)
	 * @return probability of event in one time unit
	 */
	public static double probabilityFromRate(double rate) {
		if (rate <= 0) return 0;
		return 1 - Math.exp(-rate);
	}

	/**
	 * Converts a probability to the equivalent rate parameter. Inverse of
	 * {@link #probabilityFromRate(double)}. \[ \rate = -\ln(1-\prob) \]
	 *
	 * @param p probability in range [0, 1)
	 * @return equivalent rate parameter
	 * @throws IllegalArgumentException if probability is 1 (infinite rate)
	 */
	public static double rateFromProbability(double p) {
		return -Math.log(1 - p);
	}

	/**
	 * Calculates the rate parameter given a specific quantile of the exponential
	 * distribution. Useful when specifying durations using percentiles rather
	 * than means. \[ \rate = -\frac{\ln(1-q)}{\period} \]
	 *
	 * @param period   time period for the quantile
	 * @param quantile probability threshold (e.g., 0.95 for 95th percentile)
	 * @return rate parameter
	 */
	public static double rateFromQuantile(double period, double quantile) {
		if (period <= 0) return Double.POSITIVE_INFINITY;
		return -Math.log(1 - quantile) / period;
	}

	/**
	 * Calculates rate ratio between two probabilities. Simple ratio on
	 * probability scale. \[ RR = \frac{p_1}{p_2} \]
	 *
	 * @param p1 first probability
	 * @param p2 second probability
	 * @return rate ratio
	 */
	public static double rateRatio(double p1, double p2) {
		return p1 / p2;
	}

	/**
	 * Scales a probability by an odds ratio using logistic transformation. \[
	 * p_{\text{adj}} = \text{expit}(\text{logit}(p) + \ln(OR)) \]
	 *
	 * @param p         original probability
	 * @param oddsRatio multiplicative factor
	 * @return adjusted probability
	 */
	public static double scaleProbabilityByOR(double p, double oddsRatio) {
		if (oddsRatio == Double.POSITIVE_INFINITY) return 1;
		if (oddsRatio == 1) return p;
		if (oddsRatio == 0) return 0;
		double tmp = expit(logit(p) + Math.log(oddsRatio));
		return tmp;
	}

	/**
	 * Scales a probability by a rate ratio (simple multiplication). \[
	 * p_{\text{adj}} = p \cdot RR \] Note: Result may exceed 1.0 if RR > 1/p.
	 *
	 * @param p         original probability
	 * @param rateRatio multiplicative factor
	 * @return scaled probability
	 */
	public static double scaleProbabilityByRR(double p, double rateRatio) {
		return p * rateRatio;
	}

	/**
	 * Scales a rate by an odds ratio using logistic transformation. Applies the
	 * odds ratio on the log-odds scale.
	 *
	 * @param rate      original rate parameter
	 * @param oddsRatio multiplicative factor on odds scale
	 * @return scaled rate
	 */
	public static double scaleRateByOR(double rate, double oddsRatio) {
		return rateFromProbability(
				scaleProbabilityByOR(probabilityFromRate(rate), oddsRatio)
		);
	}

	/**
	 * Applies exponential waning to log-odds over time. Models diminishing
	 * effect with hyperbolic decay. \[ \text{logOdds}_{\text{waned}} =
	 * \text{logOdds} \cdot \frac{x}{x + t} \]
	 *
	 * @param logOdds   original log-odds
	 * @param period    current time period
	 * @param maxPeriod maximum period for full waning
	 * @return waned log-odds
	 */
	public static double waneLogOdds(
			double logOdds, double period, double maxPeriod
	) {
		if ((period < 0) || (period > maxPeriod)) return 0;
		if (maxPeriod == 0) return logOdds;
		// scale by x/(x+delay) such that at end point
		// it is 10% of original value
		// 0.1 = x / (x + maxPeriod)
		double x = maxPeriod / 9.0;
		return logOdds * x / (x + period);
	}
}