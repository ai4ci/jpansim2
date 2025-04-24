package io.github.ai4ci.abm;


import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Person.Relationship;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.InHostConfiguration;
import io.github.ai4ci.util.DelayDistribution;

public class Calibration {

	static Logger log = LoggerFactory.getLogger(Calibration.class);
	
	public static double getConnectedness(Outbreak outbreak) {
		SimpleWeightedGraph<Person, Relationship> contacts = outbreak.getSocialNetwork();
		return stream(contacts.iterables().vertices())
				.mapToInt(
						c -> contacts.degreeOf(c)
				).average().getAsDouble();
	}
	
	private static <X> Stream<X> stream(Iterable<X> it) {
		return StreamSupport.stream(
				it.spliterator(),false
			);
	}
	
	public static double contactsPerDay(Outbreak outbreak) {
		// SimpleWeightedGraph<Person, Person.Relationship> social = outbreak.getSocialNetwork();
		ExecutionConfiguration configuration = outbreak.getExecutionConfiguration();
		// Distribution jointMob = configuration.getContactProbability();
				//.combine(
				//configuration.getContactProbability(), (d1,d2) -> d1*d2);
				//removed as we started to define baseline mobility as the sqrt 
				// of contact probability
		
		Distribution jointMob = configuration.getContactProbability().combine(
				configuration.getContactProbability(), (d1,d2) -> Math.sqrt(d1)*Math.sqrt(d2))
				.getInterpolation();
		double meanProbContact = jointMob.getCentral();
		double socialContacts = getConnectedness(outbreak);
		return meanProbContact * socialContacts; 
		
//		return stream(social.iterables().vertices())
//			.mapToDouble( pers ->
//				stream(social.iterables().edgesOf(pers))
//				// This is a probability that this contact is made on a 
//				// given time step based on expected mobility
//				// at the stage this calibration is needed the individuals
//				// haven't had their personal mobility assigned so we
//				// have to assume it is based on the overall mobility distribution
//				// which we can build into the definition of R0 - i.e. in a 
//				// homogeneous population
//				.mapToDouble(c -> 1-jointMob.pLessThan(c.getConnectednessQuantile()))
//				.sum() 
//			)
//			.average().orElse(0);
	}
	
//	private static double guesstimateR0(double transP, 
//			DelayDistribution omega, 
//			Distribution jointMob,
//			SimpleWeightedGraph<Person, Person.Relationship> social
//	) {
//		stream(social.iterables().vertices())
//			.mapToDouble(
//				pers -> {
//					double eContactsPerDay = stream(social.iterables().edgesOf(pers))
//					// This is a probability that this contact is made on a 
//					// given time step based on expected mobility
//					// at the stage this calibration is needed the individuals
//					// haven't had their personal mobility assigned so we
//					// have to assume it is based on the overall mobility distribution
//					// which we can build into the definition of R0 - i.e. in a 
//					// homogeneous population
//					.mapToDouble(c -> 1-jointMob.pLessThan(c.getConnectednessQuantile()))
//					// p here is the probability a contact is made in any given day
//					// for each social relationship. Sum of all these would be
//					// the expected number of contacts made each day. It the 
//					// freely mixing R0 situation the number of contacts per 
//					// infected person will scale linearly with duration of 
//					// infectiousness
//					.sum();
//					
//					// The probability of transmission over a typical infection
//					// depends on the viral load. The typical case is described
//					// in omega, but will be different for every 
//					// individual; The transmission probability is a linear fn
//					// of viral load, calibrated such that the total probability
//					// * contacts = R0.
//					
//					
//				}
//					
//					// The probability of a secondary infection per contact is
//					// just the probability of transmission per day for each of
//					// the infection is a function of the viral load and a 
//					// calibration constant. The infectivity profile
//					
//					// The probability of a secondary infections for a person is
//					// the probability of transmission per contact * expected 
//					// number of contacts which is to do
//					// at least one time step during the infectious period.
//					// each time step has a p probability of and the average 
//					// number of exposures defined by the infectivity profile.  
//					.map(p -> (1-Math.pow((1-p*transP), omega.expected()))) // omega.totalHazard(p*transP))
//					// sum of transmission probability for all the contacts is
//					// the potential R0 for this individual based on their local
//					// social network and their expected social mobility and the 
//					// per contact transmission probability.
//					.sum()
//			).average().getAsDouble();	
//	}
	
//	public static double guestimateR0(Outbreak outbreak, double transP) {
//		ExecutionConfiguration configuration = outbreak.getExecutionConfiguration();
//		Distribution jointMobility = configuration.getContactProbability().combine(
//				configuration.getContactProbability(), (d1,d2) -> d1*d2);
//		
//		DelayDistribution dd = outbreak.getExecutionConfiguration().getInfectivityProfile();
//		SimpleWeightedGraph<Person, Person.Relationship> social = outbreak.getSocialNetwork();
//		return guesstimateR0(transP, dd, jointMobility, social);
//	}
	
