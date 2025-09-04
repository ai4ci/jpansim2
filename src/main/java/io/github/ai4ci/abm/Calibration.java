package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.HistogramDistribution;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * Various utilities to calibrate model to real world observations
 */
public class Calibration {

	static Logger log = LoggerFactory.getLogger(Calibration.class);

	/**
	 * This is used to baseline the outbreak and at the point of baselining the
	 * social network and demographics are set up. We should also have the
	 * individuals configured. This means we can look at their individual mobility
	 * and hence the probability of contact across the network.
	 */
	public static double contactsPerPersonPerDay(Outbreak outbreak) {

		ThreadSafeArray<SocialRelationship> social = outbreak.getSocialNetwork();
		double totalContactProbability = social.parallelStream().mapToDouble(r -> {
			Person person1 = r.getSource(outbreak);
			Person person2 = r.getTarget(outbreak);
			return r.contactProbability(person1.getBaseline().getMobilityBaseline(),
					person2.getBaseline().getMobilityBaseline());
		}).sum();
		// Each contact involves 2 people so we have to count them twice
		return totalContactProbability / outbreak.getPeople().size();

	}

	/**
	 * The maximum R0 a network can support is the result of an transmission for 
	 * every edge.
	 */
	public static double maxR0(Outbreak outbreak) {
		double[][] dd = outbreak.getExecutionConfiguration().getViralLoadProfile();
		return expectedExposuresPerInfection(outbreak,dd,1);
	}
	
	// Utility for aggregating risk of exposure over one viral load profile.
	private static double pAnyExposure(double pContact, double[] profile, double parameter) {
		double pNonExposure = 1; // pNonExposure
		for (double viralLoad: profile) {
			// In the main simulation this transmissibility value is modified by
			// various odds ratios
			var pExposureGivenContact = OutbreakBaseline.transmissibilityFromViralLoad(viralLoad, parameter);
			pNonExposure = pNonExposure * (1-pContact*pExposureGivenContact);
		}
		return 1-pNonExposure;
	}
	
	/**
	 * Identifies for a given transmission parameter, the expected number of
	 * exposures per infection, given social network topology and baseline contact
	 * probability between agents. This looks at the set of theoretical networks
	 * where one person is infected, and identifies for a sampled set of random viral
	 * load profiles, how infectious that person is at any given time, and how
	 * likely they are to be contacting someone.
	 */
	public static double expectedExposuresPerInfection(Outbreak outbreak, double[][] dd, double parameter) {
		
		return outbreak.getSocialNetwork().parallelStream().mapToDouble(r -> {
				Person person1 = r.getSource(outbreak);
				Person person2 = r.getTarget(outbreak);
				// Per day probability of contact between two people (baseline) on any day
				// This is equivalent to what happens in {@link Contact#contactNetwork}
				var pContact = r.contactProbability(
					person1.getBaseline().getMobilityBaseline(),
					person2.getBaseline().getMobilityBaseline()
				);
				
				double meanPAnyExposure = Arrays.stream(dd).parallel().mapToDouble(
					profile -> pAnyExposure(pContact, profile, parameter)
				).average().orElse(0);
				
				

				// This is the probability that an exposure passes over a 
				// single edge in the social network graph (assuming that 
				// one end of the edge is infected) it takes into account
				// the probability of contact and the delay in transmission
				// due to the in host (i.e. it is aggregated in time - like R0)
				return meanPAnyExposure;
				
			})
				.map( d-> d*outbreak.getSocialNetwork().size()*2/outbreak.getPopulationSize())
				.average().getAsDouble();
				// So here we need to look at spread of distribution in a way 
				// that is deterministic.  
		
			// factor of 2 because each edge has 2 vertices
			// so over all the edges in the social network the expected flux of 
			// the infection is the sum of the probability of exposures. This 
			// flux represents the networks capacity for next generation size 
			// assuming some sort of random state in previous generation 
			// or more accurately the sum of all possible states where 1 person 
			// is infected 
		
			// This distribution is likely to be very skewed (maybe depending
			// on the network. Also the exposures of exposures is going to be 
			// much larger again if there is any degree of correlation in the 
			// node degree
	}	

