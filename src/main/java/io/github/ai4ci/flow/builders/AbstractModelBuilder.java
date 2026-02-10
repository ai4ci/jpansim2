package io.github.ai4ci.flow.builders;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * High level template for constructing and initialising the simulation model.
 *
 * <p>This abstract builder defines the five staged model construction
 * process used by the simulation: setup, baseline outbreak, baseline
 * people, initialise outbreak and initialise people. Concrete subclasses
 * supply the domain specific logic by implementing the abstract hook
 * methods such as {@link #setupOutbreak(ModifiableOutbreak, SetupConfiguration, Sampler)}
 * and {@link #initialisePerson(ImmutablePersonState.Builder, Person, Sampler)}.
 *
 * <p>Configuration driven behaviour: callers provide an instance of
 * {@link io.github.ai4ci.config.setup.SetupConfiguration} to
 * {@link #doSetupOutbreak(ModifiableOutbreak, SetupConfiguration, Sampler)}.
 * The common implementation in this class sets the configuration on the
 * outbreak instance and allocates the people container from the outbreak's
 * population size. Implementations should consult the following fields on
 * the setup configuration when implementing hooks:
 * <ul>
 *   <li>{@link io.github.ai4ci.config.setup.SetupConfiguration#getInitialImports()}
 *       — suggested number of initial cases to seed when implementing
 *       seeding strategies.</li>
 *   <li>{@link io.github.ai4ci.config.setup.SetupConfiguration#getNetwork()}
 *       — network topology and size options used when creating social
 *       structures; use {@link io.github.ai4ci.config.setup.SetupConfiguration#hilbertBits}
 *       or {@link io.github.ai4ci.config.setup.SetupConfiguration#getHilbertCoords(Integer)}
 *       for spatial placement where appropriate.</li>
 *   <li>{@link io.github.ai4ci.config.setup.SetupConfiguration#getDemographics()}
 *       — demographic configuration used to draw population age/sex strata or
 *       location aware demographic attributes during person baselining.</li>
 * </ul>
 *
 * <p>Rationale and extension points: separating build stages makes the
 * construction process robust and testable. Each stage performs a distinct
 * responsibility so callers may validate preconditions before proceeding.
 * Subclasses should implement the abstract hooks to provide domain logic.
 * When writing hook implementations consider keeping them side effect free
 * where practical (builders return constructed immutable baseline/state
 * objects) and prefer to perform expensive IO or CPU work in the setup stage
 * where it is obvious to parallelise or cache results.
 *
 * <p>Concurrency: the base class initialises a thread safe people container
 * via {@link io.github.ai4ci.util.ThreadSafeArray}. Implementers that modify
 * shared outbreak structures should ensure thread safety; for example by
 * using local concurrent collections, synchronisation or by confining state
 * mutation to a single thread during setup.
 *
 * <p>Downstream consumers of the produced objects include the state machine
 * initialisers and runtime components such as
 * {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} and the
 * outbreak execution code which expect baselined outbreak and person
 * objects before simulation starts.
 *
 * @author Rob Challen
 */
public abstract class AbstractModelBuilder {

	/**
	 * Default constructor.
	 */
	public AbstractModelBuilder() {}
	
    /**
     * Perform initial outbreak setup.
     *
     * <p>This method performs the first stage of the build: it records the
     * supplied {@link SetupConfiguration} on the outbreak, allocates the
     * thread safe people container sized by the configured population, and
     * then delegates to {@link #setupOutbreak(ModifiableOutbreak, SetupConfiguration, Sampler)}
     * for domain specific setup (such as creating households, workplaces and
     * seeding initial infections).
     *
     * <p>How configuration drives behaviour: implementers should read the
     * {@link SetupConfiguration#getInitialImports()} value when deciding how
     * many initial infections to add. Network topology and size should be
     * derived from {@link SetupConfiguration#getNetwork()} and demographic
     * defaults from {@link SetupConfiguration#getDemographics()}.
     *
     * <p>Extension guidance: heavy IO or long running construction work is
     * best performed inside the {@link #setupOutbreak} hook where callers
     * can control parallelism. Keep this common method deterministic and
     * fail early for invalid configurations.
     *
     * @param outbreak the modifiable outbreak instance to populate
     * @param config the setup configuration that drives topology and seeding
     * @param sampler a random sampler used for stochastic choices during setup
     * @return the populated outbreak instance
     * @throws RuntimeException if the configuration is null or invalid
     */
    public ModifiableOutbreak doSetupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler) {
        if (config == null) throw new RuntimeException("Outbreak is not configured correctly for setup.");
        outbreak.setSetupConfiguration( config );
        outbreak.setPeople(new ThreadSafeArray<Person>(Person.class, outbreak.getPopulationSize()));
        return setupOutbreak(outbreak,config,sampler);
    }
    
    /**
     * Perform outbreak baselining.
     *
     * <p>This method checks that prior stages have completed successfully
     * (setup, execution configuration and social network) and that people have
     * been baselined before delegating to
     * {@link #baselineOutbreak(ImmutableOutbreakBaseline.Builder, Outbreak, Sampler)}
     * to populate outbreak level baseline parameters.
     *
     * <p>How configuration drives behaviour: the outbreak baseline should use
     * demographic defaults from {@link SetupConfiguration#getDemographics()}
     * where population‑level priors are required. Network derived metrics
     * available from {@link SetupConfiguration#getNetwork()} can be used to
     * compute baseline contact rates or global transmission modifiers.
     *
     * <p>Extension guidance: implementers may cache computed aggregates
     * (for example age distributions or contact matrices) here so that
     * per‑person baselining can remain lightweight and possibly parallel.
     *
     * @param builder the outbreak baseline builder
     * @param o the outbreak to base
     * @param sampler a random sampler used during baselining
     * @return the completed outbreak baseline
     * @throws RuntimeException if preconditions for baselining are not met
     */
    public ImmutableOutbreakBaseline doBaselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak o , Sampler sampler) {
        if (o instanceof ModifiableOutbreak) {
            ModifiableOutbreak m = (ModifiableOutbreak) o;
            if(
                    !m.getPeople().isEmpty() &&
                    m.initialisedSetupConfiguration() &&
                    m.initialisedExecutionConfiguration() &&
                    m.initialisedSocialNetwork() &&
                    isBaselined(m.getPeople())
                ) {
                return baselineOutbreak(builder,o,sampler);
            }
        } 
        throw new RuntimeException("Outbreak is not configured correctly for baselining.");
    } 
    
    /**
     * Helper that checks the people array for baseline initialisation.
     *
     * <p>Note: this is an implementation detail and is intentionally private.
     */
    private boolean isBaselined(ThreadSafeArray<Person> people) {
        if (people.isEmpty()) return false;
        return ((ModifiablePerson) people.get(0)).initialisedBaseline();
    };

    /**
     * Perform per-person baselining.
     *
     * <p>This method validates that the outbreak has been sufficiently
     * prepared (setup, execution configuration and social network) before
     * delegating to {@link #baselinePerson(ImmutablePersonBaseline.Builder, Person, Sampler)}
     * which is expected to populate non dynamic person-level baseline
     * parameters such as demographic attributes or behaviour defaults.
     *
     * <p>How configuration drives behaviour: implementers should use the
     * outbreak's attached {@link SetupConfiguration} to derive individual
     * demographic attributes (for example by sampling age/sex strata from
     * {@link SetupConfiguration#getDemographics()}) and location assignment
     * using {@link SetupConfiguration#getNetwork()} options.
     *
     * @param builder the person baseline builder
     * @param p the person to baseline
     * @param sampler a random sampler used during baseline
     * @return the completed person baseline
     * @throws RuntimeException if preconditions for baselining are not met
     */
    public ImmutablePersonBaseline doBaselinePerson(ImmutablePersonBaseline.Builder builder, Person p, Sampler sampler) {
        // Filter only to people that have been fully initialised
        if (p.getOutbreak() instanceof ModifiableOutbreak) {
            ModifiableOutbreak m = (ModifiableOutbreak) p.getOutbreak();
            if (m.initialisedSetupConfiguration() &&
                    m.initialisedExecutionConfiguration() &&
                    m.initialisedSocialNetwork()
            ) {
                return baselinePerson(builder,p,sampler);
            }
        } 
        throw new RuntimeException("Person is not configured correctly for baselining.");
    }
    
    /**
     * Perform outbreak initialisation.
     *
     * <p>After baselining this method checks that all required outbreak
     * baseline and setup steps have been completed before delegating to
     * {@link #initialiseOutbreak(ImmutableOutbreakState.Builder, Outbreak, Sampler)}
     * to produce the runtime outbreak state used by the simulator.
     *
     * <p>How configuration drives behaviour: the initial outbreak state may
     * incorporate global modifiers derived from {@link SetupConfiguration}
     * (for example network scaling factors or demographic aggregates) which
     * are typically computed during the baselining stage.
     *
     * @param builder the outbreak state builder
     * @param o the outbreak to initialise
     * @param sampler a random sampler used during initialisation
     * @return the initial outbreak state
     * @throws RuntimeException if preconditions for initialisation are not met
     */
    public ImmutableOutbreakState doInitialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak o , Sampler sampler) {
        if (o instanceof ModifiableOutbreak) {
            ModifiableOutbreak m = (ModifiableOutbreak) o;
            if ( 
                    m.initialisedBaseline() &&
                    m.initialisedSetupConfiguration() &&
                    m.initialisedExecutionConfiguration() &&
                    m.initialisedSocialNetwork()
            ) {
                return initialiseOutbreak(builder,o,sampler);
            }
        } 
        throw new RuntimeException("Outbreak is not configured correctly for initialisation."); 
    }
    
    /**
     * Perform per-person initialisation.
     *
     * <p>This method ensures the person and its owning outbreak are properly
     * baselined and configured and then delegates to
     * {@link #initialisePerson(ImmutablePersonState.Builder, Person, Sampler)}
     * to set the person's runtime state (for example infection state,
     * immunity and other dynamic attributes). The implementation is the
     * canonical place to convert baseline parameters into runtime values.
     *
     * <p>How configuration drives behaviour: initial values such as starting
     * immunity or seeded infection timers should be derived from a combination
     * of the person baseline and outbreak level configuration (for example
     * lookups from {@link SetupConfiguration#getDemographics()}). The person
     * initialiser is responsible for converting baseline priors into the
     * concrete numeric state used by runtime components.
     *
     * @param builder the person state builder
     * @param p the person to initialise
     * @param sampler a random sampler used during initialisation
     * @return the initial person state
     * @throws RuntimeException if preconditions for initialisation are not met
     */
    public ImmutablePersonState doInitialisePerson(ImmutablePersonState.Builder builder, Person p, Sampler sampler) {
                    if (p instanceof ModifiablePerson) {
                        ModifiablePerson mp = (ModifiablePerson) p;
                        if (p.getOutbreak() instanceof ModifiableOutbreak) {
                            ModifiableOutbreak m = (ModifiableOutbreak) p.getOutbreak();
                            if (
                                    mp.initialisedBaseline() &&
                                    m.initialisedSetupConfiguration() &&
                                    m.initialisedExecutionConfiguration() &&
                                    m.initialisedSocialNetwork()
                            ) {
                                return initialisePerson(builder,p,sampler);
                            }
                        } 
                    }
                    throw new RuntimeException("Person is not configured correctly for initialisation.");
                }
    
    

    /**
     * Hook for domain specific outbreak setup.
     *
     * <p>Concrete subclasses implement this method to create people, social
     * structures and any other domain entities required by the simulation.
     * Implementations should consult the supplied {@link SetupConfiguration}
     * rather than external global state. The method returns the modified
     * outbreak instance.
     *
     * <p>Suggested responsibilities for implementations:
     * <ul>
     *   <li>Create persons and set baseline demographic placeholders or
     *       references to census data based on
     *       {@link SetupConfiguration#getDemographics()}.</li>
     *   <li>Construct social structures (households, workplaces) informed by
     *       {@link SetupConfiguration#getNetwork()} parameters.</li>
     *   <li>Seed initial infections using
     *       {@link SetupConfiguration#getInitialImports()} and any configured
     *       seeding strategy.</li>
     * </ul>
     *
     * @param outbreak the modifiable outbreak to configure
     * @param config the setup configuration driving creation choices
     * @param sampler a random sampler for stochastic choices
     * @return the configured outbreak instance
     */
    public abstract ModifiableOutbreak setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration config, Sampler sampler);
    
    /**
     * Hook for producing outbreak baseline values.
     *
     * <p>Subclasses should populate the outbreak baseline builder with static
     * parameters such as policy defaults, disease baseline parameters and any
     * other non dynamic settings. Keep the method side effect free and return
     * the built baseline.
     *
     * <p>Implementation tip: compute and cache aggregates derived from the
     * setup configuration (for example age pyramids, contact matrices or
     * network degree distributions) here to speed up per‑person baselining.
     *
     * @param builder the baseline builder to populate
     * @param outbreak the outbreak to base upon
     * @param sampler random sampler for any stochastic baseline choices
     * @return the built outbreak baseline
     */
    public abstract ImmutableOutbreakBaseline baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak, Sampler sampler);
    
    /**
     * Hook for producing person baseline values.
     *
     * <p>Implementations should author non dynamic person attributes such as
     * demographics, household membership and persistent behaviour settings.
     * These baseline values are used later by the person initialiser to
     * create runtime state.
     *
     * <p>Implementation tip: keep this method focused on stable attributes
     * (that do not change at runtime). Any derived values needed for fast
     * runtime initialisation can be precomputed here and stored in the
     * baseline object.
     *
     * @param builder the person baseline builder to populate
     * @param person the target person instance
     * @param rng random sampler for any stochastic choices
     * @return the built person baseline
     */
    public abstract ImmutablePersonBaseline baselinePerson(ImmutablePersonBaseline.Builder builder, Person person, Sampler rng);

    /**
     * Hook for converting baseline values into a runtime person state.
     *
     * <p>Concrete builders should use this method to translate baseline
     * attributes into the runtime state that will be used during simulation
     * (for example initial infection timers, immunity levels and behaviour
     * state). The method is the canonical place where baseline settings from
     * {@link io.github.ai4ci.config.setup.SetupConfiguration} and the baseline
     * builders are combined to produce a person state.
     *
     * <p>Developer guidance: prefer idempotent transformations and avoid
     * writing to shared mutable state from within this method; return the
     * constructed {@code ImmutablePersonState} and let the caller attach it
     * to the outbreak container.
     *
     * @param builder the runtime person state builder to populate
     * @param person the person to initialise
     * @param rng random sampler for stochastic initial values
     * @return the initial person state
     */
    public abstract ImmutablePersonState initialisePerson(ImmutablePersonState.Builder builder, Person person, Sampler rng);

    /**
     * Hook for creating the initial outbreak runtime state.
     *
     * <p>Subclasses produce the outbreak state used by the simulator. This is
     * where global runtime values that depend on the baselined outbreak are
     * computed and stored. Keep the method side effect free other than
     * producing the built state object.
     *
     * @param builder the outbreak state builder to populate
     * @param outbreak the outbreak to initialise
     * @param sampler a random sampler used during construction
     * @return the initial outbreak state
     */
    public abstract ImmutableOutbreakState initialiseOutbreak(ImmutableOutbreakState.Builder builder, Outbreak outbreak, Sampler sampler);

    
}