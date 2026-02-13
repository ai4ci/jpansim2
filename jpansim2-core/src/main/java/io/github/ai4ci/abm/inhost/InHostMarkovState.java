package io.github.ai4ci.abm.inhost;

import java.io.Serializable;

import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

import io.github.ai4ci.abm.inhost.ImmutableInHostMarkovState.Builder;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.util.Sampler;

/**
 * A discrete-time Markov chain model of in-host disease and symptom
 * progression.
 *
 * <p>
 * This model tracks two independent state machines:
 * <ul>
 * <li><b>Disease state</b>: SUSCEPTIBLE → EXPOSED → INFECTIOUS → IMMUNE,</li>
 * <li><b>Symptom state</b>: ASYMPTOMATIC → SYMPTOMATIC → HOSPITALISED →
 * DEAD.</li>
 * </ul>
 *
 * <p>
 * Transitions are governed by daily probabilities defined in
 * {@link InHostMarkovStateMachine}. The model assumes binary immunity: once
 * immune, individuals are fully protected unless waning occurs.
 *
 * <p>
 * The viral load, severity, and immune activity are derived from the current
 * state, enabling integration with transmission and calibration modules. This
 * approach sacrifices biological detail for computational efficiency and direct
 * mapping to observable epidemiological rates (e.g., IFR, HFR).
 *
 * @see InHostMarkovStateMachine
 * @see <a href=
 *      "https://stats.stackexchange.com/questions/93852/sum-of-bernoulli-variables-with-different-success-probabilities">Poisson
 *      Binomial Distribution</a>
 */
@Value.Immutable
public interface InHostMarkovState extends InHostModelState<MarkovStateModel> {

	/**
	 * The disease progression states in the Markov model.
	 *
	 * <p>
	 * States evolve as: \[ \text{SUSCEPTIBLE} \xrightarrow{p_{\text{inf}}}
	 * \text{EXPOSED} \xrightarrow{p_{\text{inf→inf}}} \text{INFECTIOUS}
	 * \xrightarrow{p_{\text{rec}}} \text{IMMUNE} \xrightarrow{p_{\text{waning}}}
	 * \text{SUSCEPTIBLE} \] where transitions are governed by daily
	 * probabilities. Immunity is assumed to be complete while active.
	 */
	public static enum DiseaseState {
		/**
		 * The individual is susceptible to infection and has no immunity. They
		 * can transition to EXPOSED if exposed to infectious virions, or to
		 * IMMUNE if immunized (e.g., vaccinated). This state represents the
		 * baseline risk of infection.
		 */
		SUSCEPTIBLE,
		/**
		 * The individual has been exposed to infectious virions but is not yet
		 * infectious. They can transition to INFECTIOUS with probability \(
		 * p_{\text{inf→inf}} \) or, in some models, back to SUSCEPTIBLE if the
		 * exposure does not lead to infection. This state captures the incubation
		 * period before viral shedding begins.
		 */
		EXPOSED,
		/**
		 * The individual is actively infectious and can transmit the virus to
		 * others. They can transition to IMMUNE upon recovery or, in some models,
		 * back to SUSCEPTIBLE if immunity wanes. This state represents the period
		 * of active infection and viral shedding.
		 */
		INFECTIOUS,
		/**
		 * The individual has recovered from infection and is temporarily immune.
		 * They can transition back to SUSCEPTIBLE if immunity wanes. This state
		 * represents the post-infection period of protection against reinfection.
		 */
		IMMUNE

	}

	// these next 3 are here because there is no easy way to access them within
	// the top level experiment configuration outside of the config stage so
	// we have to copy them.

	/**
	 * Defines the transition probabilities for the Markov chain model of disease
	 * and symptom progression.
	 *
	 * <p>
	 * This component encapsulates all time-homogeneous transition rates between
	 * states. It enables calibration from epidemiological data (e.g., duration
	 * of infectiousness, case fatality rate).
	 *
	 * <p>
	 * The probability of becoming symptomatic is computed as: \[ p_s = 1 - (1 -
	 * r_c)^{1/D} \] where \( r_c \) is the infection-case rate and \( D \) is
	 * the average duration of infection, ensuring that over the full infectious
	 * period, the cumulative probability of symptoms is \( r_c \).
	 *
	 * @see #getPAsymptomaticSymptomatic()
	 */
	@Value.Immutable
	public static interface InHostMarkovStateMachine extends Serializable {

