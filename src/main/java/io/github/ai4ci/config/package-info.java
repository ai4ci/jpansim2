/**
 * Provides configuration interfaces for epidemiological simulations and experiments.
 * 
 * <p>Contains:
 * <ul>
 *   <li>Core experiment configuration ({@link io.github.ai4ci.config.TestParameters}, {@link io.github.ai4ci.config.ExperimentConfiguration})</li>
 *   <li>Setup and parameterization classes {@link io.github.ai4ci.config.setup.SetupConfiguration}</li>
 *   <li>Execution control parameters {@link io.github.ai4ci.config.execution.ExecutionConfiguration}, {@link io.github.ai4ci.config.ExecutionFacet}</li>
 *   <li>Batch processing configurations {@link io.github.ai4ci.config.BatchConfiguration}</li>
 * </ul>
 *
 * <p>Key features:
 * <ul>
 *   <li>Immutable configuration objects using Immutables library</li>
 *   <li>JSON serialization/deserialization support</li>
 *   <li>Modular design with facets and modifications</li>
 * </ul>
 *
 * @author Rob Challen
 */
package io.github.ai4ci.config;