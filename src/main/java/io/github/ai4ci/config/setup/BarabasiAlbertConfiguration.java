package io.github.ai4ci.config.setup;

import org.immutables.value.Value;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Person;

/**
 * Barabasi‑Albert preferential attachment network configuration.
 *
 * <p>This configuration is used to generate scale‑free networks via the
 * Barabasi‑Albert algorithm. The concrete generator is invoked during the
 * {@code setupOutbreak} stage to populate the simulation's social graph.
 *
 * <p>Extension guidance: tune the degree parameter or replace the generator
 * with a custom implementation if a different degree distribution is
 * required.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableBarabasiAlbertConfiguration.class)
@JsonDeserialize(as = ImmutableBarabasiAlbertConfiguration.class)
public interface BarabasiAlbertConfiguration extends NetworkConfiguration {

    /**
	 * Default Barabasi‑Albert configuration used in examples and tests.
	 *
	 * <p>The default values provide a compact set of parameters used in
	 * examples and tests. Downstream callers that rely on this simple default
	 * include the default setup configuration and example experiments.
	 *
	 * @see io.github.ai4ci.config.setup.SetupConfiguration#DEFAULT
	 */
	ImmutableBarabasiAlbertConfiguration DEFAULT = ImmutableBarabasiAlbertConfiguration.builder()
            .setNetworkSize(128*128)
            .setNetworkDegree(100)
            .build();
    
    //Integer getMinimumDegree();
    // Integer getMParameter();

    // n is final size nodes
    // total edges = m0*(m0-1)/2 + m*( n - m0)
    // av degree (d) = total edges / n
    // d*n = m0^2/2 -m0/2 + m*n - m*m0
    // 0 = m0^2 - (1 + 2*m)*m0 + 2*(m-d)*n
    // m*(n - m0) = - m0^2/2 + m0/2 + d*n
    // m = (- m0^2/2 + m0/2 + d*n)/(n - m0)

    // minimum degree = m0-1

    /**
     * Populate the supplied social network graph using the stored parameters.
     *
     * @param socialNetwork mutable graph to populate
     */
    default void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork) {
        int n = this.getNetworkSize();
        int m = this.getNetworkDegree() / 2;
        BarabasiAlbertGraphGenerator<Person, DefaultEdge> gen = 
                new BarabasiAlbertGraphGenerator<Person, DefaultEdge>(
                        (int) m, (int) m, (int) n
                );
        gen.generateGraph(socialNetwork);
    }
}