	public static double[] inferTransmissionProbability(Outbreak outbreak, double R0) {
		double fact = inferViralLoadTransmissionProbabilityFactor(outbreak, R0);
		DelayDistribution dd = outbreak.getExecutionConfiguration().getInfectivityProfile();
		return Arrays.stream(dd.getProfile()).map(d -> d*fact).toArray();
	}
	
	/**
	 * The translation betweeen R0 and the in host model viral load. The in 
	 * host viral load model has been run for a range of different parameters
	 * and the average response held in the infectivity profile. The total
	 * viral load over the whole infectious period is the sum of viral load on 
	 * each day. It is assumed that for R0 the population are making a fixed 
	 * number of contacts per day. This is determined by their social network
	 * structure and their mobility. The R0 value is the population average of 
	 * the sum of each individuals contacts per day multiplied by the 
	 * transmission probability per day. This is defined as a linear scale factor 
	 * of the viral load per day. 
	 * 
	 * This tends to produce outbreaks with larger effective reproduction numbers 
	 * than R0 in the simulation because (I think) of the number of exposures
	 * people have, means there is a difference between the R0 in a completely
	 * random population at the R0 in a network where repeated exposure is the
	 * norm. In this case I think the effective generation time will also be
	 * shorter. This comes down to the rate of repeated exposure.
	 * 
	 * @param outbreak
	 * @param R0
	 * @return
	 */
	public static double inferViralLoadTransmissionProbabilityFactor(Outbreak outbreak, double R0) {
		double c = contactsPerDay(outbreak);
		
		DelayDistribution dd = outbreak.getExecutionConfiguration().getInfectivityProfile();
		// The expected value here is what you would see in terms of
		// the average number of days over which the infection is distributed. This sets
		// an upper limit on the R0, given this contact network.
		if (c*dd.expected()<R0) throw new RuntimeException("Max R0 for this network is: "+c*dd.expected());
		// The delay distribution here is initialised from the viral load profile
		// and the total is the viral load total over the course of the in host
		// infection. This is where the difference between R0 in an infinite 
		// population
		double tmp =  R0 / (c * dd.total());
		log.debug("Effective contact network degree: "+c+"; P(transmission|contact with viral load = 1): "+tmp+"; for RO: "+R0);
		return tmp;
		
	}
	
//	public static double meanContactWeight(Outbreak outbreak, double medianContactProbability) {
//		SimpleWeightedGraph<Person, Person.Relationship> contacts = outbreak.getSocialNetwork();
//		long peopleCount = contacts.iterables().vertexCount();
//		double meanContactWeight = StreamSupport.stream(
//							contacts.iterables().edges().spliterator(),false
//						).mapToDouble(
//							c -> c.getConnectednessQuantile()*
//								medianContactProbability *
//								contacts.getEdgeSource(c).getBaseline().getMobilityBaseline()*
//								contacts.getEdgeTarget(c).getBaseline().getMobilityBaseline()
//						).sum() / peopleCount;
//		return meanContactWeight;
//	}
	
}
