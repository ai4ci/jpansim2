package io.github.ai4ci.abm.policy;

import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.util.Binomial;

/**
 * Interface for defining policy activation triggers based on outbreak state metrics.
 * 
 * <p>Triggers provide a mechanism for policy models to evaluate when to activate
 * or deactivate interventions based on real-time epidemiological data. Each trigger
 * encapsulates a specific outbreak metric that can be monitored for policy decisions.
 * 
 * <h2>Usage Context</h2>
 * <p>Triggers are primarily used by policy state machines like {@link ReactiveLockdown}
 * to determine when to transition between policy states based on threshold comparisons.
 * 
 * <h2>Statistical Foundation</h2>
 * <p>All triggers return {@link Binomial} distributions, enabling statistical
 * confidence evaluations for policy decisions. This allows policies to make
 * decisions with specified confidence levels (e.g., 95% confidence) rather than
 * relying on point estimates that may be influenced by random fluctuations.
 * 
 * @see ReactiveLockdown
 * @see Binomial
 * @see OutbreakState
 */
public interface Trigger {

	/**
	 * Selects the binomial distribution representing the current outbreak metric
	 * that this trigger monitors.
	 * 
	 * <p>The returned binomial distribution can be used for statistical comparisons
	 * against policy thresholds with specified confidence levels.
	 * 
	 * @param state the current outbreak state containing epidemiological data
	 * @return a binomial distribution representing the monitored outbreak metric
	 */
	Binomial select(OutbreakState state);
	
	/**
	 * Predefined trigger implementations for common outbreak metrics used in policy decisions.
	 * 
	 * <p>These enum values provide ready-to-use triggers for the most commonly monitored
	 * epidemiological indicators in outbreak response policies.
	 */
	static enum Value implements Trigger {
		
		/**
		 * Trigger based on overall test positivity rate across all testing types.
		 * 
		 * <p>This trigger monitors the proportion of positive tests among all tests conducted,
		 * providing a general measure of outbreak prevalence in the tested population.
		 * 
		 * <p><b>Use Case:</b> General outbreak monitoring and broad policy decisions
		 */
		TEST_POSITIVITY {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getPresumedTestPositivity();
			}
		},
		
		/**
		 * Trigger based on screening test positivity rate from routine asymptomatic testing.
		 * 
		 * <p>This trigger specifically monitors positivity rates from screening programs
		 * targeting asymptomatic individuals, which may provide earlier detection of
		 * outbreak spread before symptomatic cases emerge.
		 * 
		 * <p><b>Use Case:</b> Early outbreak detection and proactive policy activation
		 */
		SCREENING_TEST_POSITIVITY {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getScreeningTestPositivity();
			}
		},
		
		/**
		 * Trigger based on the count of positive tests (case incidence).
		 * 
		 * <p>This trigger monitors the absolute number of positive test results,
		 * representing case incidence rather than prevalence proportion. Useful for
		 * policies that respond to absolute case numbers rather than relative rates.
		 * 
		 * <p><b>Use Case:</b> Policies triggered by absolute case counts
		 */
		TEST_COUNT {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getPresumedTestPositivePrevalence();
			}
		},
		
		/**
		 * Trigger based on hospitalisation burden or hospitalisation rates.
		 * 
		 * <p>This trigger monitors healthcare system impact through hospitalisation
		 * metrics. This provides a measure of disease severity and healthcare system
		 * strain rather than just infection spread.
		 * 
		 * <p><b>Use Case:</b> Policies focused on healthcare capacity management
		 * and severe disease response
		 */
		HOSPITAL_BURDEN {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getHospitalisationRate();
			}
		}
		
	}
	
}