	/**
	 * The translation and the in host model viral load and transmission probability
	 * is a function
	 * {@link OutbreakBaseline#transmissibilityFromViralLoad(double, double)}. The
	 * in host viral load model has been run for a range of different parameters and
	 * the average response held in the viral load profile. The probability of a
	 * contact that generates one or more exposures over the whole infective
	 * period is calculated. The R0 value is the population average of the sum of
	 * each individuals secondary exposures. This is solved for R0 to get a parameter
	 * of the function that converts viral load to transmission probability per day. 
	 * This ignores the possibility of multiple exposure by different people, but this
	 * is OK for R0 calibration.
	 */
	public static double inferViralLoadTransmissionParameter(Outbreak outbreak, double R0) {

		// So we need to forward simulate the number of exposures given viral load for
		// every potential contact in the outbreak
		double[][] dd = outbreak.getExecutionConfiguration().getViralLoadProfile();
		return inferViralLoadTransmissionParameter(outbreak, dd, R0);
	}
		
	public static double inferViralLoadTransmissionParameter(Outbreak outbreak, double[][] dd, double R0) {
		BrentSolver solver = new BrentSolver(0.001);
		double adjR0 = adjustR0(outbreak, R0);
		UnivariateFunction fnR0 = (toInfer) -> {
			return expectedExposuresPerInfection(outbreak, dd, toInfer) - adjR0;
			//return expectedExposuresPerInfection(outbreak, dd, toInfer) - R0;
		};
		
		log.info("Max R0 for this network is: " + (fnR0.value(1) + adjR0));
		
		try {
			double inferred = solver.solve(10000, fnR0, 0, 1);
			log.info(
					"P(transmission|infectious contact with viral load 1.5): {}, for RO: {} (adjusted for network: {})",
					String.format("%1.4f",OutbreakBaseline.transmissibilityFromViralLoad(1.5, inferred)),
					R0, adjR0
			);
			return inferred;
			// return inferred * perc / maxPerc;
		} catch (NoBracketingException e) {
			throw new RuntimeException("No solution for R0 in the range 0 to 1");
		}
		
	}

	
	
	
	/**
	 * Calibrate severity cutoffs for events based on probability. Events might be
	 * hospitalisation, death, and be relative to infection so always a number less
	 * than 1. A small IFR correlates with a high cutoff for severity based on
	 * population severity distribution. Therefore for a 0.01 IFR we cut-off at the
	 * 0.99 quantile for severity.
	 * 
	 * @param outbreak            the outbreak
	 * @param infectionEventRatio a ratio between infected and those infected and
	 *                            e.g. hospitalised.
	 * @return a
	 */
	public static double inferSeverityCutoff(Outbreak outbreak, double infectionEventRatio) {

		EmpiricalDistribution dist = InHostConfiguration.getPeakSeverity(
				outbreak.getExecutionConfiguration().getInHostConfiguration(), 
				outbreak.getExecutionConfiguration(),
				1000, 50);
		// N.B. only interested in the in host peak hence we can use a shorter duration.
		double tmp = dist.getQuantile(1 - infectionEventRatio);
		return tmp;
	}

	/**
	 * Node (aka Person) temporal contact network degree based on probabilistic
	 * weighting of social network. This is a mean estimate of the degree of contact
	 * network for each node based on a single bernouilli trial for each social
	 * network edge with a per edge probability. The distribution would be the sum
	 * of bernoullis with different probabilities which is a Poisson Binomial
	 * Distribution
	 * {@see https://stats.stackexchange.com/questions/93852/sum-of-bernoulli-variables-with-different-success-probabilities}
	 * 
	 */
	public static double[] networkDegreePerPerson(Outbreak o) {
		AtomicDouble[] degrees = new AtomicDouble[o.getPopulationSize()];
		
		// AtomicDouble[] variances = new AtomicDouble[o.getPopulationSize()];
		IntStream.range(0, degrees.length).forEach(i -> {
			degrees[i] = new AtomicDouble(0);
			// variances[i] = new AtomicDouble(0);
		});
		o.getSocialNetwork().parallelStream().forEach(r -> {
			Person person1 = r.getSource(o);
			Person person2 = r.getTarget(o);
			double p = r.contactProbability(
					person1.getBaseline().getMobilityBaseline(),
					person2.getBaseline().getMobilityBaseline()
			);
			degrees[person1.getId()].addAndGet(p);
			// variances[person1.getId()].addAndGet(p * (1 - p));
			degrees[person2.getId()].addAndGet(p);
			// variances[person2.getId()].addAndGet(p * (1 - p));
		});
		return Arrays.stream(degrees).mapToDouble(d -> d.doubleValue()).toArray();
	}

