/**
 * Configuration types and helpers that describe how a simulation population and
 * its social network are constructed.
 *
 * <p>
 * The classes in this package provide the immutable configuration objects
 * consumed by the model builders during the setup and baselining stages. The
 * primary responsibilities are to describe:
 * <ul>
 * <li>population level inputs such as initial imports and demographic data (see
 * {@link io.github.ai4ci.config.setup.SetupConfiguration});</li>
 * <li>network generation parameters for different graph models (see
 * {@link io.github.ai4ci.config.setup.NetworkConfiguration} and its
 * implementations {@link io.github.ai4ci.config.setup.ErdosReyniConfiguration},
 * {@link io.github.ai4ci.config.setup.WattsStrogatzConfiguration} and
 * {@link io.github.ai4ci.config.setup.BarabasiAlbertConfiguration});</li>
 * <li>demographic strategies that control how persons are created and how
 * contact strengths are computed (see
 * {@link io.github.ai4ci.config.setup.DemographicConfiguration} and its
 * implementations {@link io.github.ai4ci.config.setup.UnstratifiedDemography},
 * {@link io.github.ai4ci.config.setup.AgeStratifiedDemography} and
 * {@link io.github.ai4ci.config.setup.LocationAwareDemography}).</li>
 * </ul>
 *
 * <h2>How these types are used</h2>
 * <p>
 * At experiment bootstrap the {@link io.github.ai4ci.flow.ExecutionBuilder} and
 * a concrete {@link io.github.ai4ci.flow.builders.AbstractModelBuilder} consume
 * a {@link io.github.ai4ci.config.setup.SetupConfiguration}. The typical flow
 * is:
 * <ol>
 * <li>the builder reads {@code SetupConfiguration} and calls
 * {@link io.github.ai4ci.config.setup.NetworkConfiguration#generateGraph} to
 * build the social network graph during the setup stage;</li>
 * <li>the builder uses an instance of
 * {@link io.github.ai4ci.config.setup.DemographicConfiguration} to create
 * person stubs (via
 * {@link io.github.ai4ci.config.setup.DemographicConfiguration#createPersonStub})
 * and to compute per‑pair relationship strengths;</li>
 * <li>during baselining and initialisation the builder and specialised in‑host
 * initialisers consult the execution configuration and the demographic/ad hoc
 * mapping logic to set per‑person and outbreak runtime priors.</li>
 * </ol>
 *
 * <h2>Extension and composition guidance</h2>
 * <p>
 * Prefer small, focused changes. The codebase uses a compositional pattern
 * where concrete builders such as
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} mix small default
 * interfaces that implement single concerns (network setup, outbreak baseliner,
 * person baseliner, in‑host initialisers). To extend the default behaviour
 * consider one of the following approaches:
 * <ul>
 * <li>subclass {@link io.github.ai4ci.flow.builders.DefaultModelBuilder} and
 * override one or two delegation methods (for example
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak});</li>
 * <li>implement an alternative demographic or network configuration and
 * register it via Jackson polymorphism so it can be selected from JSON;</li>
 * <li>create a custom {@code AbstractModelBuilder} or provide a factory that
 * returns a composed builder and wire that factory into
 * {@link io.github.ai4ci.flow.ExecutionBuilder}.</li>
 * </ul>
 *
 * <h2>Quick mapping table</h2>
 * <p>
 * The table below is a compact guide to where common configuration elements are
 * mapped into ABM types. Use the links to navigate the code.
 * </p>
 * <table>
 * <caption>Mapping of configuration elements to ABM fields</caption>
 * <tr>
 * <th>Config</th>
 * <th>Builder / method</th>
 * <th>ABM target</th>
 * </tr>
 * <tr>
 * <td>{@link io.github.ai4ci.config.setup.SetupConfiguration#getInitialImports}</td>
 * <td>{@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser#initialisePerson(io.github.ai4ci.abm.ImmutablePersonState.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}</td>
 * <td>{@link io.github.ai4ci.abm.ImmutablePersonState#getImportationExposure()}</td>
 * </tr>
 * <tr>
 * <td>{@link io.github.ai4ci.config.setup.SetupConfiguration#getNetwork}</td>
 * <td>{@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak(io.github.ai4ci.abm.ModifiableOutbreak, io.github.ai4ci.config.setup.SetupConfiguration, io.github.ai4ci.util.Sampler)}</td>
 * <td>Populated {@link io.github.ai4ci.abm.ModifiableOutbreak} social
 * graph</td>
 * </tr>
 * <tr>
 * <td>{@link io.github.ai4ci.config.setup.DemographicConfiguration}</td>
 * <td>{@link io.github.ai4ci.flow.builders.DefaultPersonBaseliner#baselinePerson(io.github.ai4ci.abm.ImmutablePersonBaseline.Builder, io.github.ai4ci.abm.Person, io.github.ai4ci.util.Sampler)}</td>
 * <td>{@link io.github.ai4ci.abm.ImmutablePersonBaseline} demographic
 * fields</td>
 * </tr>
 * </table>
 *
 * <p>
 * For more detail follow the links from the builder methods to the small
 * default interfaces in {@code io.github.ai4ci.abm.builders} which contain the
 * canonical mapping logic.
 *
 * @author Rob Challen
 */
package io.github.ai4ci.config.setup;
