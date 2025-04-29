package io.github.ai4ci.abm;

import java.io.Serializable;

import org.jgrapht.graph.DefaultWeightedEdge;

public class SocialRelationship extends DefaultWeightedEdge implements Serializable {
	/**
	 * The significance of the relationship. This is a probability / quantile
	 * that a contact will occur given 2 maximally mobile participants.
	 * A value near 1 here signifies a close relationship between two individuals.
	 * A value near zero is an improbable contact
	 * @return
	 */
	public double getRelationshipStrength() {
		return this.getWeight();
	}
	
	public double contactProbability(double adjMobility1, double adjMobility2) {
		return getRelationshipStrength()*adjMobility1*adjMobility2;
	}
	
	
}