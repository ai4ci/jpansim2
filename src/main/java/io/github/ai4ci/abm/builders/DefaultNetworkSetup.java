package io.github.ai4ci.abm.builders;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.ImmutableSocialRelationship;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.SocialRelationship;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;
import it.unimi.dsi.fastutil.ints.IntIntImmutableSortedPair;

public interface DefaultNetworkSetup {

	static Logger log = LoggerFactory.getLogger(DefaultNetworkSetup.class);
	
	@SuppressWarnings("unchecked")
	default ModifiableOutbreak setupOutbreak(ModifiableOutbreak outbreak, SetupConfiguration setupConfig, Sampler sampler) {
		
		SimpleGraph<Person, DefaultEdge> socialNetwork = 
				new SimpleGraph<Person, DefaultEdge>(
						(Supplier<Person> & Serializable) () -> setupConfig.getDemographics().createPersonStub(outbreak), 
						(Supplier<DefaultEdge> & Serializable) () -> new DefaultEdge(),
						false
				);
		
		log.debug("Initialising network config");
		
		setupConfig.getNetwork().generateGraph(socialNetwork);
		
		
		// This sets the weight of the network. 
		// socialNetwork.edgeSet().forEach(r -> socialNetwork.setEdgeWeight(r, sampler.uniform()));
		int size = (int) socialNetwork.iterables().edgeCount();
		
		ThreadSafeArray<SocialRelationship> relationships = new ThreadSafeArray<SocialRelationship>(SocialRelationship.class, size);
		socialNetwork.edgeSet().parallelStream()
			.forEach(e -> {
				Person source = socialNetwork.getEdgeSource(e);
				Person target = socialNetwork.getEdgeTarget(e);
				relationships.put(
						ImmutableSocialRelationship.builder()
							.setRelationshipStrength(setupConfig.getDemographics().getRelationshipStrength(source,target,sampler))
							.setPeopleIds(IntIntImmutableSortedPair.of(
									source.getId(),
									target.getId()
							))
							.build()
				);
			});
		
		outbreak.getPeople().finish();
		relationships.finish();
		
		outbreak.setSocialNetwork(relationships);
		log.debug("contact graph {} edges, {} average degree ", 
				outbreak.getSocialNetwork().size(),
				((double) outbreak.getSocialNetwork().size()) / outbreak.getPopulationSize()
			);
		return outbreak;
	}
	
	
	
}
