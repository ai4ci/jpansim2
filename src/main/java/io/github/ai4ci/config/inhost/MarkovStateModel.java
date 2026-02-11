package io.github.ai4ci.config.inhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.execution.DemographicAdjustment;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.functions.Distribution;
import io.github.ai4ci.functions.SimpleDistribution;

/**
 * Configuration for an in‑host Markov state model.
 *
 * <p>
 * This interface describes the distributions and parameters used by the
 * simplistic in‑host Markov model implementation. It supplies distributions for
 * incubation, infectious and symptom durations, and for the immune waning
 * half‑life. Implementations are primarily consumed by the initialiser and
 * state classes that set up and manage an individual's in‑host state.
 *
 * <p>
 * Downstream uses include the initialisers and builders such as
 * {@link io.github.ai4ci.flow.builders.DefaultInHostMarkovStateInitialiser},
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} and
 * {@link io.github.ai4ci.flow.builders.DefaultInHostPhenomenologicalStateInitialiser}.
 * It is also referenced by example and test code such as
 * {@link io.github.ai4ci.example.Experiment}.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMarkovStateModel.class)
@JsonDeserialize(as = ImmutableMarkovStateModel.class)
public interface MarkovStateModel extends InHostConfiguration,
		DemographicAdjustment.Markov<Distribution, Double> {

	/**
	 * A sensible default parameterisation of the Markov model.
	 *
	 * <p>
	 * This instance provides a small set of log‑normal distributions used as a
	 * baseline in examples and tests. Downstream callers that rely on a simple
	 * default include {@link io.github.ai4ci.example.Experiment}.
	 *
	 * @see io.github.ai4ci.flow.builders.DefaultInHostMarkovStateInitialiser
	 */
	public static ImmutableMarkovStateModel DEFAULT = ImmutableMarkovStateModel
			.builder()
			.setImmuneWaningHalfLife(SimpleDistribution.logNorm(300D, 10D))
			.setIncubationPeriod(SimpleDistribution.logNorm(5D, 2D))
			.setInfectiousDuration(SimpleDistribution.logNorm(8D, 3D))
			.setSymptomDuration(SimpleDistribution.logNorm(5D, 3D)).build();

	/**
	 * Distribution for the half life of waning immunity.
	 *
	 * <p>
	 * This distribution describes the expected time scale for immune waning and
	 * is sampled when determining the decay of immunity after recovery or
	 * vaccination. It is consulted by initialisers and state updaters. The
	 * distribution is not age specific in this model but may be modified by
	 * {@link DemographicAdjustment.Markov} implementations that provide
	 * age‑specific adjustments to the symptom duration distribution in the form
	 * of an odds ratio.
	 *
	 * @return a {@link io.github.ai4ci.functions.Distribution} representing the
	 *         immune waning half life
	 */
	@Override
	Distribution getImmuneWaningHalfLife();

	/**
	 * Distribution for the incubation period (time from infection to onset).
	 *
	 * <p>
	 * This distribution is sampled by initialisers such as
	 * {@link io.github.ai4ci.flow.builders.DefaultInHostMarkovStateInitialiser}
	 * and by phenomenological initialisers when converting to internal state
	 * durations. It is not age specific in this model but may be modified by
	 * {@link DemographicAdjustment.Markov} implementations that provide
	 * age‑specific adjustments to the incubation period distribution in the form
	 * of a odds ratio.
	 *
	 * @return a {@link io.github.ai4ci.functions.Distribution} used to sample
	 *         incubation periods
	 */
	@Override
	Distribution getIncubationPeriod();

	/**
	 * Distribution for the infectious duration.
	 *
	 * <p>
	 * This value is not age specific in the current model. Callers include the
	 * default initialiser which samples an infectious duration for each infected
	 * individual. The distribution is not age specific in this model but may be
	 * modified by {@link DemographicAdjustment.Markov} implementations that
	 * provide age‑specific adjustments to the symptom duration distribution in
	 * the form of an odds ratio.
	 *
	 * @return a {@link io.github.ai4ci.functions.Distribution} for infectious
	 *         duration
	 */
	@Override
	Distribution getInfectiousDuration(); // not age specfic

	/**
	 * Severity cutoff used to decide whether an infected individual is classed
	 * as fatal.
	 *
	 * <p>
	 * The default implementation derives the cutoff from the
	 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration#getInfectionFatalityRate()}
	 * as 1 minus the fatality rate. This is used when mapping severity to death
	 * outcomes in the outbreak and state logic.
	 *
	 * @param outbreak      the current outbreak context
	 * @param configuration the execution configuration providing fatality rates
	 * @return the cutoff in the unit interval [0,1] used to classify fatality
	 */
	@Override
	default double getSeverityFatalityCutoff(
			Outbreak outbreak, ExecutionConfiguration configuration
	) {
		return 1 - configuration.getInfectionFatalityRate();
	}
	// NB not modifiable at present

	// The markov model has no internal representation of continuous severity
	// and the cutoffs are arbitrary

	/**
	 * Severity cutoff used to decide whether an infected individual is classed
	 * as hospitalised.
	 *
	 * <p>
	 * The default implementation derives the cutoff from the
	 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration#getInfectionHospitalisationRate()}
	 * as 1 minus the hospitalisation rate. Downstream uses include the same
	 * baseline and state classes that interpret severity measures.
	 *
	 * @param outbreak      the current outbreak context
	 * @param configuration the execution configuration providing hospitalisation
	 *                      rates
	 * @return the cutoff in the unit interval [0,1] used to classify
	 *         hospitalisation
	 */
	@Override
	default double getSeverityHospitalisationCutoff(
			Outbreak outbreak, ExecutionConfiguration configuration
	) {
		return 1 - configuration.getInfectionHospitalisationRate();
	}
	// NB not modifiable at present

	/**
	 * Severity cutoff used to decide whether an infected individual is classed
	 * as a symptomatic case.
	 *
	 * <p>
	 * The default implementation derives the cutoff from the
	 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration#getInfectionCaseRate()}
	 * as 1 minus the case rate. This method is used by classes such as
	 * {@link io.github.ai4ci.abm.OutbreakBaseline},
	 * {@link io.github.ai4ci.abm.PersonState} and
	 * {@link io.github.ai4ci.flow.builders.DefaultOutbreakBaseliner} to map a
	 * continuous severity measure to discrete outcomes.
	 *
	 * @param outbreak      the current outbreak context
	 * @param configuration the execution configuration providing attack rates
	 * @return the cutoff in the unit interval [0,1] used to classify symptoms
	 */
	@Override
	default double getSeveritySymptomsCutoff(
			Outbreak outbreak, ExecutionConfiguration configuration
	) {
		return 1 - configuration.getInfectionCaseRate();
	}
	// NB not modifiable at present

	/**
	 * Distribution for the duration of symptoms.
	 *
	 * <p>
	 * Used by initialisers to determine how long symptomatic states last. The
	 * distribution is not age specific in this model but may be modified by
	 * {@link DemographicAdjustment.Markov} implementations that provide
	 * age‑specific adjustments to the symptom duration distribution in the form
	 * of an odds ratio.
	 *
	 * @return a {@link io.github.ai4ci.functions.Distribution} for symptom
	 *         duration
	 */
	@Override
	Distribution getSymptomDuration(); // not age specfic

}