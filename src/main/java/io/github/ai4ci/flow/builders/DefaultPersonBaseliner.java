package io.github.ai4ci.flow.builders;

import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

/**
 * Default logic for producing a per‑person {@link ImmutablePersonBaseline}.
 *
 * <p>This interface provides a focused default implementation for person
 * baselining that populates persistent person attributes such as mobility
 * baseline, compliance, app use probability and symptom test characteristics.
 * It centralises the common per‑person baseline logic so that alternative
 * builders can reuse or replace it easily.
 *
 * <p>Role in composition: {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}
 * implements this interface and delegates person baselining to the default
 * method via
 * {@code DefaultPersonBaseliner.super.baselinePerson(...)}. This explicit
 * delegation keeps the wiring clear: callers may use the concrete
 * {@code DefaultModelBuilder} or substitute a different baseliner by
 * implementing this interface or by subclassing the model builder and
 * overriding the delegation point.
 *
 * <p>Behaviour and sequencing: the default method adjusts the outbreak's
 * execution configuration for the individual's demographic group (using
 * {@link io.github.ai4ci.util.ReflectionUtils#modify}), initialises the
 * person's state machine and samples distributional entries. The method is
 * intended to be invoked after setup and before initialisation of runtime
 * person state; in particular it assumes the outbreak's execution
 * configuration and demographic adjustment are available.
 *
 * <p>Extension guidance: implementers who replace this default should
 * preserve the contract that the method returns a built
 * {@link ImmutablePersonBaseline} and should avoid heavy IO or long running
 * tasks inside the method unless coordinated by the calling builder. For
 * compute heavy per‑person work consider precomputing aggregates during
 * outbreak baselining.
 *
 * @author Rob Challen
 */
public interface DefaultPersonBaseliner {

    /**
     * Build a per‑person baseline from the supplied person and the outbreak's
     * execution configuration.
     *
     * <p>The default implementation performs these steps:
     * <ol>
     *   <li>Adjust the outbreak execution configuration using the person's
     *       demographic via {@link ReflectionUtils#modify}.</li>
     *   <li>Initialise the person's state machine with the configured
     *       default behaviour model.</li>
     *   <li>Sample distributional entries from the adjusted configuration to
     *       populate mobility, compliance and app use probabilities.</li>
     *   <li>Set symptom sensitivity, specificity and self‑isolation depth from
     *       the adjusted configuration.</li>
     * </ol>
     *
     * <p>Composition note: when used from {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}
     * this method is invoked by the builder's delegation and therefore forms
     * the canonical per‑person baseline behaviour for the default builder.
     *
     * @param builder the person baseline builder to populate
     * @param person the person for whom the baseline is being built
     * @param rng random sampler used to sample distributional entries
     * @return the built {@link ImmutablePersonBaseline}
     */
    default ImmutablePersonBaseline baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng) {
        ExecutionConfiguration configuration = 
                ReflectionUtils.modify(
                        person.getOutbreak().getExecutionConfiguration(),
                        person.getOutbreak().getExecutionConfiguration().getDemographicAdjustment(),
                        person.getDemographic()
                );
        person.getStateMachine().init(configuration.getDefaultBehaviourModel());
        
        builder
            .setMobilityBaseline( configuration.getContactProbability().sample(rng) )
            .setComplianceBaseline( configuration.getComplianceProbability().sample(rng))
            .setAppUseProbability( configuration.getAppUseProbability().sample(rng))
            .setDefaultBehaviourState( configuration.getDefaultBehaviourModel() )
            
            .setSymptomSensitivity( configuration.getSymptomSensitivity().sample(rng))
            .setSymptomSpecificity( configuration.getSymptomSpecificity().sample(rng))
            .setSelfIsolationDepth( configuration.getMaximumSocialContactReduction().sample(rng) )
        ;
        
        return builder.build();
    }
    
}