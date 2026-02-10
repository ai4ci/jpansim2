package io.github.ai4ci.util;

import java.util.stream.Collector;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.statistics.distribution.NormalDistribution;

/**
 * Represents a binomial proportion with numerator and denominator counts.
 * Provides methods for calculating probabilities, odds, and confidence intervals.
 * 
 * @author Rob Challen
 */
public class Binomial extends MutablePair<Long,Long> {
	
	/**
	 * Represents a confidence interval for a binomial proportion.
	 * 
	 * @author Rob Challen
	 */
	public static class Confidence extends ImmutablePair<Double, Double> {

		/**
		 * Constructs a Confidence interval with specified lower and upper bounds.
		 * 
		 * @param left the lower bound of the confidence interval
		 * @param right the upper bound of the confidence interval
		 */
		public Confidence(Double left, Double right) {
			super(left, right);
		}
		
		/**
		 * Returns a string representation of the confidence interval.
		 * 
		 * @return a formatted string showing the lower and upper bounds as percentages
		 */
		public String toString() {
			return String.format("[%.1f - %.1f]", left*100, right*100);
		}
		
		/**
		 * Gets the lower bound of the confidence interval.
		 * 
		 * @return the lower bound as a double
		 */
		public double lower() {
			return left;
		}
		
		/**
		 * Gets the upper bound of the confidence interval.
		 * 
		 * @return the upper bound as a double
		 */
		public double upper() {
			return right;
		}
		
	}
	
	private Binomial() {
		super(0L, 0L);
	}
	
	/**
	 * Gets the numerator count.
	 * 
	 * @return the numerator value
	 */
	public long getNumerator() {
		return this.getLeft();	}
	
	/**
	 * Gets the denominator count.
	 * 
	 * @return the denominator value
	 */
	public long getDenominator() {
		return this.getRight();	}
	
	
	/**
	 * Constructs a new Binomial with given numerator and denominator.
	 * 
	 * @param num the numerator
	 * @param denom the denominator
	 */
	public Binomial(long num, long denom) {
		super(num,denom);
	}
	
	/**
	 * Static factory method to create a Binomial instance.
	 * 
	 * @param num the numerator
	 * @param denom the denominator
	 * @return a new Binomial instance
	 */
	public static Binomial of(long num, long denom) {
		return new Binomial(num,denom);
	}
	
	/**
	 * Returns a collector that accumulates Binomial counts.
	 * 
	 * @return a Collector for Binomial instances
	 */
	public static Collector<Binomial, ?, Binomial> collect() {
		return Collector.of(Binomial::new, (p1,p2) -> p1.update(p2), Binomial::combine);
	}
	
	/**
	 * Returns a collector that converts boolean values increment numerator if true and denominator otherwise.
	 * 
	 * @return a Collector for boolean values
	 */
	public static Collector<Boolean, ?, Binomial> collectBinary() {
		return Collector.of(Binomial::new, (p,b) -> p.update(b ? 1 : 0, 1), Binomial::combine);
	}
	
	/**
	 * Combines two Binomial instances by summing their numerators and denominators.
	 * 
	 * @param left the first Binomial instance
	 * @param right the second Binomial instance
	 * @return a new Binomial instance with combined counts
	 */
	public static Binomial combine(Binomial left, Binomial right) {
		return Binomial.of(
				left.getLeft()+right.getLeft(),
				left.getRight()+right.getRight()
			);
	}

	/**
	 * Calculates the probability of the binomial proportion.
	 * 
	 * @return the probability as a double between 0 and 1
	 */
	public double probability() {
		if (this.right == 0) return(0);
		return ((double) this.left)/((double) this.right);
	}
	
	/**
	 * Calculates the odds of the binomial proportion.
	 * 
	 * @return the odds as a double
	 */
	public double odds() {
		return probability() / (1-probability());
	}
	
	/**
	 * Calculates the ratio of the binomial proportion.
	 * 
	 * @return the ratio as a double
	 */
	public double ratio() {
		if ((this.right-this.left) == 0) return 0;
		return ((double) this.left)/(this.right-this.left);
	}
	
	/**
	 * Updates the binomial counts by adding the counts from another Binomial instance.
	 * 
	 * @param toAdd the Binomial instance to add
	 */
	public void update(Binomial toAdd) {
		update(toAdd.getLeft(), toAdd.getRight());
	}
	
	/**
	 * Updates the binomial counts by adding specified numerator and denominator values.
	 * 
	 * @param num the numerator to add
	 * @param denom the denominator to add
	 */
	public void update(long num, long denom) {
		this.left += num;
		this.right += denom;
	}
	
	/**
	 * Calculates the Wilson score interval for the binomial proportion.
	 * 
	 * @param interval the confidence interval (e.g., 0.05 for 95% confidence)
	 * @return a Confidence instance representing the interval
	 */
	public Confidence wilson(double interval) {
		double z = NormalDistribution.of(0, 1).inverseCumulativeProbability(1-interval/2);
		double p = probability();
		long n = right;
		double tmp = z/(2*n)*Math.sqrt(4*n*p*(1-p)+z*z);
		double tmp2 = p+(z*z)/(2*n);
		double tmp3 = 1/(1+(z*z)/n);
		return new Confidence(
				tmp3*(tmp2-tmp),
				tmp3*(tmp2+tmp)
		);
	}
	
	/**
	 * Returns a string representation of the binomial proportion.
	 * 
	 * @return a formatted string with probability, confidence interval, and counts
	 */
	public String toString() {
		return String.format("%.1f %s (%d/%d)", probability()*100, wilson(0.05), this.left, this.right);
	}
	
	/**
	 * Checks if the binomial proportion is confidently greater than a specified value.
	 * 
	 * @param p the value to compare against
	 * @param conf the confidence level (e.g., 0.05 for 95% confidence)
	 * @return true if the lower bound of the confidence interval is greater than p
	 */
	public boolean confidentlyGreaterThan(double p, double conf) {
		return this.wilson(conf).lower() > p;
	}
	
	/**
	 * Checks if the binomial proportion is confidently less than a specified value.
	 * 
	 * @param p the value to compare against
	 * @param conf the confidence level (e.g., 0.05 for 95% confidence)
	 * @return true if the upper bound of the confidence interval is less than p
	 */
	public boolean confidentlyLessThan(double p, double conf) {
		return this.wilson(conf).upper() < p;
	}
}