	public static double averageContactDegree(Outbreak o) {
		double[] nodeDegree = networkDegreePerPerson(o);
		return DoubleStream.of(nodeDegree).average().orElse(0);
	}
	
	/**
	 * Computes the percolation threshold of the probabilistic contact network using the
	 * second moment of the degree distribution.
	 *
	 * <p>In epidemic percolation theory, the threshold for sustained transmission is determined by
	 * the structure of the contact network. For a random network, the critical transmissibility \( T_c \)
	 * at which a giant connected component emerges is:
	 * $$
	 * T_c = \frac{\langle k \rangle}{\langle k^2 \rangle - \langle k \rangle}
	 * $$
	 * where \( \langle k \rangle \) is the mean degree and \( \langle k^2 \rangle \) is the second moment
	 * of the degree distribution.
	 *
	 * <p>In this model, contact between agents is probabilistic, so the degree of each node is a
	 * sum of independent Bernoulli variables with different success probabilities \( p_{ij} \),
	 * forming a <i>Poisson binomial distribution</i>. The expected degree of node \( i \) is:
	 * $$
	 * \mathbb{E}[k_i] = \sum_{j} p_{ij}
	 * $$
	 * and the variance is:
	 * $$
	 * \mathrm{Var}(k_i) = \sum_{j} p_{ij}(1 - p_{ij})
	 * $$
	 * as per the properties of the Poisson binomial distribution.
	 *
	 * <p>The second moment \( \langle k^2 \rangle \) is estimated as:
	 * $$
	 * \langle k^2 \rangle = \langle k \rangle^2 + \langle \mathrm{Var}(k_i) \rangle
	 * $$
	 * where the latter term accounts for the excess variance due to heterogeneous \( p_{ij} \).
	 *
	 * <p>This method computes the average percolation threshold across the population,
	 * reflecting the network's susceptibility to epidemic spread. A lower threshold implies
	 * higher connectivity and easier outbreak propagation.
	 *
	 * @param o the outbreak configuration, including the social network and agent mobility
	 * @return the percolation threshold \( T_c \), interpreted as the minimum transmissibility
	 *         required for an epidemic to occur
	 *
	 * @see <a href="https://stats.stackexchange.com/questions/93852/sum-of-bernoulli-variables-with-different-success-probabilities">Poisson Binomial Distribution</a>
	 */
	public static double percolationThreshold(Outbreak o) {
		
		AtomicDouble[] degrees = new AtomicDouble[o.getPopulationSize()];
		AtomicDouble[] variances = new AtomicDouble[o.getPopulationSize()];
		
		IntStream.range(0, degrees.length).forEach(i -> {
			degrees[i] = new AtomicDouble(0);
			variances[i] = new AtomicDouble(0);
		});
		
		o.getSocialNetwork().parallelStream().forEach(r -> {
			Person person1 = r.getSource(o);
			Person person2 = r.getTarget(o);
			double p = r.contactProbability(
					person1.getBaseline().getMobilityBaseline(),
					person2.getBaseline().getMobilityBaseline()
			);
			degrees[person1.getId()].addAndGet(p);
			variances[person1.getId()].addAndGet(p * (1 - p));
			degrees[person2.getId()].addAndGet(p);
			variances[person2.getId()].addAndGet(p * (1 - p));
		});
		// N.B. this is the average percolation threshold (average of average contact
		// network degree ....
		double k = Arrays.stream(degrees).mapToDouble(d -> d.doubleValue()).average().orElse(0);
		// Technically this should be the average of the product of the same
		// Poisson Binomial Distributions, which is going to be wider than this.
		double varK = Arrays.stream(variances).mapToDouble(d -> d.doubleValue()).average().orElse(0);
		double k2 = Arrays.stream(degrees).mapToDouble(d -> d.doubleValue() * d.doubleValue()).average().orElse(0) + varK;
		return k / (k2 - k);
	}
	
