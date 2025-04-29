package io.github.ai4ci.abm.builders;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutablePersonDemographic;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.SocialRelationship;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.EmpiricalFunction;
import io.github.ai4ci.util.Sampler;

public interface AgeStratifiedNetworkSetup {
	
	static Logger log = LoggerFactory.getLogger(AgeStratifiedNetworkSetup.class);

	private static ModifiablePerson createPersonStub(Outbreak outbreak, EmpiricalDistribution age) {
		ModifiablePerson tmp = Person.createPersonStub(outbreak);
		tmp.setDemographic(
			ImmutablePersonDemographic.builder()
				.setAge(age.sample())
				.build()
		);
		return tmp;
	}
	
	/**
	 * Compared to an average relationship strength of 1 how close is the 
	 * relationship between two people, expressed as an odds ratio. I.e.
	 * an OR of 2 means that the odds of the contact is doubled. The underlying
	 * probability of the contact is still random but this will change the 
	 * distribution depending on the contact individual.    
	 * @param one
	 * @param two
	 * @return
	 */
	private static double getRelativeSimilarity(Person one, Person two, EmpiricalFunction oddsContactFromAgeDifference) {
		double age1 = one.getDemographic().getAge();
		double age2 = two.getDemographic().getAge();
		double diff = Math.abs(age1-age2);
		return oddsContactFromAgeDifference.interpolate(diff);
//		if (diff < 5) return 1.5;
//		if (diff % 25 < 5) return 1.2;
//		return 0.75;
	}
	
	default void setupOutbreak(ModifiableOutbreak outbreak, AgeStratifiedNetworkConfiguration config, Sampler sampler) {
		
		// For starters lets keep this simple.
		// We generate a vanilla social network but age weight the edge strength
		
		@SuppressWarnings("unchecked")
		SimpleWeightedGraph<Person, SocialRelationship> socialNetwork = 
				new SimpleWeightedGraph<Person, SocialRelationship>(
						(Supplier<Person> & Serializable) () -> createPersonStub(outbreak, config.getAgeDistribution()), 
						(Supplier<SocialRelationship> & Serializable) () -> new SocialRelationship()
				);
		
		outbreak.setSocialNetwork(socialNetwork);
		
		log.debug("Initialising network config");
		
		WattsStrogatzGraphGenerator<Person, SocialRelationship> gen = 
				new WattsStrogatzGraphGenerator<Person, SocialRelationship>(
						config.getNetworkSize(),
						Math.min(
							config.getNetworkConnectedness(),
							config.getNetworkSize()
						) / 2 * 2,
						config.getNetworkRandomness()
				);
		
		gen.generateGraph(socialNetwork);
		
		// This sets the weight of the network. This is accessible as 
		// the "connectednessQuantile" of the relationship.
		socialNetwork.edgeSet().forEach(r -> {
			Person one = socialNetwork.getEdgeSource(r);
			Person two = socialNetwork.getEdgeTarget(r);
			socialNetwork.setEdgeWeight(r,
				// heavier weight means more likely to be selected regardless of mobility.
				Conversions.scaleProbabilityByOR(
					sampler.uniform(),
					getRelativeSimilarity(one,two, config.getOddsContactFromAgeDifference())
				)
			);
		});
	}
}
