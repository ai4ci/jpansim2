/**
 * In-host models for simulating viral infection dynamics within individuals.
 *
 * <p>
 * The classes in this package implement different mathematical approaches to
 * model the progression of viral infections and immune responses within a
 * single host. These models simulate the discrete-time evolution of key
 * compartments including virions, target cells (susceptible, exposed, infected,
 * removed), and immune cells (dormant, priming, active).
 *
 * <h2>Core Model Types</h2>
 * <ul>
 * <li>{@link io.github.ai4ci.abm.inhost.InHostStochasticState} - A stochastic
 * difference equation model that captures demographic and interaction noise
 * using binomial and Poisson sampling</li>
 * <li>{@link io.github.ai4ci.abm.inhost.InHostMarkovState} - A Markov chain
 * model for simulating discrete state transitions in viral dynamics</li>
 * <li>{@link io.github.ai4ci.abm.inhost.InHostPhenomenologicalState} - A
 * phenomenological model that focuses on observed infection patterns rather
 * than mechanistic details</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Convert continuous-time rate parameters to discrete-time probabilites
 * using p = 1 - e^(-r) for consistent simulation</li>
 * <li>Support external inputs such as new virion exposure and immunization
 * events</li>
 * <li>Track multiple infection waves and immune memory effects</li>
 * <li>Interface with agent-based models through shared state definitions</li>
 * </ul>
 *
 * <h2>Usage Context</h2>
 * <p>
 * These in-host models are typically used within the broader agent-based
 * simulation framework to represent infection progression at the individual
 * level, informing transmission dynamics between agents in the population-level
 * model.
 */
package io.github.ai4ci.abm.inhost;