		/**
		 * Returns the daily probability of transitioning from asymptomatic to
		 * symptomatic, calibrated to achieve the target infection-case rate over
		 * the infectious period.
		 *
		 * <p>
		 * If \( r_c \) is the desired case rate and \( D \) is the average
		 * duration of infectiousness, then: \[ p_s = 1 - (1 - r_c)^{1/D} \] This
		 * ensures that the probability of ever becoming symptomatic during
		 * infection is \( r_c \), under the assumption of independent daily risk.
		 *
		 * <p>
		 * This formulation assumes each day presents an independent chance of
		 * symptom onset, and the overall process resembles a <i>geometric
		 * trial</i> or a sum of Bernoulli variables with equal probability — a
		 * special case of the Poisson binomial distribution.
		 *
		 * @return daily probability of symptom onset given infectiousness
		 */
		double getPAsymptomaticSymptomatic();

		/**
		 * Returns the daily probability of transitioning from EXPOSED to
		 * INFECTIOUS.
		 *
		 * @return the daily probability of an exposed individual becoming
		 *         infectious
		 */
		double getPExposedInfectious();
		// double getPExposedSusceptible();

		/**
		 * Returns the daily probability of transitioning from HOSPITALISED to
		 * ASYMPTOMATIC (recovery), calibrated to achieve the target hospital
		 * recovery rate over the course of hospitalisation.
		 *
		 * @return the daily probability of recovery from hospitalisation,
		 *         calibrated to match target hospital recovery rates
		 */
		double getPHospitalisedAsymptomatic();

		/**
		 * Returns the daily probabilities of transitioning from HOSPITALISED to
		 * DEAD, and from HOSPITALISED to ASYMPTOMATIC (recovery), calibrated to
		 * achieve the target hospital fatality rate (HFR) and recovery rate over
		 * the course of hospitalisation.
		 *
		 * @return the daily probabilities of death and recovery from
		 *         hospitalisation, calibrated to match target HFR and recovery
		 *         rates
		 */
		double getPHospitalisedDead();

		/**
		 * Returns the daily probability of transitioning from IMMUNE to
		 * SUSCEPTIBLE due to waning immunity.
		 *
		 * @return the daily probability of waning immunity (IMMUNE → SUSCEPTIBLE)
		 */
		double getPImmuneSusceptible();

		/**
		 * Returns the daily probability of transitioning from INFECTIOUS to
		 * IMMUNE, and from IMMUNE to SUSCEPTIBLE.
		 *
		 * @return the daily probability of recovery (INFECTIOUS → IMMUNE) and
		 *         waning immunity (IMMUNE → SUSCEPTIBLE)
		 */
		double getPInfectiousImmune();

		/**
		 * Returns the daily probabilities of transitions from symptomatic and
		 * hospitalised states to less severe or more severe states, calibrated to
		 * match population-level outcomes (e.g., IFR, HFR).
		 *
		 * @return the daily probabilities of symptom progression and regression,
		 *         including recovery and death, conditioned on current symptom
		 *         state
		 */
		double getPSymptomaticAsymptomatic();

		/**
		 * Returns the daily probabilities of transitioning from SYMPTOMATIC to
		 * DEAD, and from HOSPITALISED to DEAD, calibrated to achieve the target
		 * infection-fatality rate (IFR) and hospital fatality rate (HFR) over the
		 * course of the disease.
		 *
		 * @return the daily probabilities of death from symptomatic and
		 *         hospitalised states, calibrated to match target IFR and HFR
		 */
		double getPSymptomaticDead();

		/**
		 * Returns the daily probability of transitioning from SYMPTOMATIC to
		 * HOSPITALISED, and from SYMPTOMATIC to DEAD.
		 *
		 * @return the daily probabilities of worsening symptoms leading to
		 *         hospitalisation or death
		 */
		double getPSymptomaticHospitalised();