	/**
	 * Adjusts the target \( R_0 \) to account for network structure effects by normalizing
	 * against the maximum possible percolation threshold of a fully connected network
	 * with the same mean degree.
	 *
	 * <p>The effective reproductive number in a network is constrained by its topology.
	 * The percolation threshold \( T_c = \langle k \rangle / (\langle k^2 \rangle - \langle k \rangle) \)
	 * determines the minimum transmissibility for an epidemic. In a fully connected network
	 * with homogeneous contact probability, \( \langle k^2 \rangle = \langle k \rangle^2 \), so:
	 * $$
	 * T_{\text{max}} = \frac{\langle k \rangle}{\langle k \rangle^2 - \langle k \rangle} = \frac{1}{\langle k \rangle - 1}
	 * $$
	 *
	 * <p>This method computes the ratio of the actual percolation threshold \( T_c \) to \( T_{\text{max}} \),
	 * which captures how much the network structure reduces or enhances transmission potential.
	 * The adjusted \( R_0 \) is then:
	 * $$
	 * R_0^{\text{adj}} = R_0 \cdot \frac{T_c}{T_{\text{max}}}
	 * $$
	 *
	 * <p>This adjustment ensures that the calibrated transmission parameter reflects only
	 * the biological mechanism (viral load → transmissibility), decoupled from network-specific
	 * structural effects. It allows for transferable parameter estimates across different
	 * population structures.
	 *
	 * <p>For example, a clustered or sparse network will have \( T_c > T_{\text{max}} \), leading to
	 * \( R_0^{\text{adj}} \lt R_0 \), meaning a higher intrinsic transmissibility is needed to achieve
	 * the same population-level \( R_0 \).
	 *
	 * @param o the outbreak configuration
	 * @param R0 the desired basic reproduction number in a well-mixed population
	 * @return the network-adjusted \( R_0 \) that compensates for reduced connectivity or clustering
	 *         in the actual social network
	 */
	public static double adjustR0(Outbreak o, double R0) {
		// N.B. this is the average percolation threshold (average of average contact
		// network degree ....)
		double k = averageContactDegree(o);
		double perc = percolationThreshold(o);
		// This is the percolation of a network
		double maxPerc = k / (k*k - k);
		return R0 * perc / maxPerc;
	}
	
	/**
	 * As per Koch et al 2013, Edge removal in contact networks eqn 2
	 */
	public static double perEdgeTransmissionProbability(double R0, Outbreak o) {
		double Tc = percolationThreshold(o);
		return R0 * Tc;
	}

	/**
	 * Infers the viral load transmission parameter that achieves a target R₀ using a fast,
	 * polynomial-based approximation.
	 *
	 * <p>This method is an optimized alternative to {@link #inferViralLoadTransmissionParameter(Outbreak, double)}.
	 * It uses the {@link Estimator} class to approximate the expected number of secondary exposures
	 * per infection via a precomputed quartic polynomial in the transmission parameter. The approximation
	 * is derived from a series expansion of the probability of at least one exposure over time,
	 * accounting for variable daily transmissibility and contact probability.
	 *
	 * <p>The method adjusts the target R₀ for network structure using {@link #adjustR0(Outbreak, double)}
	 * and solves for the transmission parameter using Brent's method. The result is significantly faster
	 * than the full simulation-based approach, making it suitable for repeated calibration tasks.
	 *
	 * @param outbreak the outbreak configuration, including social network and viral load profiles
	 * @param R0 the desired basic reproduction number to calibrate to
	 * @return the inferred transmission parameter (scales transmissibility per unit viral load above 1.0)
	 * @throws RuntimeException if no solution is found within the valid parameter range [0, 0.1]
	 */
	public static double inferViralLoadTransmissionParameterQuick(Outbreak outbreak, double R0) {
		double[][] viralLoadProfile = outbreak.getExecutionConfiguration().getViralLoadProfile();
		double [] contactProbability = outbreak.getSocialNetwork().parallelStream().mapToDouble(r -> {
				Person person1 = r.getSource(outbreak);
				Person person2 = r.getTarget(outbreak);
				// Per day probability of contact between two people (baseline) on any day
				// This is equivalent to what happens in {@link Contact#contactNetwork}
				return r.contactProbability(
					person1.getBaseline().getMobilityBaseline(),
					person2.getBaseline().getMobilityBaseline()
				);
			}).toArray();
		Estimator est = new Estimator(viralLoadProfile, contactProbability);
		BrentSolver solver = new BrentSolver(0.001);
		double adjR0 = adjustR0(outbreak,R0);
		long countAgents = outbreak.getPopulationSize();
		UnivariateFunction fnR0 = (toInfer) -> {
			return est.fastPTransmission(toInfer)*2/countAgents - adjR0;
		};
		
		try {
			double inferred = solver.solve(10000, fnR0, 0, 0.1);
			log.info(
					"P(transmission|infectious contact with viral load 1.5): {}, for RO: {} (adjusted for network: {})",
					String.format("%1.4f",OutbreakBaseline.transmissibilityFromViralLoad(1.5, inferred)),
					R0, adjR0
			);
			return inferred;
		} catch (NoBracketingException e) {
			throw new RuntimeException("No solution for R0 in the range 0 to 1");
		}
	}
	
