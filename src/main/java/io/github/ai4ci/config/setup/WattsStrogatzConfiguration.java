package io.github.ai4ci.config.setup;

import org.immutables.value.Value;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Person;

@Value.Immutable
@JsonSerialize(as = ImmutableWattsStrogatzConfiguration.class)
@JsonDeserialize(as = ImmutableWattsStrogatzConfiguration.class)
public interface WattsStrogatzConfiguration extends NetworkConfiguration {

	ImmutableWattsStrogatzConfiguration DEFAULT = ImmutableWattsStrogatzConfiguration.builder()
			.setNetworkSize(128*128)
			.setNetworkDegree(100)
			.setNetworkRandomness(0.15)
			.build();
	
	Double getNetworkRandomness();
	
	
	default void generateGraph(SimpleGraph<Person, DefaultEdge> socialNetwork) {
		WattsStrogatzGraphGenerator<Person, DefaultEdge> gen = 
				new WattsStrogatzGraphGenerator<Person, DefaultEdge>(
						this.getNetworkSize(),
						Math.min(
							this.getNetworkDegree(),
							this.getNetworkSize()
						) / 2 * 2,
						this.getNetworkRandomness()
				);
		gen.generateGraph(socialNetwork);
	}
}
