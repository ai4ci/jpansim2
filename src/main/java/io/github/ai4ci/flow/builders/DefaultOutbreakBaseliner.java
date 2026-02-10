package io.github.ai4ci.flow.builders;

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.util.Sampler;

/**
 * Default logic for producing an {@link ImmutableOutbreakBaseline} from an
 * {@link Outbreak} and execution configuration.
 *
 * <p>This interface provides a small, focussed default implementation of
 * outbreak baselining that computes outbreak level aggregates and model
 * calibration parameters. It is intended to be composed into higher level
 * builders such as {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}
 * where composition keeps responsibilities separated and makes it easy to
 * swap alternative baseline strategies.
 *
 * <p>Composition with {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}:
 * {@code DefaultModelBuilder} implements this interface and delegates to the
 * default implementation defined here via
 * {@code DefaultOutbreakBaseliner.super.baselineOutbreak(...)}. This makes the
 * wiring explicit: callers can use {@code DefaultModelBuilder} as a ready‑made
 * builder or substitute alternative baseliner logic by implementing this
 * interface differently or by subclassing the model builder and overriding
 * the delegation point.
 *
 * <p>Rationale: keeping outbreak baselining as a composable default makes the
 * codebase easier to extend and test. The default implementation performs
 * common tasks such as inferring a viral load transmissibility parameter,
 * computing expected contacts per person, deriving severity cutoffs from the
 * configured in‑host model and building an infectivity profile.
 *
 * <p>Downstream uses: the returned {@code ImmutableOutbreakBaseline} is used
 * by the initialisation stage of the simulation and by runtime components
 * that need global priors (for example transmission scaling, default policy
 * state and severity thresholds). See
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} for an example of
 * how this interface is composed into the concrete model builder.
 *
 * @author Rob Challen
 */
public interface DefaultOutbreakBaseliner {

    
    /**
     * Produce an outbreak baseline from the supplied outbreak and execution
     * configuration.
     *
     * <p>This default implementation performs the following key steps:
     * <ol>
     *   <li>Obtain the {@link ExecutionConfiguration} from the outbreak.</li>
     *   <li>Attempt a fast inference of the viral load transmissibility
     *       parameter with a fallback to a more robust method on failure.</li>
     *   <li>Compute expected contacts per person using {@link Calibration}.
     *   </li>
     *   <li>Derive severity cutoffs by consulting the configured
     *       {@link InHostConfiguration}.</li>
     *   <li>Construct an infectivity profile using
     *       {@link InHostConfiguration#getInfectivityProfile}.</li>
     *   <li>Set a conservative symptom duration from the configured severity
     *       profile quantile.</li>
     * </ol>
     *
     * <p>Composition notes: when this method is composed into
     * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} the builder
     * delegates to the default implementation shown here. Implementers that
     * provide an alternative baseliner should preserve the expectation that
     * people have already been baselined and avoid performing heavy IO or
     * long running work inside this method unless the calling builder is
     * specifically designed to parallelise or manage such work.
     *
     * <p>Notes for implementers: the method assumes people have already been
     * baselined (see model builder sequencing). The try/catch around the
     * transmission parameter inference provides a fast path for typical
     * datasets while guarding against pathological cases where the quick
     * routine fails.
     *
     * @param builder builder used to assemble the {@link ImmutableOutbreakBaseline}
     * @param outbreak the outbreak used to compute aggregates and calibration
     * @param sampler a sampler available for any stochastic choices during baselining
     * @return a built {@link ImmutableOutbreakBaseline} containing global priors and calibration values
     */
    default ImmutableOutbreakBaseline baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak,
            Sampler sampler) {
        ExecutionConfiguration configuration = outbreak.getExecutionConfiguration();
    
        //N.B. happens after people are baselined.., I think
        double parameter;
        try {
            parameter = Calibration.inferViralLoadTransmissionParameterQuick(outbreak, configuration.getR0());
        } catch (Exception e) {
            parameter = Calibration.inferViralLoadTransmissionParameter(outbreak, configuration.getR0());
        }
        builder
            .setDefaultPolicyState( configuration.getDefaultPolicyModel() )
            .setViralLoadTransmissibilityParameter( 
                    parameter
            )
            .setExpectedContactsPerPersonPerDay(
                    Calibration.contactsPerPersonPerDay(outbreak)
            )
            .setSeveritySymptomsCutoff(
                    configuration.getInHostConfiguration().getSeveritySymptomsCutoff(outbreak, configuration)
            )
            .setSeverityHospitalisationCutoff(
                    configuration.getInHostConfiguration().getSeverityHospitalisationCutoff(outbreak, configuration)
            )
            .setSeverityDeathCutoff(
                    configuration.getInHostConfiguration().getSeverityFatalityCutoff(outbreak, configuration)
            )
            .setInfectivityProfile(
                InHostConfiguration.getInfectivityProfile(configuration,parameter, 100, 100)
            )
            .setSymptomDuration(
                (int) configuration.getSeverityProfile().getQuantile(0.95)
            )
            ;
        outbreak.getStateMachine().init( configuration.getDefaultPolicyModel() );
        return builder.build();
    }
    
}