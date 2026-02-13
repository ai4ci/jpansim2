package io.github.ai4ci.flow.builders;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutableSocialRelationship;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.SocialRelationship;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;
import it.unimi.dsi.fastutil.ints.IntIntImmutableSortedPair;

/**
 * Provides a setupOutbreak default function that can be mixed and matched with
 * other setup components to configure the setup process. The purpose of this
 * default setup is to build a social network based on the network configuration
 * options supplied. This is config driven by
 * {@link io.github.ai4ci.config.setup.NetworkConfiguration}. In the second
 * stage it determines the weight of the social network
 * {@link io.github.ai4ci.config.setup.DemographicConfiguration}
 *
 * @see DefaultModelBuilder
 * @see AbstractModelBuilder
 * @see io.github.ai4ci.config.setup.NetworkConfiguration
 * @see io.github.ai4ci.config.setup.DemographicConfiguration
 *
 */
public interface DefaultNetworkSetup {

	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(DefaultNetworkSetup.class);

	/**
	 * Default outbreak setup function that builds a social network based on the
	 * network configuration supplied. The network is built using JGraphT and
	 * then converted to a thread safe array of social relationships. The
	 * relationship strengths are determined by the demographic configuration.
	 * The resulting social network is set on the outbreak and the outbreak is
	 * returned for further processing by other setup functions.
	 *
	 * @param outbreak    the outbreak to set up
	 * @param setupConfig the setup configuration containing network and
	 *                    demographic settings
	 * @param sampler     a sampler for generating random numbers used in network
	 *                    construction and relationship strength assignment
	 * @return the outbreak with the social network set up
	 */
	@SuppressWarnings("unchecked")
	default ModifiableOutbreak setupOutbreak(
			ModifiableOutbreak outbreak, SetupConfiguration setupConfig,
			Sampler sampler
	) {

		SimpleGraph<Person, DefaultEdge> socialNetwork = new SimpleGraph<>(
				(Supplier<Person> & Serializable) () -> setupConfig
						.getDemographics().createPersonStub(outbreak),
				(Supplier<DefaultEdge> & Serializable) () -> new DefaultEdge(),
				false
		);

		log.debug("Initialising network config");

		// The type of graph build depends on the configuration supplied
		// E.g. may be Erdos-Reyni etc.
		setupConfig.getNetwork().generateGraph(socialNetwork);

		// This sets the weight of the network.
		// socialNetwork.edgeSet().forEach(r -> socialNetwork.setEdgeWeight(r,
		// sampler.uniform()));
		// This is configuration determined by the DemographicConfiguration
		// E.g. AgeStratified
		int size = (int) socialNetwork.iterables().edgeCount();

		ThreadSafeArray<SocialRelationship> relationships = new ThreadSafeArray<>(
				SocialRelationship.class, size
		);
		socialNetwork.edgeSet().parallelStream().forEach(e -> {
			Person source = socialNetwork.getEdgeSource(e);
			Person target = socialNetwork.getEdgeTarget(e);
			relationships.put(
					ImmutableSocialRelationship.builder().setRelationshipStrength(
							setupConfig.getDemographics()
									.getRelationshipStrength(source, target, sampler)
					).setPeopleIds(
							IntIntImmutableSortedPair
									.of(source.getId(), target.getId())
					).build()
			);
		});

		outbreak.getPeople().finish();
		relationships.finish();

		outbreak.setSocialNetwork(relationships);
		log.debug(
				"contact graph {} edges, {} average degree ",
				outbreak.getSocialNetwork().size(),
				(outbreak.getSocialNetwork().size() * 2.0)
						/ outbreak.getPopulationSize()
		);
		return outbreak;
	}

}
