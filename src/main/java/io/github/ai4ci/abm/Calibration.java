package io.github.ai4ci.abm;

import java.util.stream.StreamSupport;

import org.jgrapht.graph.SimpleWeightedGraph;

import io.github.ai4ci.abm.Person.Relationship;

public class Calibration {

	public static double getConnectedness(Outbreak outbreak) {
		SimpleWeightedGraph<Person, Relationship> contacts = outbreak.getSocialNetwork();
		return StreamSupport.stream(
				contacts.iterables().vertices().spliterator(),
				true
			).mapToInt(
				c -> contacts.degreeOf(c)
			).average().getAsDouble();
	}
	
	public static double meanContactWeight(Outbreak outbreak, double medianContactProbability) {
		SimpleWeightedGraph<Person, Person.Relationship> contacts = outbreak.getSocialNetwork();
		long peopleCount = contacts.iterables().vertexCount();
		double meanContactWeight = StreamSupport.stream(
							contacts.iterables().edges().spliterator(),false
						).mapToDouble(
							c -> c.getConnectednessQuantile()*
								medianContactProbability *
								contacts.getEdgeSource(c).getBaseline().getMobilityBaseline()*
								contacts.getEdgeTarget(c).getBaseline().getMobilityBaseline()
						).sum() / peopleCount;
		return meanContactWeight;
	}
	
}
