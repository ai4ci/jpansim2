/**
 * High-level documentation for the agent-based modelling package
 * {@code io.github.ai4ci.abm}.
 *
 * <p>
 * This package implements an agent-based infectious disease model with the
 * following primary capabilities and design conventions. The intent of this
 * package-level javadoc is to help developers and modelers understand where key
 * behaviours are implemented, where to change model assumptions, and how the
 * pieces interact.
 * </p>
 *
 * <h2>Core modelling concepts</h2>
 * <ul>
 * <li><b>Agents</b> — represented by {@link io.github.ai4ci.abm.Person Person}
 * objects. Each agent has both a static demographic description
 * ({@link io.github.ai4ci.abm.PersonDemographic PersonDemographic}) and
 * time-stamped state/history objects ({@link io.github.ai4ci.abm.PersonState
 * PersonState} and {@link io.github.ai4ci.abm.PersonHistory
 * PersonHistory}).</li>
 * <li><b>Temporal states</b> — shared behaviour and measurements that are
 * meaningful at a particular model time are exposed via
 * {@link io.github.ai4ci.abm.PersonTemporalState PersonTemporalState}. This
 * includes in-host quantities (viral load, severity), contact exposure
 * accumulated that day, and flags such as {@code isInfectious()},
 * {@code isSymptomatic()}, and incident markers.</li>
 * <li><b>Outbreak and configuration</b> — global parameters and model setup
 * live in {@link io.github.ai4ci.abm.Outbreak Outbreak} and its configuration
 * helpers (e.g. {@link io.github.ai4ci.config.execution.ExecutionConfiguration
 * ExecutionConfiguration} and
 * {@link io.github.ai4ci.config.setup.SetupConfiguration SetupConfiguration}).
 * Change default tests, thresholds and timing by editing the relevant
 * configuration objects.</li>
 * </ul>
 *
 * <h2>Network interactions</h2>
 * <p>
 * The social contact network is modelled as weighted relationships between
 * agents. Key points:
 * </p>
 * <ul>
 * <li>{@link io.github.ai4ci.abm.SocialRelationship SocialRelationship}
 * encapsulates an edge between two agents and stores a <em>relationship
 * strength</em> quantile. The per-day probability of a contact is modelled as
 * {@code relationshipStrength * adjMobility(personA) * adjMobility(personB)}
 * via
 * {@link io.github.ai4ci.abm.SocialRelationship#contactProbability(double,double)}.</li>
 * <li>Contacts produced by these relationships (and other mobility-driven
 * interactions) are materialised as {@link io.github.ai4ci.abm.Contact Contact}
 * objects and stored in daily histories
 * ({@link io.github.ai4ci.abm.PersonHistory#getTodaysContacts()}).</li>
 * <li>Additional context (masking, venue, duration) is encoded in contact
 * weights and influences per-contact transmission risk; augment or replace that
 * logic by changing the contact-building phase of the simulation (see social
 * network / mobility generators and any calibration code).</li>
 * </ul>
 *
 * <h2>In-host dynamics and viral load</h2>
 * <p>
 * The model separates within-host dynamics from population processes. Relevant
 * behaviours:
 * </p>
 * <ul>
 * <li>Each {@link io.github.ai4ci.abm.PersonTemporalState} exposes two
 * continuous in-host signals:
 * {@link io.github.ai4ci.abm.PersonTemporalState#getNormalisedViralLoad()
 * normalised viral load} and
 * {@link io.github.ai4ci.abm.PersonTemporalState#getNormalisedSeverity()
 * normalised severity}. Values are scaled so that <em>0</em> denotes no
 * detectable infection and <em>1</em> denotes a baseline infectious level. This
 * scale is used throughout the codebase to compare exposures, determine
 * detectability by tests, and decide symptomatic / hospitalisation events.</li>
 * <li>A person is considered infectious when
 * {@link io.github.ai4ci.abm.PersonTemporalState#isInfectious() isInfectious()}
 * returns {@code true} — implemented by thresholding
 * {@code getNormalisedViralLoad()} against an infectivity cutoff supplied by
 * the {@link io.github.ai4ci.abm.Outbreak#getSetupConfiguration() setup
 * configuration}.</li>
 * <li>Daily exposure accumulation (the sum of viral doses experienced that day)
 * is recorded via
 * {@link io.github.ai4ci.abm.PersonTemporalState#getContactExposure()
 * getContactExposure()} and stored as {@link io.github.ai4ci.abm.Exposure
 * Exposure} entries in the exposee's
 * {@link io.github.ai4ci.abm.PersonHistory#getTodaysExposures() PersonHistory}
 * when transmission occurs.</li>
 * <li>Incubation and infectious period assumptions used by the model are
 * available from the {@link io.github.ai4ci.abm.ModelNav ModelNav} / model
 * state accessors. Use
 * {@link io.github.ai4ci.abm.PersonTemporalState#incubPeriod() incubPeriod()}
 * and {@link io.github.ai4ci.abm.PersonTemporalState#infPeriod() infPeriod()}
 * to read the current working values.</li>
 * </ul>
 *
 * <h2>Testing, sensitivity and specificity</h2>
 * <p>
 * Testing behaviour is captured by {@link io.github.ai4ci.abm.TestResult
 * TestResult} and {@link io.github.ai4ci.config.TestParameters TestParameters}:
 * </p>
 * <ul>
 * <li><b>Viral load sampling:</b> When a test is taken the model records the
 * true {@link io.github.ai4ci.abm.TestResult#getViralLoadTruth() viral load
 * truth} (from
 * {@link io.github.ai4ci.abm.PersonTemporalState#getNormalisedViralLoad()}) and
 * then simulates measurement noise with
 * {@link io.github.ai4ci.config.TestParameters#applyNoise(double, io.github.ai4ci.util.Sampler)}
 * to produce a sampled value
 * ({@link io.github.ai4ci.abm.TestResult#getViralLoadSample()}).</li>
 * <li><b>Limit of detection (LoD):</b> Each
 * {@link io.github.ai4ci.config.TestParameters} defines a
 * {@link io.github.ai4ci.config.TestParameters#getLimitOfDetection() limit of
 * detection}. The
 * {@link io.github.ai4ci.abm.TestResult#getFinalObservedResult() final observed
 * result} compares the noisy sample against the LoD to determine a
 * positive/negative observation.</li>
 * <li><b>Sensitivity:</b> In this codebase sensitivity is represented
 * implicitly by the combination of the LoD and the noise model: for a given
 * true viral load, the probability that the noisy sample exceeds the LoD maps
 * to the empirical sensitivity. The default test types (e.g. {@code LFT},
 * {@code PCR}) provide baseline {@link io.github.ai4ci.config.TestParameters}
 * that can be modified via the
 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration#getAvailableTests()
 * execution configuration}.</li>
 * <li><b>Specificity:</b> Specificity is modelled as the probability that an
 * individual with very low viral load produces a sample below the LoD.
 * TestParameters also supply explicit specificity values used to compute
 * likelihood ratios
 * ({@link io.github.ai4ci.config.TestParameters#positiveLikelihoodRatio()} and
 * {@link io.github.ai4ci.config.TestParameters#negativeLikelihoodRatio()}).</li>
 * <li><b>Delays:</b> Test processing delay is sampled (log-normal) using
 * parameters in {@link io.github.ai4ci.config.TestParameters} and influences
 * when {@link io.github.ai4ci.abm.TestResult#isResultAvailable(int) results
 * become observable}.</li>
 * <li><b>Evidence and likelihoods:</b> The model computes log-likelihood ratios
 * for results ({@link io.github.ai4ci.abm.TestResult#trueLogLikelihoodRatio()})
 * which are used to update agent-level estimates of infection probability
 * (e.g., for smart agents or contact tracing logic).</li>
 * </ul>
 *
 * <h2>Tracing exposures and diagnostic history</h2>
 * <p>
 * Daily contacts, exposures and tests are stored in the agent's
 * {@link io.github.ai4ci.abm.PersonHistory PersonHistory}. Exposures link back
 * to the infecting agent via
 * {@link io.github.ai4ci.abm.Exposure#getExposerId()}; use
 * {@link io.github.ai4ci.abm.Exposure#getExposer(io.github.ai4ci.abm.PersonTemporalState)}
 * to resolve the exposer's {@link io.github.ai4ci.abm.PersonHistory} at the
 * relevant time.
 * </p>
 *
 * <h2>Extension points and where to change behaviour</h2>
 * <ul>
 * <li>Network generation and contact probability: modify the social network /
 * mobility generators or change
 * {@link io.github.ai4ci.abm.SocialRelationship#contactProbability(double,double)}
 * if you want different functional forms for contact likelihood.</li>
 * <li>In-host dynamics: replace or adjust the model that populates
 * {@link io.github.ai4ci.abm.PersonTemporalState#getNormalisedViralLoad()} and
 * {@link io.github.ai4ci.abm.PersonTemporalState#getNormalisedSeverity()} if
 * you need alternate within-host dynamics or scaling.</li>
 * <li>Testing regimes and properties: edit available
 * {@link io.github.ai4ci.config.TestParameters} in
 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration
 * ExecutionConfiguration} to change sensitivity, specificity, LoD or delay
 * distributions. Use {@link io.github.ai4ci.abm.TestResult.Type#modify()} to
 * start from a default and tune parameters programmatically.</li>
 * <li>Thresholds and timing: incubation and infectious period assumptions are
 * surfaced via {@link io.github.ai4ci.abm.ModelNav ModelNav} and the outbreak
 * configuration; change them in the global configuration or in calibration code
 * used before runs.</li>
 * </ul>
 *
 * <h2>Behaviour and policy state models</h2>
 * <p>
 * The runtime decision logic for both individuals and the system-wide policy is
 * implemented using the state model framework found in
 * {@link io.github.ai4ci.flow.mechanics.State State} and driven by
 * {@link io.github.ai4ci.flow.mechanics.StateMachine StateMachine} and
 * {@link io.github.ai4ci.flow.mechanics.StateMachineContext
 * StateMachineContext}:
 * </p>
 * <ul>
 * <li><b>State types:</b> Two complementary flavours of state are supported:
 * <ul>
 * <li>{@link io.github.ai4ci.flow.mechanics.State.BehaviourState
 * BehaviourState} — an enum-based state model that implements individual
 * behaviour logic (mobility, compliance, testing behaviour). These states
 * operate on {@link io.github.ai4ci.abm.PersonState PersonState} and update
 * {@link io.github.ai4ci.abm.PersonHistory PersonHistory}.</li>
 * <li>{@link io.github.ai4ci.flow.mechanics.State.PolicyState PolicyState} — an
 * enum-based state model that implements system-level policy logic
 * (population-level interventions, testing strategies, or triggering of new
 * policies). These states operate on {@link io.github.ai4ci.abm.OutbreakState
 * OutbreakState} and update {@link io.github.ai4ci.abm.OutbreakHistory}.</li>
 * </ul>
 * </li>
 * <li><b>Update phases:</b> The state framework separates the daily update into
 * two phases implemented by
 * {@link io.github.ai4ci.flow.mechanics.State#updateHistory(Object,Object,io.github.ai4ci.flow.mechanics.StateMachineContext,io.github.ai4ci.util.Sampler)}
 * (executed in the history stage) and
 * {@link io.github.ai4ci.flow.mechanics.State#nextState(Object,Object,io.github.ai4ci.flow.mechanics.StateMachineContext,io.github.ai4ci.util.Sampler)}
 * (executed at the state update stage). The history update is the appropriate
 * place for actions that materialise during the day (e.g. scheduling tests,
 * recording contacts) while the nextState step computes the behavioural or
 * policy state that should apply on the following day.</li>
 * <li><b>Policy-to-behaviour interaction:</b> Policies can directly influence
 * individuals in two ways:
 * <ol>
 * <li>By mutating the {@link io.github.ai4ci.abm.OutbreakState} (via the policy
 * state's builder) the policy can change global parameters (e.g. available
 * tests, thresholds, or contact restrictions) which are then observed by
 * individuals when they compute their next state.</li>
 * <li>By branching people into different behaviour models: policy states may
 * call
 * {@link io.github.ai4ci.flow.mechanics.StateUtils#branchPeopleTo(io.github.ai4ci.abm.OutbreakState, io.github.ai4ci.flow.mechanics.State.BehaviourState)}
 * (or similar helpers) to immediately push individuals into a different
 * behaviour model. The
 * {@link io.github.ai4ci.flow.mechanics.StateMachine#rememberCurrentState(io.github.ai4ci.flow.mechanics.State)}
 * and
 * {@link io.github.ai4ci.flow.mechanics.StateMachine#forceTo(io.github.ai4ci.flow.mechanics.State.PolicyState)}
 * semantics control how model switching is tracked and reversed.</li>
 * </ol>
 * </li>
 * <li><b>Behaviour update semantics:</b> Behaviour models are implemented as
 * enums that implement
 * {@link io.github.ai4ci.flow.mechanics.State.BehaviourState}. In their
 * {@code nextState} implementation they receive the current
 * {@link io.github.ai4ci.abm.PersonState} and a
 * {@link io.github.ai4ci.flow.mechanics.StateMachineContext} object containing
 * recent policy-level signals and context. Behaviour states can modify the
 * {@link io.github.ai4ci.abm.ImmutablePersonState.Builder} (the next state
 * builder) to schedule actions such as testing or isolation, and return the
 * behaviour enum that should apply tomorrow.</li>
 * <li><b>Where to change or extend:</b> To add or alter policy/behaviour logic
 * look at
 * <ul>
 * <li>{@link io.github.ai4ci.flow.mechanics.State State} — the state interface
 * and subtypes;</li>
 * <li>{@link io.github.ai4ci.flow.mechanics.StateMachine StateMachine} —
 * orchestration of history and state updates;</li>
 * <li>{@link io.github.ai4ci.flow.mechanics.StateMachineContext
 * StateMachineContext} — shared context/flags used by states;</li>
 * <li>{@link io.github.ai4ci.flow.mechanics.StateUtils StateUtils} — helper
 * functions used by states to implement common actions (branching, forcing, or
 * recording events);</li>
 * <li>{@link io.github.ai4ci.flow.mechanics.Updater Updater} — the component
 * that drives the simulation update cycle and invokes state update hooks for
 * both outbreak and agents.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <h2>Policy modelling notes</h2>
 * <ul>
 * <li>PolicyState implementations should be conservative about making
 * irreversible changes to individuals' next states because behaviour models may
 * subsequently override them. Use the state-machine branching helpers if you
 * need immediate, coordinated changes across the population.</li>
 * <li>Policy models can be used to implement dynamic testing strategies (for
 * example switching which tests are available or changing priorities), trigger
 * contact tracing workflows, or alter population-level mobility multipliers
 * that affect
 * {@link io.github.ai4ci.abm.SocialRelationship#contactProbability(double,double)}
 * outcomes.</li>
 * </ul>
 *
 * <h2>Examples and entry points</h2>
 * <p>
 * Look at the example experiment and baseline implementations to see the
 * pattern in practice:
 * </p>
 * <ul>
 * <li>{@link io.github.ai4ci.example.Experiment Experiment} — demonstrates
 * configuration of behaviour models and tests (see examples folder).</li>
 * <li>{@link io.github.ai4ci.abm.OutbreakBaseline OutbreakBaseline} — provides
 * default policy/behaviour baseline states and wiring into the state machine on
 * simulation start.</li>
 * </ul>
 *
 * @see io.github.ai4ci.abm.Person
 * @see io.github.ai4ci.abm.PersonHistory
 * @see io.github.ai4ci.abm.PersonTemporalState
 * @see io.github.ai4ci.abm.SocialRelationship
 * @see io.github.ai4ci.abm.Contact
 * @see io.github.ai4ci.abm.Exposure
 * @see io.github.ai4ci.abm.TestResult
 * @see io.github.ai4ci.config.TestParameters
 * @see io.github.ai4ci.abm.Outbreak
 */
package io.github.ai4ci.abm;
