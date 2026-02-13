/**
 * Configurations for in‑host disease progression models used within persons.
 *
 * <p>
 * Main purpose: provide polymorphic configuration types that describe how an
 * individual's infection progresses while inside the host. Implementations
 * include phenomenological curves, stochastic compartmental approximations and
 * explicit Markov state models.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Jackson polymorphic bindings to select a concrete in‑host model at
 * deserialisation time;</li>
 * <li>convenience factory methods to construct infectivity and severity
 * profiles used by the execution configuration;</li>
 * <li>interfaces intended for use by ABM initialisers such as
 * {@link io.github.ai4ci.flow.builders.DefaultInHostPhenomenologicalStateInitialiser}.
 * </li>
 * </ul>
 *
 * <p>
 * Downstream uses: the
 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration} references an
 * {@code InHostConfiguration} which is consulted during person initialisation
 * to populate per‑person {@link io.github.ai4ci.abm.inhost.InHostModelState}.
 * Builders, samplers and calibration code use these configurations to derive
 * delay distributions and clinical severity profiles.
 *
 * @author Rob Challen
 */
package io.github.ai4ci.config.inhost;