	/**
	 * An optimized estimator for the expected probability of transmission across a social network edge
	 * given a transmission parameter and viral load profiles.
	 *
	 * <p>This class precomputes a quartic polynomial approximation of the expected transmission
	 * probability by leveraging the structure of the Poisson binomial distribution formed by
	 * independent daily exposure risks over an infection period. For each viral load profile,
	 * the excess viral load (above threshold 1.0) is summarized into raw moments \( B_1 \)–\( B_4 \),
	 * where \( B_n = \sum_t (\delta_t)^n \) and \( \delta_t = \max(v_t - 1, 0) \).
	 *
	 * <p>The probability of at least one exposure is \( 1 - \prod_t (1 - p_t) \), with \( p_t = k \cdot c \cdot \delta_t \).
	 * Using the cumulant generating function of the Poisson binomial distribution,
	 * this is approximated as:
	 * $$
	 * \mathbb{P}(\text{any exposure}) = 1 - \exp\left( \sum_t \log(1 - p_t) \right)
	 * $$
	 * Expanding the logarithm:
	 * $$
	 * \sum_t \log(1 - p_t) = -\sum_{n=1}^\infty \frac{(k c)^n}{n} \sum_t (\delta_t)^n = -\sum_{n=1}^\infty \frac{(k c)^n}{n} B_n
	 * $$
	 * Let \( S(c) = \sum_{n=1}^4 \frac{(k c)^n}{n} B_n \). Then:
	 * $$
	 * \mathbb{P}(\text{any exposure}) \approx 1 - e^{-S(c)}
	 * $$
	 * This is expanded as a power series in \( c \):
	 * $$
	 * 1 - e^{-S(c)} = a_1 c + a_2 c^2 + a_3 c^3 + a_4 c^4 + \cdots
	 * $$
	 * The coefficients \( a_1, a_2, a_3, a_4 \) are computed using combinatorial identities from the Taylor expansion,
	 * then averaged over all contact probabilities \( k_{ij} \) and viral load profiles \( m \).
	 *
	 * <p>The final surrogate model is:
	 * $$
	 * \hat{p}(c) = \overline{a_1} \cdot c + \overline{a_2} \cdot c^2 + \overline{a_3} \cdot c^3 + \overline{a_4} \cdot c^4
	 * $$
	 * enabling \( O(1) \) evaluation during root-finding. This avoids repeated time-series computations,
	 * accelerating R₀ calibration while preserving accuracy for realistic parameter ranges.
	 *
	 * <p>The underlying distribution of exposures is a Poisson binomial with:
	 * $$
	 * \mathbb{E}\left[\sum_t X_t\right] = \sum_t p_t, \quad \mathrm{Var}\left[\sum_t X_t\right] = \sum_t p_t(1 - p_t),
	 * $$
	 * as referenced in the Poisson binomial distribution literature. The independence of daily exposure events
	 * justifies the additive structure of the cumulants used in the expansion.
	 *
	 * @see <a href="https://stats.stackexchange.com/questions/93852/sum-of-bernoulli-variables-with-different-success-probabilities">Poisson Binomial Distribution</a>
	 */
	public static class Estimator {
		
		public Estimator(double[][] viralLoadProfiles, double[] contactProbabilities) {
			double[][] tmp = computeBProfiles(viralLoadProfiles);
			precomputeAverageCoefficients(contactProbabilities, tmp);
		}
		
