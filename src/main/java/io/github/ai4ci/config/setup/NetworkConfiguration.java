package io.github.ai4ci.config.setup;

import java.io.Serializable;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.abm.Person;

/**
 * Abstract configuration for network generation used during model setup.
 *
 * <p>Concrete implementations provide parameters for specific graph
 * generation algorithms (for example Erdos‑Reyni, Watts‑Strogatz and
 * Barabasi‑Albert). The configuration objects are consumed by the model
 * builder during the {@code setupOutbreak} stage to create the social
 * network used by the simulation.
 *
 * <p>Extension guidance: to add a new network type implement a new
 * configuration class and add it to the Jackson polymorphic mapping above
 * (or register it with your object mapper). Keep generation logic in the
 * configuration's {@link #generateGraph} method so the builder can remain
 * generic.</p>
 *
 * @author Rob Challen
 */
@JsonTypeInfo(use = Id.SIMPLE_NAME)
@JsonSubTypes( {
    @Type(value = ImmutableWattsStrogatzConfiguration.class, name="watts-strogatz"), 
    @Type(value = ImmutableErdosReyniConfiguration.class, name="erdos-reyni"),
    @Type(value = ImmutableBarabasiAlbertConfiguration.class, name="barabasi-albert")
} )
public interface NetworkConfiguration extends Serializable {

    /**
     * Model parameter: number of agents to generate in the network.
     *
     * @return the number of agents in the model
     */
    Integer getNetworkSize();
    /**
     * Model parameter: target average degree for the generated network.
     *
     * @return the average number of social network contacts
     */
    Integer getNetworkDegree();
    
    /**
     * Generate a social network graph according to the concrete
     * configuration parameters.
     *
     * <p>Builders pass an empty {@link SimpleGraph} and expect the method to
     * populate it with nodes and edges. Implementations should add nodes and
     * edges deterministically from the configuration values so results can
     * be reproduced when the same seed is used.
     *
     * @param socialNetwork mutable graph to populate with nodes and edges
     */
    @JsonIgnore
    void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork);
    
    
}