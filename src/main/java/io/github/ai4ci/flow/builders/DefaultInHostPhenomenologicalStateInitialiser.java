package io.github.ai4ci.flow.builders;

import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.ImmutableInHostPhenomenologicalState;
import io.github.ai4ci.abm.inhost.InHostPhenomenologicalState;
import io.github.ai4ci.abm.inhost.InHostPhenomenologicalState.BiPhasicLogistic;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

/**
 * Default initialiser for the phenomenological in‑host model.
 *
 * <p>
 * This interface provides the default logic to construct an
 * {@link InHostPhenomenologicalState} from a {@link PhenomenologicalModel}
 * configuration and execution parameters. It samples the incubation and
 * viral/immune peak timings and calibrates simple biphasic logistic models used
 * to represent viral load and immune response.
 *
 * <p>
 * Role in composition: this initialiser is composed into
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} and
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}. The
 * {@code DefaultModelBuilder} delegates to this interface's default method to
 * create a person's phenomenological in‑host state. Replacing the in‑host
 * behaviour can be done by substituting this interface or by overriding the
 * delegation point in a subclass of the model builder.
 *
 * <p>
 * Demographic adjustment: where a {@link PersonDemographic} is supplied the
 * configuration is adjusted with
 * {@link io.github.ai4ci.util.ReflectionUtils#modify} prior to sampling so that
 * age or group specific scalings provided by
 * {@link io.github.ai4ci.config.execution.DemographicAdjustment.Phenomenological}
 * are applied.
 *
 * <p>
 * Extension guidance: keep this initialiser focused on creating the immutable
 * in‑host state and avoid heavy IO; any expensive precomputation should be
 * performed at outbreak baselining.
 *
 * @author Rob Challen
 */
public interface DefaultInHostPhenomenologicalStateInitialiser {

	/**
	 * Create an {@link InHostPhenomenologicalState} from a phenomenological
	 * configuration.
	 *
	 * <p>
	 * The default implementation samples incubation, time to peak and recovery
	 * delays, and calibrates the viral load and immune response models using
	 * {@link BiPhasicLogistic} helper methods. Demographic adjustments are
	 * applied before sampling.
	 *
	 * @param configuration the phenomenological in‑host configuration
	 * @param execConfig    execution configuration providing contextual rates
	 * @param person        optional demographic used to apply demographic
	 *                      adjustments
	 * @param rng           sampler used to draw from distributions
	 * @param time          current simulation time used to timestamp the state
	 * @return an initial {@link InHostPhenomenologicalState}
	 */
	default InHostPhenomenologicalState initialiseInHostModel(
			PhenomenologicalModel configuration, ExecutionConfiguration execConfig,
			Optional<PersonDemographic> person, Sampler rng, int time
	) {

		if (person.isPresent()) {
			configuration = ReflectionUtils.modify(
					configuration, execConfig.getDemographicAdjustment(),
					person.get()
			);
		}
		double incubation = configuration.getIncubationPeriod().sample(rng);
		return ImmutableInHostPhenomenologicalState.builder().setTime(time)
				.setInfectiousnessCutoff(configuration.getInfectiousnessCutoff())
				.setViralLoadModel(
						BiPhasicLogistic.calibrateViralLoad(
								incubation, // onsetTime
								configuration.getIncubationToPeakViralLoadDelay()
										.sample(rng), // peakTime,
								configuration.getPeakToRecoveryDelay().sample(rng), // double
																										// duration,
								configuration.getInfectiousnessCutoff(), // double
																						// thresholdLevel,
								configuration.getApproxPeakViralLoad().sample(rng)// double
																									// peakLevel
						)
				)
				.setImmunityModel(
						BiPhasicLogistic.calibrateImmuneActivity(
								configuration.getPeakImmuneResponseDelay().sample(rng), // peakTime,
								configuration.getApproxPeakImmuneResponse().sample(rng), // peakLevel,
								configuration.getImmuneWaningHalfLife().sample(rng) // halfLife
						)
				).build();

	}

}