		/**
		 * Precomputes the first four raw moments of the excess viral load (above threshold 1.0)
		 * for each simulated viral load profile, used to approximate transmission risk over time.
		 *
		 * <p>For each viral load profile \(  m  \) , define \(  \delta_t^{(m)} = \max(v_t^{(m)} - 1, 0)  \)  as the
		 * excess infectiousness on day \(  t  \) . The transmission probability on day \(  t  \)  is assumed to be
		 * linear in \(  \delta_t  \) : \(  p_t = k \cdot c \cdot \delta_t  \) , where \(  k  \)  is the contact probability
		 * and \(  c  \)  is the transmission parameter.
		 *
		 * <p>The probability of at least one exposure over the infectious period is:
		 * $$
		 * \mathbb{P}(\text{any exposure}) = 1 - \prod_{t=1}^T (1 - p_t)
		 * $$
		 * Taking logs of the survival probability:
		 * $$
		 * \log \mathbb{P}(\text{no exposure}) = \sum_{t=1}^T \log(1 - p_t) = -\sum_{n=1}^\infty \frac{(k c)^n}{n} \sum_{t=1}^T (\delta_t)^n
		 * $$
		 * Define the \(  n  \) -th raw moment of excess viral load as:
		 * $$
		 * B_n^{(m)} = \sum_{t=1}^T (\delta_t^{(m)})^n
		 * $$
		 * This method computes \(  B_1^{(m)}, B_2^{(m)}, B_3^{(m)}, B_4^{(m)}  \)  for each profile \(  m  \) ,
		 * which capture the mean, variance-like, skewness-like, and kurtosis-like contributions
		 * to the cumulant structure of the underlying Poisson binomial process.
		 *
		 * @param viralLoadData A 100x100 array: [profile][day] of simulated viral load trajectories
		 * @return A 100x4 array: [profile][B₁, B₂, B₃, B₄], the first four raw moments of \(  \delta_t  \) 
		 */
	    private static double[][] computeBProfiles(double[][] viralLoadData) {
	        if (viralLoadData.length != 100 || viralLoadData[0].length != 100)
	            throw new IllegalArgumentException("Input must be a 100x100 array");

	        double[][] bProfiles = new double[100][4];

	        for (int m = 0; m < 100; m++) {
	            double B1 = 0, B2 = 0, B3 = 0, B4 = 0;

	            for (int t = 0; t < 100; t++) {
	                double v = viralLoadData[m][t];
	                if (v > 1.0) {
	                    double delta = v - 1.0;
	                    B1 += delta;
	                    B2 += delta * delta;
	                    B3 += delta * delta * delta;
	                    B4 += delta * delta * delta * delta;
	                }
	            }

	            bProfiles[m][0] = B1;
	            bProfiles[m][1] = B2;
	            bProfiles[m][2] = B3;
	            bProfiles[m][3] = B4;
	        }

	        return bProfiles;
	    }
	    
	    	// Cached average polynomial coefficients: avg_c1, ..., avg_c4
	        private double avg_c1 = 0.0;
	        private double avg_c2 = 0.0;
	        private double avg_c3 = 0.0;
	        private double avg_c4 = 0.0;

	        // Compute A(m)(c) ≈ B1*c - B2/2*c^2 + B3/6*c^3 - B4/24*c^4
//	        private static double computeA(int m, double c, double[][] bProfiles) {
//	            double B1 = bProfiles[m][0];
//	            double B2 = bProfiles[m][1];
//	            double B3 = bProfiles[m][2];
//	            double B4 = bProfiles[m][3];
//
//	            return B1 * c
//	                 - (B2 / 2.0) * Math.pow(c, 2)
//	                 + (B3 / 6.0) * Math.pow(c, 3)
//	                 - (B4 / 24.0) * Math.pow(c, 4);
//	        }

