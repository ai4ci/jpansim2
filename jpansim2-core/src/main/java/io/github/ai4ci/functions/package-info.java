/**
 * Package containing mathematical function implementations and utilities for
 * the AI4CI framework.
 *
 * <p>
 * <b>Current Unified Architecture:</b>
 * <ol>
 * <li><b>Core Function Interface:</b> {@code SimpleFunction} with basic
 * evaluation capability</li>
 * <li><b>Function Implementations:</b>
 * <ul>
 * <li>{@code EmpiricalFunction} - Piecewise interpolation from data points</li>
 * <li>{@code MathematicalFunction} - Mathematical expressions using
 * mxparser</li>
 * <li>{@code FixedValueFunction} - Constant value provider</li>
 * <li>{@code DelayDistribution} - Time-to-event probability distributions</li>
 * </ul>
 * </li>
 * <li><b>Distribution Hierarchy:</b> Complex probability distributions
 * extending statistical framework</li>
 * <li><b>Unified Interpolation System:</b></li>
 * </ol>
 *
 * <p>
 * <b>Improvement Opportunities:</b>
 * <ol>
 * <li><b>Function Composition:</b> Add support for building complex functions
 * from simpler ones</li>
 * <li><b>Enhanced Serialisation:</b> Improve JSON type resolution
 * consistency</li>
 * <li><b>Mathematical Operations:</b> Extend interface with common
 * transformations</li>
 * <li><b>Performance Optimisation:</b> Add caching and lazy evaluation
 * strategies</li>
 * </ol>
 *
 * <p>
 * <b>Specific TODOs for Enhancement (Post-Consolidation):</b>
 * <ol>
 * <li>Create {@code CompositeFunction} interface supporting: \[ f(x) = g(h(x))
 * \quad \text{and} \quad f(x) = a \cdot g(x) + b \]</li>
 * <li>Implement {@code FunctionRegistry} for dynamic function creation</li>
 * <li>Add caching decorator pattern for expensive function evaluations</li>
 * <li>Create validation framework with domain checking</li>
 * <li>Standardise mathematical documentation with LaTeX formulae</li>
 * </ol>
 *
 * @see SimpleFunction
 * @see Distribution
 * @see Interpolator
 * @see EmpiricalFunction
 * @see MathematicalFunction
 * @see FixedValueFunction
 * @see DelayDistribution
 */
package io.github.ai4ci.functions;