		/**
		 * Updates the disease state using daily Bernoulli trials.
		 *
		 * <p>
		 * Transitions:
		 * <ul>
		 * <li>EXPOSED → INFECTIOUS with probability \( p_{\text{inf}} \),</li>
		 * <li>INFECTIOUS → IMMUNE with probability \( p_{\text{rec}} \),</li>
		 * <li>IMMUNE → SUSCEPTIBLE with probability \( p_{\text{waning}} \).</li>
		 * </ul>
		 *
		 * <p>
		 * The use of independent Bernoulli draws aligns with the discrete-time
		 * survival framework. When aggregating over a population, the number of
		 * individuals transitioning follows a <i>Poisson binomial
		 * distribution</i> with: \[ \mathbb{E}[N_{\text{transitions}}] = \sum_i
		 * p_i, \quad \mathrm{Var}(N_{\text{transitions}}) = \sum_i p_i(1 - p_i)
		 * \] as referenced in the knowledge base.
		 *
		 * @param current the current disease state
		 * @param rng     the random sampler
		 * @return the next disease state
		 */
		default DiseaseState updateDiseaseState(
				DiseaseState current, Sampler rng
		) {
			switch (current) {
			case EXPOSED:
				return rng
						.bern(this.getPExposedInfectious(), DiseaseState.INFECTIOUS
						// This is disabled because partial transmission is handles
						// before an
						// individual gets exposed. It is a fundamental part of the
						// contact process
						// and in this model immunity is binary and a disease state so
						// we don't get a path from exposed to susceptible because of
						// that.
						// return rng.multinom(
						// Pair.of(this.getPExposedInfectious(),
						// DiseaseState.INFECTIOUS) ,
						// Pair.of(this.getPExposedSusceptible(),
						// DiseaseState.SUSCEPTIBLE)
						).orElse(DiseaseState.EXPOSED);

			case INFECTIOUS:
				return rng.bern(this.getPInfectiousImmune(), DiseaseState.IMMUNE)
						.orElse(DiseaseState.INFECTIOUS);
			case IMMUNE:
				return rng
						.bern(this.getPImmuneSusceptible(), DiseaseState.SUSCEPTIBLE)
						.orElse(DiseaseState.IMMUNE);
			case SUSCEPTIBLE:
				return DiseaseState.SUSCEPTIBLE;
			default:
				throw new RuntimeException();
			}
		}

		/**
		 * Updates the symptom state using multinomial transitions conditioned on
		 * disease state.
		 *
		 * <p>
		 * For example, from SYMPTOMATIC, the individual may:
		 * <ul>
		 * <li>Recover → ASYMPTOMATIC,</li>
		 * <li>Worsen → HOSPITALISED,</li>
		 * <li>Deteriorate → DEAD.</li>
		 * </ul>
		 *
		 * <p>
		 * The multinomial draw ensures only one transition occurs per day. The
		 * probabilities are calibrated to reproduce population-level outcomes
		 * (e.g., IFR) over time.
		 *
		 * @param current the current symptom state
		 * @param dx      the current disease state (used to gate symptom onset)
		 * @param rng     the random sampler
		 * @return the next symptom state
		 */
		default SymptomState updateSymptomState(
				SymptomState current, DiseaseState dx, Sampler rng
		) {
			switch (current) {
			case ASYMPTOMATIC:
				if (dx.equals(DiseaseState.INFECTIOUS)) return rng.bern(
						this.getPAsymptomaticSymptomatic(), SymptomState.SYMPTOMATIC
				).orElse(SymptomState.ASYMPTOMATIC);
				return SymptomState.ASYMPTOMATIC;
			case SYMPTOMATIC:
				return rng.multinom(
						Pair.of(
								this.getPSymptomaticAsymptomatic(),
								SymptomState.ASYMPTOMATIC
						),
						Pair.of(
								this.getPSymptomaticHospitalised(),
								SymptomState.HOSPITALISED
						), Pair.of(this.getPSymptomaticDead(), SymptomState.DEAD)
				).orElse(SymptomState.SYMPTOMATIC);
			case HOSPITALISED:
				return rng.multinom(
						Pair.of(
								this.getPHospitalisedAsymptomatic(),
								SymptomState.ASYMPTOMATIC
						), Pair.of(this.getPHospitalisedDead(), SymptomState.DEAD)
				).orElse(SymptomState.HOSPITALISED);
			case DEAD:
				return SymptomState.DEAD;
			default:
				throw new RuntimeException();
			}
		}
	}

	/**
	 * The clinical symptom progression states.
	 *
	 * <p>
	 * These represent observable health outcomes: \[ \text{ASYMPTOMATIC}
	 * \xrightarrow{p_s} \text{SYMPTOMATIC} \xrightarrow{p_h,p_d}
	 * \text{HOSPITALISED} \xrightarrow{p_{hd}} \text{DEAD} \] with possible
	 * regression to less severe states (e.g., recovery). The symptom state is
	 * conditional on disease state and evolves independently.
	 */
	public static enum SymptomState {
		/**
		 * The individual is infected but shows no symptoms. They can transition
		 * to SYMPTOMATIC with probability \( p_s \) if they are in the INFECTIOUS
		 * disease state, or remain ASYMPTOMATIC if they do not develop symptoms.
		 * This state captures subclinical infections that may still contribute to
		 * transmission.
		 */
		ASYMPTOMATIC,
		/**
		 * The individual is infected and shows symptoms. They can transition to
		 * HOSPITALISED if symptoms worsen, or to ASYMPTOMATIC if they recover.
		 * This state represents the period of symptomatic illness.
		 */
		SYMPTOMATIC,
		/**
		 * The individual is infected and has severe symptoms requiring
		 * hospitalisation. They can transition to DEAD if they deteriorate, or to
		 * ASYMPTOMATIC if they recover. This state captures severe clinical
		 * outcomes.
		 */
		HOSPITALISED,
		/**
		 * The individual has died from the infection. This is an absorbing state
		 * with no transitions out. It represents the most severe outcome of the
		 * disease.
		 */
		DEAD

	}

