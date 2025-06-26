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

@JsonTypeInfo(use = Id.SIMPLE_NAME)
@JsonSubTypes( {
	@Type(value = ImmutableWattsStrogatzConfiguration.class, name="watts-strogatz"), 
	@Type(value = ImmutableErdosReyniConfiguration.class, name="erdos-reyni"),
	@Type(value = ImmutableBarabasiAlbertConfiguration.class, name="barabasi-albert")
} )
public interface NetworkConfiguration extends Serializable {

	Integer getNetworkSize();
	Integer getNetworkDegree();
	
	@JsonIgnore
	void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork);
	
	
}
