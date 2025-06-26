package io.github.ai4ci.config.setup;

import org.immutables.value.Value;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Person;

@Value.Immutable
@JsonSerialize(as = ImmutableBarabasiAlbertConfiguration.class)
@JsonDeserialize(as = ImmutableBarabasiAlbertConfiguration.class)
public interface BarabasiAlbertConfiguration extends NetworkConfiguration {

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