	        /**
	         * Precomputes the average polynomial coefficients that approximate the expected
	         * probability of transmission across all contact edges and viral load profiles.
	         *
	         * <p>The goal is to approximate:
	         * $$
	         * \bar{p}(c) = \frac{1}{E} \sum_{ij} \frac{1}{M} \sum_{m=1}^M \left(1 - \prod_{t=1}^T (1 - k_{ij} c \delta_t^{(m)})\right)
	         * $$
	         * using a quartic polynomial:
	         * $$
	         * \bar{p}(c) \approx a_1 c + a_2 c^2 + a_3 c^3 + a_4 c^4
	         * $$
	         *
	         * <p>Using the cumulant generating function of the Poisson binomial distribution of exposures,
	         * we expand:
	         * $$
	         * \mathbb{P}(\text{any exposure}) = 1 - \exp\left( \sum_{t} \log(1 - k_{ij} c \delta_t^{(m)}) \right)
	         * \approx 1 - \exp\left( -\sum_{n=1}^4 \frac{(k_{ij} c)^n}{n} B_n^{(m)} \right)
	         * $$
	         * Let \(  S = \sum_{n=1}^4 s_n c^n  \)  with \(  s_n = \frac{k_{ij}^n B_n^{(m)}}{n}  \) . Then:
	         * $$
	         * 1 - e^{-S} = s_1 c + \left(s_2 - \frac{s_1^2}{2}\right) c^2 + \left(s_3 - s_1 s_2 + \frac{s_1^3}{6}\right) c^3 + \cdots
	         * $$
	         *
	         * <p>However, the implementation uses an equivalent but algebraically reorganized expansion,
	         * where intermediate terms \(  d_n = k_{ij} \cdot \frac{(-1)^{n+1} B_n^{(m)}}{n!}  \) are used to construct
	         * the polynomial coefficients via combinatorial identities arising from the Taylor expansion
	         * of \(  1 - e^{-X}  \) .
	         *
	         * <p>This method:
	         * <ul>
	         *   <li>Iterates over all contact probabilities \(  k_{ij}  \) </li>
	         *   <li>For each \(  k_{ij}  \)  and profile \(  m  \) , computes local coefficients \(  c_1, c_2, c_3, c_4  \) </li>
	         *   <li>Averages them first over profiles (inner loop), then over edges (outer loop)</li>
	         * </ul>
	         *
	         * <p>The resulting cached coefficients \(  \texttt{avg\_c1}, \dots, \texttt{avg\_c4}  \)  enable
	         * \(  O(1)  \)  evaluation of the expected transmission probability in {@link #fastPTransmission(double)}.
	         *
	         * @param kValues Array of contact probabilities \(  k_{ij}  \)  for each edge in the social network
	         * @param bProfiles Output of {@link #computeBProfiles(double[][])}: \(  B_1, B_2, B_3, B_4  \)  per profile
	         */
	        public void precomputeAverageCoefficients(double[] kValues, double[][] bProfiles) {
	            
	            double total_c1 = 0.0;
	            double total_c2 = 0.0;
	            double total_c3 = 0.0;
	            double total_c4 = 0.0;

	            for (double k_ij : kValues) {
	                double local_c1 = 0.0;
	                double local_c2 = 0.0;
	                double local_c3 = 0.0;
	                double local_c4 = 0.0;

	                for (int m = 0; m < 100; m++) {
	                    double B1 = bProfiles[m][0];
	                    double B2 = bProfiles[m][1];
	                    double B3 = bProfiles[m][2];
	                    double B4 = bProfiles[m][3];

	                    // Compute d_n = k_ij * a_n
	                    double d1 = k_ij * B1;
	                    double d2 = k_ij * (-B2 / 2.0);
	                    double d3 = k_ij * (B3 / 6.0);
	                    double d4 = k_ij * (-B4 / 24.0);

	                    // Compute polynomial coefficients up to c^4
	                    double c1 = d1;
	                    double c2 = d2 - (d1 * d1) / 2.0;
	                    double c3 = d3 - d1 * d2 + (d1 * d1 * d1) / 6.0;
	                    double c4 = d4 - (d2 * d2) / 2.0 - d1 * d3 + (d1 * d1 * d2) / 2.0 - (d1 * d1 * d1 * d1) / 24.0;

	                    local_c1 += c1;
	                    local_c2 += c2;
	                    local_c3 += c3;
	                    local_c4 += c4;
	                }

	                // Average over 100 profiles
	                total_c1 += local_c1 / 100.0;
	                total_c2 += local_c2 / 100.0;
	                total_c3 += local_c3 / 100.0;
	                total_c4 += local_c4 / 100.0;
	            }

	            // Average over all pairs
	            avg_c1 = total_c1;
	            avg_c2 = total_c2;
	            avg_c3 = total_c3;
	            avg_c4 = total_c4;

	            
	        }

	        // Fast evaluation using cached coefficients
	        public double fastPTransmission(double c) {
	            
	            return avg_c1 * c
	                 + avg_c2 * c * c
	                 + avg_c3 * c * c * c
	                 + avg_c4 * c * c * c * c;
	        }
	    }
	    
	

}
