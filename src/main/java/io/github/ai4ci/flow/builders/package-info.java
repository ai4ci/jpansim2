/**
 * Build-time and setup documentation for the model builder components in
 * {@code io.github.ai4ci.abm.builders}.
 *
 * <p>This package contains abstractions and helpers used to construct a runnable
 * simulation from configuration inputs. It primarily exposes the
 * {@link io.github.ai4ci.flow.builders.AbstractModelBuilder AbstractModelBuilder}
 * that coordinates a five-stage build process (setup, baselining and
 * initialisation) for both the outbreak-level model and each individual agent.
 * The rest of the package contains concrete builders and utility helpers used
 * by experiments to produce a configured {@link io.github.ai4ci.abm.Outbreak}.
 *</p>
 *
 * <h2>High-level build stages</h2>
 * <p>The model construction proceeds in five logical stages, invoked by the
 * {@link io.github.ai4ci.flow.builders.AbstractModelBuilder AbstractModelBuilder} (see
 * its {@code do*} methods):</p>
 * <ol>
 *   <li><b>setupOutbreak</b> — installs the {@link io.github.ai4ci.config.setup.SetupConfiguration SetupConfiguration}
 *       on the {@link io.github.ai4ci.abm.ModifiableOutbreak ModifiableOutbreak}, creates the container for people
 *       (a {@link io.github.ai4ci.util.ThreadSafeArray ThreadSafeArray&lt;Person&gt;}), and constructs the social network
 *       structure (the {@link io.github.ai4ci.abm.SocialRelationship SocialRelationship} list). This stage is responsible
 *       for structural configuration such as spatial helpers (Hilbert coordinate mapping), population size and any
 *       static layout that the simulation requires.</li>
 *   <li><b>baselineOutbreak</b> — populates non-dynamic, outbreak-wide baseline values (producing an
 *       {@link io.github.ai4ci.abm.ImmutableOutbreakBaseline}) such as disease baseline parameters, default
 *       policy/behaviour state names, contact-detection probabilities, and global scalars or caps. This stage assumes
 *       the setup stage has completed and the social network is available.</li>
 *   <li><b>baselinePerson</b> — executed once per agent to assign static person-level parameters and baselines
 *       (producing an {@link io.github.ai4ci.abm.ImmutablePersonBaseline}). Typical outputs from this stage include:
 *       demographic attributes ({@link io.github.ai4ci.abm.PersonDemographic}), baseline mobility / compliance modifiers,
 *       baseline susceptibility and transmissibility modifiers, default behaviour state machine assignment, and
 *       any per-person tuning derived from external data or sampling procedures.</li>
 *   <li><b>initialiseOutbreak</b> — constructs the initial runtime {@link io.github.ai4ci.abm.ImmutableOutbreakState},
 *       linking the {@link io.github.ai4ci.config.execution.ExecutionConfiguration ExecutionConfiguration} (available tests, vaccine definitions,
 *       screening probabilities, timing assumptions) and setting initial policy state machine choices or schedules.</li>
 *   <li><b>initialisePerson</b> — the final per-agent initialisation that produces the runtime {@link io.github.ai4ci.abm.PersonState}.
 *       This combines baseline values, any seeding rules, and initial in-host/risk model initial conditions. The initialiser
 *       also primes the agent's {@link io.github.ai4ci.flow.mechanics.StateMachine StateMachine} with the baseline behaviour.</li>
 * </ol>
 *
 * <h2>Model composition: how to customise the default builder</h2>
 * <p>The package provides a ready‑made concrete builder {@link io.github.ai4ci.flow.builders.DefaultModelBuilder}
 * that composes a small set of default fragments. Each fragment is an interface
 * that implements a single concern with a default method — for example
 * {@link io.github.ai4ci.flow.builders.DefaultNetworkSetup},
 * {@link io.github.ai4ci.flow.builders.DefaultOutbreakBaseliner},
 * {@link io.github.ai4ci.flow.builders.DefaultPersonBaseliner} and the
 * {@code DefaultInHost*Initialiser} interfaces. This organisation makes it
 * straightforward to swap behaviour by either replacing a fragment or by
 * subclassing the default builder.</p>
 *
 * <p>Recommended extension patterns (in increasing order of flexibility):
 * <ol>
 *   <li>Subclass {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} and override a small number
 *       of delegation methods such as
 *       {@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak(io.github.ai4ci.abm.ModifiableOutbreak, io.github.ai4ci.config.setup.SetupConfiguration, io.github.ai4ci.util.Sampler)}
 *       or
 *       {@link io.github.ai4ci.flow.builders.DefaultModelBuilder#initialisePerson(io.github.ai4ci.abm.ImmutablePersonState.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}
 *       when you need a targeted change.</li>
 *   <li>Compose a new concrete builder by implementing or mixing different
 *       small interfaces. For example implement a custom
 *       {@code DefaultNetworkSetup} to alter spatial placement while keeping
 *       the standard person baselining behaviour.</li>
 *   <li>Use a factory or dependency injection at experiment bootstrap (for example in
 *       {@link io.github.ai4ci.flow.ExecutionBuilder}) to select a concrete
 *       {@link io.github.ai4ci.flow.builders.AbstractModelBuilder} implementation at runtime. This is the
 *       most flexible approach when you need dynamic selection of builders
 *       based on experiment configuration.</li>
 * </ol>
 *
 * <p>Practical hints:
 * <ul>
 *   <li>When composing fragments ensure the final concrete builder implements the
 *       set of interfaces required by the orchestration code — for example
 *       {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser} expects
 *       the specialised in‑host initialiser interfaces to be available.</li>
 *   <li>Prefer composition (small interfaces) over large monoliths: unit
 *       tests and mocking are far easier for small focused interfaces.</li>
 *   <li>Keep heavy IO or slow lookups in the setup or baselining stage so
 *       that per‑person initialisation remains fast and parallelisable.</li>
 * </ul>
 *
 * <h2>Configuration → builder → ABM mapping</h2>
 * <p>The table below documents common configuration entries in
 * {@code io.github.ai4ci.config}, which builder method consumes them and
 * which ABM class or field is ultimately set. Use the links to jump to the
 * implementation that performs the mapping.</p>
 *
 * <table>
 *   <caption>Mapping from configuration to ABM via builders</caption>
 *   <tr>
 *     <th>Configuration source</th>
 *     <th>Builder consumer</th>
 *     <th>ABM target (field/type)</th>
 *     <th>Notes</th>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.setup.SetupConfiguration#getInitialImports()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser#initialisePerson(io.github.ai4ci.abm.ImmutablePersonState.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutablePersonState#getImportationExposure()}</td>
 *     <td>Simple uniform seeding probability used to set an importation dose.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.setup.SetupConfiguration#getNetwork()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak(io.github.ai4ci.abm.ModifiableOutbreak, io.github.ai4ci.config.setup.SetupConfiguration, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ModifiableOutbreak} social network structures (placement and links)</td>
 *     <td>Network topology and size guide agent placement and contact construction.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.setup.SetupConfiguration#getDemographics()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultPersonBaseliner#baselinePerson(io.github.ai4ci.abm.ImmutablePersonBaseline.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutablePersonBaseline} demographic attributes</td>
 *     <td>Demographic lookups and CSV-driven assignments typically happen here.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.execution.ExecutionConfiguration#getR0()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultOutbreakBaseliner#baselineOutbreak(io.github.ai4ci.abm.ImmutableOutbreakBaseline.Builder, io.github.ai4ci.abm.Outbreak, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutableOutbreakBaseline#getViralLoadTransmissibilityParameter()}</td>
 *     <td>Calibrated using {@link io.github.ai4ci.abm.Calibration} to match population-level R0.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.execution.ExecutionConfiguration#getDefaultPolicyModel()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultOutbreakBaseliner#baselineOutbreak(io.github.ai4ci.abm.ImmutableOutbreakBaseline.Builder, io.github.ai4ci.abm.Outbreak, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutableOutbreakBaseline#getDefaultPolicyState()}</td>
 *     <td>Also used to initialise the outbreak policy state machine.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.execution.ExecutionConfiguration#getDefaultBehaviourModel()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultPersonBaseliner#baselinePerson(io.github.ai4ci.abm.ImmutablePersonBaseline.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutablePersonBaseline#getDefaultBehaviourState()}</td>
 *     <td>Assigned to each person's behaviour state machine during baselining.</td>
 *   </tr>
 *   <tr>
 *     <td>Contact / compliance / app use probabilities (ExecutionConfiguration getters)</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultPersonBaseliner#baselinePerson(io.github.ai4ci.abm.ImmutablePersonBaseline.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutablePersonBaseline#getMobilityBaseline()}, {@link io.github.ai4ci.abm.ImmutablePersonBaseline#getComplianceBaseline()}, {@link io.github.ai4ci.abm.ImmutablePersonBaseline#getAppUseProbability()}</td>
 *     <td>Sampled per person after demographic adjustment.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.github.ai4ci.config.execution.ExecutionConfiguration#getInHostConfiguration()}</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser#initialiseInHostModel(io.github.ai4ci.config.inhost.InHostConfiguration, io.github.ai4ci.config.execution.ExecutionConfiguration, java.util.Optional, io.github.ai4ci.util.Sampler, int)}</td>
 *     <td>Per-person {@link io.github.ai4ci.abm.inhost.InHostModelState} (phenomenological, stochastic or Markov)</td>
 *     <td>Dispatched by the person initialiser to the specialised in‑host initialisers.</td>
 *   </tr>
 *   <tr>
 *     <td>Severity / case / hospitalisation / fatality rates (ExecutionConfiguration)</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultInHostMarkovStateInitialiser#initialiseInHostModel(io.github.ai4ci.config.inhost.MarkovStateModel, io.github.ai4ci.config.execution.ExecutionConfiguration, java.util.Optional, io.github.ai4ci.util.Sampler, int)}</td>
 *     <td>{@link io.github.ai4ci.abm.inhost.InHostMarkovState} transition probabilities</td>
 *     <td>Converted to per-day probabilities with the project's Conversions helpers.</td>
 *   </tr>
 *   <tr>
 *     <td>Screening probabilities and trigger thresholds (ExecutionConfiguration)</td>
 *     <td>{@link io.github.ai4ci.flow.builders.DefaultOutbreakInitialiser#initialiseOutbreak(io.github.ai4ci.abm.ImmutableOutbreakState.Builder, io.github.ai4ci.abm.Outbreak, io.github.ai4ci.util.Sampler)}</td>
 *     <td>{@link io.github.ai4ci.abm.ImmutableOutbreakState} screening settings and trigger values</td>
 *     <td>Used as conservative runtime priors for detection and policy activation.</td>
 *   </tr>
 * </table>
 *
 * <p>Note: the table is illustrative rather than exhaustive. For a complete
 * trace follow the linked methods to the small default interfaces in this
 * package; those methods are the canonical place to find mapping logic.
 *
 * <h2>Practical extension points</h2>
 * <ul>
 *   <li>To change how agents are created or how configuration maps to individuals, extend
 *       {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} and override the {@code setup/baseline/initialise}
 *       hooks, or use composition by mixing alternative default interfaces. Wire your builder into your experiment bootstrap code (for
 *       example {@link io.github.ai4ci.flow.ExecutionBuilder}).</li>
 *   <li>If you need to parameterise agents from external data sources (CSV, databases), perform that lookup
 *       in your {@code baselinePerson} implementation and use the builder to write demographic/baseline values.</li>
 *   <li>To expose different tests or vaccines, edit the {@link io.github.ai4ci.config.execution.ExecutionConfiguration}
 *       passed to the outbreak and ensure your {@code baselinePerson} / behaviour models consult those definitions.</li>
 * </ul>
 *
 * <h2>Where to look next in the codebase</h2>
 * <ul>
 *   <li>{@link io.github.ai4ci.flow.builders.AbstractModelBuilder} — the orchestrator of the five-stage build process.</li>
 *   <li>Concrete builders in this package — examples of how to map external inputs to per-person baselines and states.</li>
 *   <li>{@link io.github.ai4ci.abm.ModelNav} — small helpers for reading configuration and baseline values during building and runtime.</li>
 * </ul>
 * 
 * <img src="setup.png" alt="Diagram of the five-stage model build process" style="display: block; margin: auto;" />
 *
 * @see io.github.ai4ci.flow.builders.AbstractModelBuilder
 * @see io.github.ai4ci.config.setup.SetupConfiguration
 * @see io.github.ai4ci.config.execution.ExecutionConfiguration
 * @see io.github.ai4ci.config.TestParameters
 * @author Rob Challen
 */
package io.github.ai4ci.flow.builders;