	/**
	 * Getters for the current disease and symptom states, and the Markov state
	 * machine.
	 *
	 * <p>
	 * These provide access to the individual's current position in the disease
	 * and symptom progression, as well as the transition probabilities defined
	 * in the Markov model. The disease and symptom states evolve independently
	 * according to their respective transition rules, but are both influenced by
	 * the underlying MarkovStateMachine which governs the probabilities of
	 * moving between states.
	 *
	 * @return the current disease state, symptom state, and Markov state machine
	 */
	DiseaseState getDiseaseState();

	/**
	 * Returns immune activity as a binary indicator.
	 *
	 * <p>
	 * Defined as: \[ I(t) = \begin{cases} 1.0 &amp; \text{if } \text{disease
	 * state} = \text{IMMUNE} \\ 0.0 &amp; \text{otherwise} \end{cases} \] This
	 * reflects full protection during the immune state, with no partial
	 * immunity.
	 *
	 * @return 1.0 if immune, 0.0 otherwise
	 */
	@Override
	default double getImmuneActivity() {
		return this.getDiseaseState().equals(DiseaseState.IMMUNE) ? 1D : 0D;
	}

	/**
	 * Returns the infection-case rate (ICR) for this individual, which is the
	 * probability of developing symptoms given infection.
	 *
	 * @return the infection-case rate (ICR) for this individual
	 */
	double getInfectionCaseRate();

	/**
	 * Returns the infection-fatality rate (IFR) for this individual, which is
	 * the probability of death given infection.
	 *
	 * @return the infection-fatality rate (IFR) for this individual
	 */
	double getInfectionFatalityRate();

	/**
	 * Returns the infection-hospitalization rate (IHR) for this individual,
	 * which is the probability of hospitalization given infection.
	 *
	 * @return the infection-hospitalization rate (IHR) for this individual
	 */
	double getInfectionHospitalisationRate();

	/**
	 * Returns the Markov state machine that defines the transition probabilities
	 * for both disease and symptom progression. This machine encapsulates the
	 * time-homogeneous transition rates between states, allowing for calibration
	 * from epidemiological data. The transition probabilities govern how
	 * individuals move through the disease states (e.g., from EXPOSED to
	 * INFECTIOUS) and symptom states (e.g., from ASYMPTOMATIC to SYMPTOMATIC) on
	 * a daily basis. The machine's methods are used in the update logic to
	 * determine the next state based on the current state and random sampling.
	 *
	 * @return the Markov state machine containing transition probabilities for
	 *         disease and symptom progression
	 * @see InHostMarkovStateMachine
	 *
	 */
	@Value.Redacted
	InHostMarkovStateMachine getMachine();

	/**
	 * Samples a continuous severity score from a uniform distribution based on
	 * symptom state.
	 *
	 * <p>
	 * The score is calibrated so that:
	 * <ul>
	 * <li>Case rate: \( \mathbb{P}(S \gt 1 - r_c) = r_c \),</li>
	 * <li>Hospitalization rate: \( \mathbb{P}(S \gt 1 - r_h) = r_h \),</li>
	 * <li>Fatality rate: \( \mathbb{P}(S \gt 1 - r_f) = r_f \),</li>
	 * </ul>
	 * where \( r_c, r_h, r_f \) are the infection-case,
	 * infection-hospitalization, and infection-fatality ratios.
	 *
	 * <p>
	 * For example: \[ S \sim \text{Uniform}(1 - r_h, 1 - r_f) \quad \text{if }
	 * \text{HOSPITALISED} \] This ensures the population-level event rates match
	 * input parameters.
	 *
	 * @return a sampled severity value in [0,1]
	 */
	@Override @Value.Derived
	default double getNormalisedSeverity() {
		Sampler rng = Sampler.getSampler();
		switch (this.getSymptomState()) {
		case ASYMPTOMATIC:
			if (!this.getDiseaseState().equals(DiseaseState.INFECTIOUS)) return 0;
			return rng.uniform(0, 1 - this.getInfectionCaseRate());
		case SYMPTOMATIC:
			return rng.uniform(
					1 - this.getInfectionCaseRate(),
					1 - this.getInfectionHospitalisationRate()
			);
		case HOSPITALISED:
			return rng.uniform(
					1 - this.getInfectionHospitalisationRate(),
					1 - this.getInfectionFatalityRate()
			);
		case DEAD:
			return rng.uniform(1 - this.getInfectionFatalityRate(), 1);
		default:
			throw new RuntimeException();
		}
	}

