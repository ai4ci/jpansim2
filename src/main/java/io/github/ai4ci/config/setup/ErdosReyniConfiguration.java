package io.github.ai4ci.config.setup;

import org.immutables.value.Value;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Person;

@Value.Immutable
@JsonSerialize(as = ImmutableErdosReyniConfiguration.class)
@JsonDeserialize(as = ImmutableErdosReyniConfiguration.class)
public interface ErdosReyniConfiguration extends NetworkConfiguration {

	ImmutableErdosReyniConfiguration DEFAULT = ImmutableErdosReyniConfiguration.builder()
			.setNetworkSize(128*128)
			.setNetworkDegree(100)
			.build();
	
	default void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork) {
		GnpRandomGraphGenerator<Person, DefaultEdge> gen = 
				new GnpRandomGraphGenerator<Person, DefaultEdge>(
						this.getNetworkSize(),
						((double) this.getNetworkDegree())/this.getNetworkSize()
				);
		gen.generateGraph(socialNetwork);
	}
}
