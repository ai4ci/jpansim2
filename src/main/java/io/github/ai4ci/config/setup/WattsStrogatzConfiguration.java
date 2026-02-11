package io.github.ai4ci.config.setup;

import org.immutables.value.Value;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Person;

/**
 * Watts‑Strogatz network configuration.
 *
 * <p>
 * This configuration encapsulates parameters used to generate a small‑world
 * social network using the JGraphT {@code WattsStrogatzGraphGenerator}. It is
 * consumed by network construction code in the model builder during the setup
 * stage and is not used directly during simulation runtime.
 *
 * <p>
 * Downstream uses: passed to the builder's
 * {@link io.github.ai4ci.flow.builders.DefaultModelBuilder#setupOutbreak} which
 * invokes {@link #generateGraph(SimpleGraph)} to populate the social network
 * graph used by the simulation.
 *
 * <p>
 * Extension guidance: to alter the seeding or node placement strategy replace
 * or extend the {@link io.github.ai4ci.flow.builders.DefaultNetworkSetup}
 * implementation rather than changing this configuration class.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableWattsStrogatzConfiguration.class)
@JsonDeserialize(as = ImmutableWattsStrogatzConfiguration.class)
public interface WattsStrogatzConfiguration extends NetworkConfiguration {

	ImmutableWattsStrogatzConfiguration DEFAULT = ImmutableWattsStrogatzConfiguration
			.builder().setNetworkSize(128 * 128).setNetworkDegree(100)
			.setNetworkRandomness(0.15).build();

	/**
	 * Populate the supplied social network graph using the stored parameters.
	 *
	 * @param socialNetwork an empty JGraphT SimpleGraph to populate with nodes
	 *                      and edges
	 */
	@Override
	default void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork) {
		WattsStrogatzGraphGenerator<Person, DefaultEdge> gen = new WattsStrogatzGraphGenerator<>(
				this.getNetworkSize(),
				Math.min(this.getNetworkDegree(), this.getNetworkSize()) / 2 * 2,
				this.getNetworkRandomness()
		);
		gen.generateGraph(socialNetwork);
	}

	/**
	 * Model parameter specific to Watts‑Strogatz.
	 *
	 * @return the probability of re‑wiring a social network edge
	 */
	Double getNetworkRandomness();
}