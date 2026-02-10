package io.github.ai4ci.config.setup;

import org.immutables.value.Value;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Person;

/**
 * Erdos‑Reyni (G(n,p)) network configuration.
 *
 * <p>Provides parameters used to generate a random graph under the G(n,p)
 * model. The graph generator is used during the {@code setupOutbreak}
 * stage by the model builder to populate the social network graph.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableErdosReyniConfiguration.class)
@JsonDeserialize(as = ImmutableErdosReyniConfiguration.class)
public interface ErdosReyniConfiguration extends NetworkConfiguration {

    /**
	 * Default Erdos‑Reyni configuration used in examples and tests.
	 *
	 * <p>The default supplies a simple parameterisation of the G(n,p) model
	 * with 16,384 nodes and an average degree of 100. This is a compact
	 * baseline for examples and tests that require a non‑trivial network.
	 */
	public static ImmutableErdosReyniConfiguration DEFAULT = ImmutableErdosReyniConfiguration.builder()
            .setNetworkSize(128*128)
            .setNetworkDegree(100)
            .build();
    
    /**
     * Populate the supplied social network graph using the stored parameters.
     *
     * @param socialNetwork a mutable JGraphT graph to populate
     */
    default void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork) {
        GnpRandomGraphGenerator<Person, DefaultEdge> gen = 
                new GnpRandomGraphGenerator<Person, DefaultEdge>(
                        this.getNetworkSize(),
                        ((double) this.getNetworkDegree()+1.0)/this.getNetworkSize()
                );
        gen.generateGraph(socialNetwork);
    }
}