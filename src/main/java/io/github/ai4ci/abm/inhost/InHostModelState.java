package io.github.ai4ci.abm.inhost;

import java.io.Serializable;
import java.util.Optional;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.builders.DefaultPersonInitialiser;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.util.Sampler;

/**
 * Interface for in-host models that simulate viral dynamics, immune response, and disease progression
 * over time within an individual.
 *
 * <p>All implementations must provide:
 * <ul>
 *   <li>Normalized viral load,</li>
 *   <li>Disease severity,</li>
 *   <li>Immune activity,</li>
 *   <li>Infection status,</li>
 *   <li>A time counter,</li>
 *   <li>A method to update the state given external exposures (virion or immunization).</li>
 * </ul>
 *
 * <p>This abstraction enables plug-and-play use of different in-host modeling approaches:
 * <ul>
 *   <li><b>Phenomenological:</b> continuous curves with superposition of exposures,</li>
 *   <li><b>Stochastic difference equations:</b> discrete compartments with binomial/Poisson transitions,</li>
 *   <li><b>Markov state models:</b> discrete states with probabilistic progression.</li>
 * </ul>
 *
 * <p><u>Biological and Statistical Foundation</u></p>
 *
 * <p>The update and aggregation of infection risks across a population rely on two key probabilistic frameworks:
 *
 * <p><b>1. Set Coverage Problem</b>  
 * When modeling exposure in networks or venues, the probability that a target cell or individual is infected
 * after \( X \) random contacts is governed by coverage:
 * \[
 * \mathbb{P}(\text{covered}) = 1 - \left(1 - \frac{1}{N}\right)^X \approx 1 - e^{-X/N}
 * \]
 * This determines the expected fraction of the population or cellular targets exposed after repeated random sampling.
 * @see <a href="https://math.stackexchange.com/questions/32800/probability-distribution-of-coverage-of-a-set-after-x-independently-randomly">Set Coverage Problem</a>
 *
 * <p><b>2. Poisson Binomial Distribution</b>  
 * In heterogeneous populations, the number of individuals transitioning between states (e.g., exposed → infectious)
 * follows a Poisson binomial distribution, where each individual has a potentially different transition probability \( p_i \).
 * The mean and variance are:
 * \[
 * \mathbb{E}\left[\sum_i x_i\right] = \sum_i p_i, \quad \mathrm{Var}\left[\sum_i x_i\right] = \sum_i p_i(1 - p_i)
 * \]
 * This is critical for accurately simulating population-level outcomes without assuming homogeneity.
 * @see <a href="https://stats.stackexchange.com/questions/93852/sum-of-bernoulli-variables-with-different-success-probabilities">Poisson Binomial Distribution</a>
 *
 * <p>This interface ensures that regardless of the internal model, downstream components (e.g., transmission,
 * calibration, severity mapping) can interact uniformly with the in-host state.
 */
public interface InHostModelState<CFG extends InHostConfiguration> extends Serializable {

	// CFG getConfig();
	/**
	 * Returns the normalized viral load, typically scaled relative to a reference infectiousness threshold.
	 *
	 * <p>This value is used in transmission models to compute the probability of infectious contact:
	 * \[
	 * p_{\text{transmit}} \propto f(V_{\text{norm}})
	 * \]
	 * where \( V_{\text{norm}} = V / V_{\text{cutoff}} \). The exact functional form depends on the model.
	 *
	 * @return normalized viral load (unitless, ≥ 0)
	 */
	@Value.NonAttribute double getNormalisedViralLoad();
	
	/**
	 * Returns a normalized severity score, typically in [0,1], representing clinical outcome.
	 *
	 * <p>This may be derived from viral load, symptom state, or hospitalization status.
	 * It is used to map infection to observable events (e.g., hospitalization) with given probabilities.
	 * For example, if \( S \) is severity and \( r_h \) is the infection-hospitalization rate, then:
	 * \[
	 * \mathbb{P}(\text{hospitalized}) = \mathbb{P}(S \gt 1 - r_h)
	 * \]
	 * ensuring population-level rates are preserved.
	 *
	 * @return normalized severity (0 = no disease, 1 = fatal)
	 */
	@Value.NonAttribute double getNormalisedSeverity();
	
	/**
	 * Returns the level of active immune response, typically normalized to [0,1].
	 *
	 * <p>This modulates:
	 * <ul>
	 *   <li>Viral clearance rate,</li>
	 *   <li>Susceptibility to reinfection,</li>
	 *   <li>Disease severity.</li>
	 * </ul>
	 *
	 * <p>In some models, this is binary (0 or 1); in others, it is continuous and cumulative.
	 *
	 * @return immune activity level (0 = none, 1 = maximum)
	 */
	@Value.NonAttribute double getImmuneActivity();
	
	int getTime();
	
	/**
	 * Advances the in-host state by one time step, processing new exposures.
	 *
	 * <p>The update incorporates:
	 * <ul>
	 *   <li><b>virionExposure:</b> external infectious dose (relative to infectious threshold),</li>
	 *   <li><b>immunisationDose:</b> vaccine or booster exposure (may prime immunity),</li>
	 *   <li><b>sampler:</b> source of randomness for stochastic transitions.</li>
	 * </ul>
	 *
	 * <p>The implementation determines how these inputs affect viral load, immunity, and disease state.
	 * All models must ensure state consistency and biological plausibility (e.g., no negative counts).
	 *
	 * @param sampler the random number generator for stochastic transitions
	 * @param virionExposure the magnitude of external viral exposure (≥ 0)
	 * @param immunisationDose the magnitude of immunizing exposure (≥ 0)
	 * @return a new instance of the model advanced by one time step
	 */
	InHostModelState<CFG> update(Sampler sampler, double virionExposure, double immunisationDose); // , double viralActivityModifier, double immuneModifier);
	
	/**
	 * Convenience method to update the in-host state using exposure data from a {@link Person}.
	 *
	 * <p>Extracts:
	 * <ul>
	 *   <li>Total exposure from contact network,</li>
	 *   <li>Immunisation dose from vaccination history.</li>
	 * </ul>
	 *
	 * <p>Delegates to {@link #update(Sampler, double, double)}.
	 *
	 * @param person the person whose exposure and immunization history is used
	 * @param sampler the random sampler
	 * @return updated in-host state
	 */
	default InHostModelState<CFG> update(Person person, Sampler sampler) {
		
		return update(
			sampler,
			person.getCurrentState().getTotalExposure(),
			person.getCurrentState().getImmunisationDose()
		);
		
//				person.getOutbreak().getCurrentState().getViralActivityModifier(),
//				person.getCurrentState().getImmuneModifier()
//		);
		
	}
	
	/**
	 * Factory method to create a test instance of the in-host model, useful for calibration and debugging.
	 *
	 * <p>Uses {@link DefaultPersonInitialiser} to initialize the model with default parameters
	 * and a given random sampler. The time step is initialized to 0.
	 *
	 * @param config the in-host configuration specific to the model
	 * @param execConfig the global execution configuration
	 * @param rng the random sampler
	 * @return a newly initialized in-host model state
	 * @param <CFG> the type of in-host configuration
	 */
	public static <CFG extends InHostConfiguration> InHostModelState<CFG> test(CFG config, ExecutionConfiguration execConfig, Sampler rng) {
		return (new DefaultPersonInitialiser() {}).initialiseInHostModel(config, execConfig, Optional.empty(), rng, 0);
	}
	
}
