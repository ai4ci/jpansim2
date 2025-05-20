package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.Optional;

import org.immutables.value.Value;
import org.jgrapht.graph.SimpleWeightedGraph;

import io.github.ai4ci.abm.mechanics.PersonStateContacts;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface Contact extends Serializable {

	/**
	 * The joint transmissibility of both participants defines the intensity of
	 * the contact. This in turn modifies the dose of the exposure. This is in 
	 * the context of a known contact, which is known to also be a transmission
	 * event.  
	 * @return
	 */
	
	boolean isDetected();
	int getParticipant1Id();
	int getParticipant2Id();
	
//	/**
//	 * The strength of the contact is at the moment defined as the probability 
//	 * of the contact occuring.
//	 * @return
//	 */
//	double getProximityDuration();
	
	default int getParticipant(int id) {
		if (id == getParticipant1Id()) return getParticipant2Id(); 
		if (id == getParticipant2Id()) return getParticipant1Id();
		throw new RuntimeException("Not a participant");
	}
	
	default PersonHistory getParticipant(PersonHistory one) {
		int id = getParticipant(one.getEntity().getId());
		int time = one.getTime();
		return one.getEntity().getOutbreak().getPersonHistoryByIdAndTime(id,time)
				.get();
	}
	
	default PersonHistory getParticipant(PersonState one) {
		int id = getParticipant(one.getEntity().getId());
		return one.getEntity().getOutbreak().getPersonById(id).flatMap(p -> p.getCurrentHistory()).get();
	}
	
	public static PersonStateContacts contactNetwork(Outbreak outbreak) {
		//Do the contact network here? and pass it as a parameter to the
		//person updateState
		SimpleWeightedGraph<Person, SocialRelationship> network = outbreak.getSocialNetwork();
		PersonStateContacts out = new PersonStateContacts(
				network.vertexSet().size(),
				network.vertexSet().stream()
					.mapToInt(v -> network.outDegreeOf(v))
					.max().getAsInt()
				);
		
		network.edgeSet().parallelStream().forEach(r -> {
			Sampler sampler = Sampler.getSampler();
			PersonState one = network.getEdgeSource(r).getCurrentState();
			PersonState two = network.getEdgeTarget(r).getCurrentState();
			
			// TODO: contacts stratified by venue such as work or school  
			// connectedness quantile is a proxy for the context of a contact
			// If we wanted to control this a different set of features of the 
			// relationship could be used. At the moment this overloads mobility
			// with type of contact, but in reality WORK contacts may be less
			// Significant that home contacts. This is where we would implement
			// something along these lines.
			
			double contactProbability = r.contactProbability(
					one.getAdjustedMobility(),
					two.getAdjustedMobility());
			
			if (sampler.bern(contactProbability)) {
				// This is a contact. 
				// Contact transmission probability depends on lowest transmissibility
				
				Integer oneref = one.getEntity().getId();
				Integer tworef = two.getEntity().getId();
				
				double jointDetect = 
						one.getAdjustedAppUseProbability()*
						two.getAdjustedAppUseProbability()*
						outbreak.getCurrentState().getContactDetectedProbability();
				
				boolean detected = sampler.bern(jointDetect);
				
				Contact contact = ImmutableContact.builder()
					.setDetected(detected)
					.setParticipant1Id(oneref)
					.setParticipant2Id(tworef)
					// TODO: Proximity and duration of a contact aren't handled
					// .setProximityDuration(contactProbability)
					.build();
				
				out.write(oneref).put(contact);
				out.write(tworef).put(contact);
				
				asExposure(contact, one, two).ifPresent(e -> out.writeExp(oneref).put(e));
				asExposure(contact, two, one).ifPresent(e -> out.writeExp(tworef).put(e));
			}
		});
		
		return out;
		

	}
	
	public static Optional<Exposure> asExposure(Contact contact, PersonState ph, PersonState infector) {
		
		Sampler sampler = Sampler.getSampler();
		
		if (infector.getNormalisedViralLoad() == 0) return Optional.empty();
		
		// This is where transmission rate / probability plays a role.
		double trans =   
			Conversions.scaleProbabilityByOR(
				infector.getAdjustedTransmissibility(),
				ph.getSusceptibilityModifier()
			);
		
		boolean transmitted = sampler.bern(trans);
		if (transmitted == false) return Optional.empty();
		
		return Optional.of(
					ImmutableExposure.builder()
						.setExposerId(infector.getEntity().getId())
						// Should the amount of exposure be dependent on the 
						// probability of transmission or is this a stochastic 
						// event? My belief is the latter. If it happens, the
						// dose of virus is independent of how likely it was
						// to happen
						.setExposure(infector.getNormalisedViralLoad())
						//.setTransmissionProbability(trans)
						.build()
				);
	}
	

}
