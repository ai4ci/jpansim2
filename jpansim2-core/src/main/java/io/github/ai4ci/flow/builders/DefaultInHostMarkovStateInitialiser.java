package io.github.ai4ci.flow.builders;

import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.ImmutableInHostMarkovState;
import io.github.ai4ci.abm.inhost.ImmutableInHostMarkovStateMachine;
import io.github.ai4ci.abm.inhost.InHostMarkovState;
import io.github.ai4ci.abm.inhost.InHostMarkovState.DiseaseState;
import io.github.ai4ci.abm.inhost.InHostMarkovState.SymptomState;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

/**
 * Default initialiser for the Markov in‑host model.
 *
 * <p>
 * This interface supplies a focussed default implementation that converts a
 * {@link MarkovStateModel} configuration and execution parameters into an
 * {@link InHostMarkovState} instance. The implementation samples durations and
 * produces per‑day transition probabilities used by the Markov in‑host state
 * machine.
 *
 * <p>
 * Role in composition: the method defined here is intended to be used by
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} and is
 * composed into {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}. In
 * the default composition the model builder delegates to this interface's
 * default method via
 * {@code DefaultInHostMarkovStateInitialiser.super.initialiseInHostModel(...)}.
 * This separation keeps in‑host specific logic isolated and easily replaceable.
 *
 * <p>
 * Demographic adjustment: the implementation consults the optional
 * {@link PersonDemographic} and applies demographic adjustments using
 * {@link io.github.ai4ci.util.ReflectionUtils#modify} before sampling from the
 * configuration. This allows age or group specific variation in incubation and
 * symptom timings via the configured
 * {@link io.github.ai4ci.config.execution.DemographicAdjustment.Markov}.
 *
 * <p>
 * Extension guidance: implementers that need different Markov initial behaviour
 * should replace this interface or subclass the model builder and override the
 * delegation point. Keep the initialiser responsible only for creating the
 * initial immutable in‑host state; avoid performing heavy IO here unless
 * managed by the calling builder.
 *
 * @author Rob Challen
 */
public interface DefaultInHostMarkovStateInitialiser {

	/**
	 * Create an {@link InHostMarkovState} from a Markov configuration.
	 *
	 * <p>
	 * The default implementation applies demographic adjustments (if a
	 * {@code PersonDemographic} is present), samples distributional durations
	 * and computes mean rates and per‑day probabilities using the
	 * {@link Conversions} helpers. The returned state initialises the internal
	 * state machine transition probabilities.
	 *
	 * @param configuration the Markov in‑host configuration (possibly adjusted)
	 * @param execConfig    the execution configuration providing population
	 *                      rates
	 * @param person        optional person demographic used for demographic
	 *                      adjustment
	 * @param rng           sampler used for stochastic sampling
	 * @param time          current simulation time used for time stamped state
	 * @return an initial {@link InHostMarkovState}
	 */
	default InHostMarkovState initialiseInHostModel(
			MarkovStateModel configuration, ExecutionConfiguration execConfig,
			Optional<PersonDemographic> person, Sampler rng, int time
	) {

		if (person.isPresent()) {
			configuration = ReflectionUtils.modify(
					configuration, execConfig.getDemographicAdjustment(),
					person.get()
			);
		}

		double infectiousDuration = configuration.getInfectiousDuration()
				.sample(rng);

		// developing symptoms can only happen whilst infectious.
		// however infection duration i s a 95% quantile
		// and on average people are only infectious for less time than this.
		double meanInfectiousDuration = 1
				/ Conversions.rateFromQuantile(infectiousDuration, 0.95);

		double symptomDuration = configuration.getSymptomDuration().sample(rng);

		// likewise the mean symptomDuration is shorter than this:

		double meanSymptomDuration = 1
				/ Conversions.rateFromQuantile(symptomDuration, 0.95);

		// A per day probability that symptoms have finished assuming duration is
		// based on a 95% probability of resolution.
		double dailyProbabilityResolutionSymptoms = Conversions
				.probabilityFromQuantile(symptomDuration, 0.95);
		double dailyProbabilityUnresolvedSymptoms = 1
				- dailyProbabilityResolutionSymptoms;

		double dailyProbabilityFatalityGivenCase = Conversions
				.probabilityFromQuantile(
						meanSymptomDuration, execConfig.getCaseFatalityRate()
				);

		double dailyProbabilityHospitalisationGivenCase = Conversions
				.probabilityFromQuantile(
						meanSymptomDuration, execConfig.getCaseHospitalisationRate()
				);

		return ImmutableInHostMarkovState.builder().setTime(time)
				// .setConfig(configuration)
				.setInfectionCaseRate(execConfig.getInfectionCaseRate())
				.setInfectionHospitalisationRate(
						execConfig.getInfectionHospitalisationRate()
				).setInfectionFatalityRate(execConfig.getInfectionFatalityRate())
				.setDiseaseState(DiseaseState.SUSCEPTIBLE)
				.setSymptomState(SymptomState.ASYMPTOMATIC)
				.setMachine(
						ImmutableInHostMarkovStateMachine.builder()
								.setPExposedInfectious(
										Conversions.probabilityFromPeriod(
												configuration.getIncubationPeriod()
														.sample(rng)
										)
								)
								.setPInfectiousImmune(
										Conversions.probabilityFromQuantile(
												infectiousDuration, 0.95
										)
								)
								.setPImmuneSusceptible(
										Conversions.probabilityFromHalfLife(
												configuration.getImmuneWaningHalfLife()
														.sample(rng)
										)
								)

								.setPAsymptomaticSymptomatic(
										// The total proportion of symptomatic cases is
										// the result of the per day probability
										// aggregated over the infectious duration.
										Conversions.probabilityFromQuantile(
												meanInfectiousDuration,
												execConfig.getInfectionCaseRate()
										)
								)
								.setPSymptomaticAsymptomatic(
										dailyProbabilityResolutionSymptoms
								)
								.setPSymptomaticDead(
										dailyProbabilityUnresolvedSymptoms
												* dailyProbabilityFatalityGivenCase
								)
								.setPSymptomaticHospitalised(
										(dailyProbabilityUnresolvedSymptoms
												+ dailyProbabilityUnresolvedSymptoms
														* dailyProbabilityFatalityGivenCase)
												* dailyProbabilityHospitalisationGivenCase
								)

								.setPHospitalisedAsymptomatic(
										dailyProbabilityResolutionSymptoms
								)
								// TODO: review logic for fatality rate in Markov state
								// in host model
								.setPHospitalisedDead(
										dailyProbabilityUnresolvedSymptoms
												* dailyProbabilityFatalityGivenCase
								)

								.build()
				).build();

	}

}