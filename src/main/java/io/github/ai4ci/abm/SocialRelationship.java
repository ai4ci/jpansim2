package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import it.unimi.dsi.fastutil.ints.IntIntImmutableSortedPair;

/**
 * A weighted network edge.
 */
@Value.Immutable
public interface SocialRelationship extends Serializable {
	/**
	 * The significance of the relationship. This is a probability / quantile
	 * that a contact will occur given 2 maximally mobile participants.
	 * A value near 1 here signifies a close relationship between two individuals.
	 * A value near zero is an improbable contact
	 */
	double getRelationshipStrength();
	IntIntImmutableSortedPair getPeopleIds();
	
	public default double contactProbability(double adjMobility1, double adjMobility2) {
		return getRelationshipStrength()*adjMobility1*adjMobility2;
	}
	
	default public Person getSource(Outbreak outbreak) { return outbreak.getPersonById(this.getPeopleIds().firstInt()).orElseThrow(); }
	default public Person getTarget(Outbreak outbreak) { return outbreak.getPersonById(this.getPeopleIds().secondInt()).orElseThrow(); }
	
}