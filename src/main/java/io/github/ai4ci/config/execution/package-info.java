/**
 * Execution configuration types and helpers used during experiment runs.
 *
 * <p>
 * Main purpose: supply runtime parameters and policies that control how a
 * simulation executes. This package contains the core execution model
 * configuration, partial modifiers and small helper interfaces used by builders
 * and initialisers.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>representations of execution‑time parameters such as testing, behaviour
 * and policy model selection;</li>
 * <li>support for partial configurations and facets used by the
 * ExperimentConfiguration loader;</li>
 * <li>integration points with in‑host models via
 * {@link io.github.ai4ci.config.inhost.InHostConfiguration} and with the
 * simulation builders in {@link io.github.ai4ci.flow.builders}.</li>
 * </ul>
 *
 * <p>
 * Downstream uses: classes in this package are consumed by model builders (for
 * example {@link io.github.ai4ci.flow.builders.DefaultPersonInitialiser}), flow
 * builders such as {@link io.github.ai4ci.flow.ExecutionBuilder} and any
 * component that needs access to runtime policy and testing parameters.
 *
 * @author Rob Challen
 */
package io.github.ai4ci.config.execution;
