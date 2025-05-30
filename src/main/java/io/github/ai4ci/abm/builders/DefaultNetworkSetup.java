package io.github.ai4ci.abm.builders;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.SocialRelationship;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.util.Sampler;

public interface DefaultNetworkSetup {

	static Logger log = LoggerFactory.getLogger(DefaultNetworkSetup.class);
	
	
	@SuppressWarnings("unchecked")
	default ModifiableOutbreak setupOutbreak(ModifiableOutbreak outbreak, WattsStrogatzConfiguration setupConfig, Sampler sampler) {
		
		SimpleWeightedGraph<Person, SocialRelationship> socialNetwork = 
				new SimpleWeightedGraph<Person, SocialRelationship>(
						(Supplier<Person> & Serializable) () -> Person.createPersonStub(outbreak), 
						(Supplier<SocialRelationship> & Serializable) () -> new SocialRelationship()
				);
		
		outbreak.setSocialNetwork(socialNetwork);
	
		log.debug("Initialising network config");
		
		WattsStrogatzGraphGenerator<Person, SocialRelationship> gen = 
				new WattsStrogatzGraphGenerator<Person, SocialRelationship>(
						setupConfig.getNetworkSize(),
						Math.min(
							setupConfig.getNetworkConnectedness(),
							setupConfig.getNetworkSize()
						) / 2 * 2,
						setupConfig.getNetworkRandomness()
				);
		
		gen.generateGraph(socialNetwork);
		log.debug("contact graph {} edges, {} average degree ", 
			socialNetwork.iterables().edgeCount(),
			Calibration.getConnectedness(outbreak)
		);
		
		// This sets the weight of the network. 
		socialNetwork.edgeSet().forEach(r -> socialNetwork.setEdgeWeight(r, sampler.uniform()));
		
		return outbreak;
	}
	
	
}
