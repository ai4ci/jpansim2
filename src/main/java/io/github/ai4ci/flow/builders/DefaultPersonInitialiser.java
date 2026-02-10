package io.github.ai4ci.flow.builders;

import java.util.Optional;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

/**
 * Default person initialiser used by the compositional {@link DefaultModelBuilder}.
 *
 * <p>This interface provides a canonical implementation for converting a
 * baselined {@link Person} into an immutable {@link ImmutablePersonState}.
 * It composes the three focused in‑host initialiser interfaces
 * {@link DefaultInHostStochasticStateInitialiser},
 * {@link DefaultInHostPhenomenologicalStateInitialiser} and
 * {@link DefaultInHostMarkovStateInitialiser} and supplies a default
 * {@link #initialisePerson(ImmutablePersonState.Builder, Person, Sampler)}
 * that performs the common orchestration tasks.
 *
 * <p>Role in composition: {@link DefaultModelBuilder} implements this
 * interface and delegates person initialisation to the default method via
 * {@code DefaultPersonInitialiser.super.initialisePerson(...)}, keeping the
 * builder focused on composition while the default initialiser centralises
 * the mapping from configuration to runtime state. Callers that require a
 * different initialisation strategy may either implement an alternative
 * interface or subclass {@code DefaultModelBuilder} and override the
 * delegation point.
 *
 * <p>Rationale and behaviour: the default method applies demographic
 * adjustments to the outbreak execution configuration (using
 * {@link ReflectionUtils#modify}), initialises the person's state machine,
 * instantiates an appropriate in‑host model via
 * {@link #initialiseInHostModel(InHostConfiguration, ExecutionConfiguration, Optional, Sampler, int)}
 * and sets importation exposure using values from the supplied
 * {@link SetupConfiguration}. Keeping this orchestration in one place
 * reduces duplication and makes it straightforward to evolve the
 * initialisation contract.
 *
 * @author Rob Challen
 */
public interface DefaultPersonInitialiser extends DefaultInHostStochasticStateInitialiser, DefaultInHostPhenomenologicalStateInitialiser, DefaultInHostMarkovStateInitialiser {

    /**
     * Convert a baselined {@link Person} into an immutable runtime
     * {@link ImmutablePersonState}.
     *
     * <p>Key steps performed by the default implementation:
     * <ol>
     *   <li>Adjust the outbreak {@link ExecutionConfiguration} for the
     *       individual's demographic group using
     *       {@link ReflectionUtils#modify}.</li>
     *   <li>Obtain the {@link SetupConfiguration} and compute an
     *       importation exposure probability using
     *       {@link SetupConfiguration#getInitialImports()} and the network
     *       size (this implements a simple uniform seeding strategy).</li>
     *   <li>Set simple per‑person modifiers (transmissibility, mobility,
     *       compliance, susceptibility, app use) to conservative defaults.
     *       These may be adjusted by downstream mechanisms or policies.</li>
     *   <li>Create the in‑host model instance by delegating to
     *       {@link #initialiseInHostModel(InHostConfiguration, ExecutionConfiguration, Optional, Sampler, int)}
     *       which dispatches to the specific in‑host initialisers based on
     *       the concrete configuration type.</li>
     *   <li>Set importation exposure and initial immunisation dose.</li>
     * </ol>
     *
     * <p>Configuration driven behaviour: behaviour is governed by the
     * outbreak's execution and setup configuration. Demographic adjustments
     * derived from {@link ExecutionConfiguration#getDemographicAdjustment()}
     * can change sampled values for distributions and therefore alter the
     * realised per‑person initial state. The importation exposure is derived
     * from {@link SetupConfiguration#getInitialImports()} and
     * {@link SetupConfiguration#getNetwork()}.
     *
     * <p>Extension guidance: implementers that wish to alter only the in‑host
     * initialiser should implement one of the specialised
     * {@code DefaultInHost...Initialiser} interfaces; larger changes to the
     * person initialisation orchestration can be achieved by subclassing
     * {@code DefaultModelBuilder} and overriding the delegation point.
     * Avoid heavy IO in this method; if expensive per‑person computation is
     * required prefer to precompute aggregates during outbreak baselining.
     *
     * @param builder the person state builder to populate
     * @param person the person to initialise (must have a baselined state)
     * @param rng a random sampler used for stochastic choices
     * @return the initialised {@link ImmutablePersonState}
     */
    default ImmutablePersonState initialisePerson(ImmutablePersonState.Builder builder, Person person,
            Sampler rng) {
        ExecutionConfiguration params = 
                ReflectionUtils.modify(
                        person.getOutbreak().getExecutionConfiguration(),
                        person.getOutbreak().getExecutionConfiguration().getDemographicAdjustment(),
                        person.getDemographic()
                );
        
        SetupConfiguration configuration = person.getOutbreak().getSetupConfiguration();
        // PersonBaseline baseline = person.getBaseline();
        double limit = ((double) configuration.getInitialImports())/configuration.getNetwork().getNetworkSize();
        
        builder
            .setTransmissibilityModifier(1.0)
            .setMobilityModifier(1.0)
            .setComplianceModifier(1.0)
            .setSusceptibilityModifier(1.0)
            .setAppUseModifier(1.0)
            
            .setInHostModel(
                initialiseInHostModel(params.getInHostConfiguration(),
                        params,
                        Optional.ofNullable(person.getDemographic()), rng, person.getOutbreak().getCurrentState().getTime())
            )
            .setImportationExposure(
                    rng.uniform() < limit ? 2.0 : 0
            )
            .setImmunisationDose(0D);
        
        return builder.build();
    }
    

    /**
     * Generic dispatcher that creates an in‑host model instance appropriate
     * to the provided configuration.
     *
     * <p>This method inspects the runtime type of the supplied
     * {@link InHostConfiguration} and forwards to the corresponding specialised
     * initialiser. It keeps the initialisation code in a single place so
     * that the {@link #initialisePerson} method need only call a single
     * entry point when creating the in‑host component of the person state.
     *
     * <p>Implementers of additional in‑host model types should add an
     * appropriate branch here or extend the dispatch mechanism to use a
     * registry if more dynamic behaviour is required.
     *
     * @param <CFG> concrete in‑host configuration type
     * @param config the in‑host configuration instance
     * @param execConfig the (possibly demographic adjusted) execution configuration
     * @param person optional person demographic used when applying demographic adjustments
     * @param rng sampler used to draw distributional entries
     * @param time current simulation time used for time‑dependent initialisation
     * @return an {@link InHostModelState} instance suitable for the supplied config
     * @throws RuntimeException if the configuration type is not recognised
     */
    @SuppressWarnings("unchecked")
    default <CFG extends InHostConfiguration> InHostModelState<CFG> initialiseInHostModel(CFG config, ExecutionConfiguration execConfig, Optional<PersonDemographic> person, Sampler rng, int time) {
        if (config instanceof PhenomenologicalModel) return (InHostModelState<CFG>) initialiseInHostModel((PhenomenologicalModel) config, execConfig, person, rng, time);
        if (config instanceof StochasticModel) return (InHostModelState<CFG>) initialiseInHostModel((StochasticModel) config, execConfig, person, rng, time);
        if (config instanceof MarkovStateModel) return (InHostModelState<CFG>) initialiseInHostModel((MarkovStateModel) config, execConfig, person, rng, time);
        throw new RuntimeException("Unknown in host configuration type");
    }
     
    
    
    
}