package io.github.ai4ci.flow.builders;

import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.util.Sampler;

/**
 * Default outbreak initialisation behaviour.
 *
 * <p>
 * This interface provides a focussed default implementation that produces the
 * runtime {@link ImmutableOutbreakState} from a baselined {@link Outbreak} and
 * its {@link io.github.ai4ci.config.execution.ExecutionConfiguration}. The
 * default method encapsulates straightforward mappings from execution
 * configuration priors into the outbreak runtime state (for example
 * transmissibility modifiers, presumed incubation and infectious periods, and
 * screening/triggers).
 *
 * <p>
 * Role in composition: this interface is designed to be composed into higher
 * level builders such as
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}. The
 * {@code DefaultModelBuilder} implements this interface and delegates outbreak
 * initialisation to the default implementation using
 * {@code DefaultOutbreakInitialiser.super.initialiseOutbreak(...)}. This
 * explicit delegation keeps the wiring declarative and allows alternative
 * initialisation strategies to be substituted by implementing this interface
 * differently or by overriding the delegation point in a subclass of the model
 * builder.
 *
 * <p>
 * Rationale and extension points: keeping outbreak initialisation as a small,
 * composable default makes the code easier to test and extend. If a simulation
 * requires a custom outbreak initial state (for example different policy
 * parameters or additional runtime metrics), implementers should either provide
 * an alternative implementation of this interface or subclass
 * {@code DefaultModelBuilder} and override
 * {@link DefaultModelBuilder#initialiseOutbreak(ImmutableOutbreakState.Builder, Outbreak, Sampler)}.
 * Avoid placing heavy IO or long running tasks inside this method unless the
 * caller (the model builder) is explicitly designed to parallelise or manage
 * such work; prefer to perform expensive data preparation in the setup stage or
 * during baselining.
 *
 * <p>
 * Sequencing expectations: the default implementation assumes the outbreak has
 * already been baselined (see {@link DefaultOutbreakBaseliner}) and that
 * demographic and network structures are in place; it therefore should be
 * invoked only after baselining has completed.
 *
 * @author Rob Challen
 */
public interface DefaultOutbreakInitialiser {

	/**
	 * Build the initial runtime outbreak state.
	 *
	 * <p>
	 * The default implementation maps a small set of values from the
	 * {@link ExecutionConfiguration} into the outbreak state builder. These
	 * values are intended as conservative runtime priors used by the simulator
	 * (for example assumed incubation and infectious durations used by detection
	 * and screening logic).
	 *
	 * <p>
	 * When composed into
	 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} this method is
	 * invoked via explicit delegation. Implementers replacing the default should
	 * preserve the contract that the outbreak is baselined and that the returned
	 * object contains global runtime priors only; any additional computed
	 * aggregates should be added in a well documented way.
	 *
	 * @param builder  builder for {@link ImmutableOutbreakState}
	 * @param outbreak the baselined outbreak used as input
	 * @param sampler  a sampler available for stochastic choices during
	 *                 initialisation
	 * @return a built {@link ImmutableOutbreakState} containing runtime priors
	 */
	default ImmutableOutbreakState initialiseOutbreak(
			ImmutableOutbreakState.Builder builder, Outbreak outbreak,
			Sampler sampler
	) {
		ExecutionConfiguration config = outbreak.getExecutionConfiguration();
		builder.setTransmissibilityModifier(1.0)
				.setContactDetectedProbability(
						config.getContactDetectedProbability()
				)
				.setPresumedInfectiousPeriod(
						config.getInitialEstimateInfectionDuration().intValue()
				)
				.setPresumedIncubationPeriod(
						config.getInitialEstimateIncubationPeriod().intValue()
				)
				.setPresumedSymptomSensitivity(
						config.getInitialEstimateSymptomSensitivity()
				)
				.setPresumedSymptomSpecificity(
						config.getInitialEstimateSymptomSpecificity()
				).setScreeningProbability(config.getInitialScreeningProbability())
				.setTriggerValue(config.getLockdownTriggerValue());

		return builder.build();
	}
}