	/**
	 * Returns a fixed normalized viral load based on disease state.
	 *
	 * <p>
	 * Defined as: \[ V_{\text{norm}} = \begin{cases} 1.5 &amp; \text{if }
	 * \text{state} = \text{INFECTIOUS} \\ 0.5 &amp; \text{if } \text{state} =
	 * \text{EXPOSED} \\ 0.0 &amp; \text{otherwise} \end{cases} \] This coarse
	 * proxy enables transmission modeling without detailed viral kinetics.
	 *
	 * @return a state-dependent normalized viral load
	 */
	@Override
	default double getNormalisedViralLoad() {
		if (this.getDiseaseState().equals(DiseaseState.INFECTIOUS)) return 1.5D;
		if (this.getDiseaseState().equals(DiseaseState.EXPOSED)) return 0.5D;
		return 0D;
	}

	/**
	 * The symptom state is conditional on the disease state, but evolves
	 * independently according to its own transition probabilities defined in the
	 * MarkovStateMachine. For example, an individual may be in the INFECTIOUS
	 * disease state but remain ASYMPTOMATIC in terms of symptoms, and then
	 * transition to SYMPTOMATIC based on the probability defined for that
	 * transition. The symptom progression is thus a separate layer of the model
	 * that captures clinical outcomes without directly affecting the underlying
	 * disease state transitions.
	 *
	 * @return the current symptom state of the individual
	 */
	SymptomState getSymptomState();

	// MarkovStateModel getConfig();
	/**
	 * Returns the current time step in the in-host progression.
	 *
	 * @return the current time step (e.g., days since infection) in the in-host
	 *         progression
	 */
	@Override
	int getTime();

	/**
	 * Advances the in-host state by one time step using Markov transitions.
	 *
	 * <p>
	 * The update logic:
	 * <ol>
	 * <li>If currently SUSCEPTIBLE and exposed to virions: → EXPOSED,</li>
	 * <li>If currently SUSCEPTIBLE and immunized: → IMMUNE,</li>
	 * <li>Otherwise: apply Markov transitions from current disease and symptom
	 * states.</li>
	 * </ol>
	 *
	 * <p>
	 * Disease progression uses Bernoulli and multinomial sampling: \[
	 * \mathbb{P}(X_{t+1} = x') = \sum_{x} \mathbb{P}(X_t = x) \cdot p(x \to x')
	 * \] where transition probabilities are constant per day.
	 *
	 * <p>
	 * The use of {@link Sampler#bern(Double, Object)} and
	 * {@link Sampler#multinom(Pair...)} ensures independent stochastic
	 * realizations. The sum of such independent transitions aligns with the
	 * framework of the <i>Poisson binomial distribution</i> when aggregating
	 * over populations, where each individual has a potentially different path
	 * through the state space.
	 *
	 * @param sampler          the random sampler for stochastic transitions
	 * @param virionExposure   non-zero if the individual is exposed to
	 *                         infectious virions
	 * @param immunisationDose non-zero if the individual receives an immunizing
	 *                         dose (e.g., vaccine)
	 * @return a new state advanced by one time unit
	 */
	@Override
	default InHostMarkovState update(
			Sampler sampler, double virionExposure, double immunisationDose
	) {

		DiseaseState next;
		if (this.getDiseaseState().equals(DiseaseState.SUSCEPTIBLE)) {
			if (virionExposure > 0) {
//				if (this.getMachine().getPExposedInfectious() == 1.0) {
//					//Skip E if E->I duration is zero => pTransition = 1.
//					next = DiseaseState.INFECTIOUS;
//				} else {
				next = DiseaseState.EXPOSED;
//				}
			} else if (immunisationDose > 0) {
				next = DiseaseState.IMMUNE;
			} else {
				next = this.getMachine()
						.updateDiseaseState(this.getDiseaseState(), sampler);
			}
		} else {
			next = this.getMachine()
					.updateDiseaseState(this.getDiseaseState(), sampler);
		}

		Builder builder = ImmutableInHostMarkovState.builder().from(this);
		return builder.setDiseaseState(next)
				.setSymptomState(
						this.getMachine().updateSymptomState(
								this.getSymptomState(), this.getDiseaseState(), sampler
						)
				).setTime(this.getTime() + 1).build();

	}

}
