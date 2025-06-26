package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.Optional;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.PersonStateContacts;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.ThreadSafeArray;

@Value.Immutable
/**
 * A contact between two people. A contact is always associated with a 
 * {@link PersonHistory} which tells you when it happened. There are a lot
 * of contacts recorded so this is optimised for small storage. and contains
 * the bare minimum. Participants in the contact are resolved by id.
 * 
 * @see Exposure
 */
public interface Contact extends Serializable {
	
	/** Was a contact detected by a smart agent */
	boolean isDetected();
	int getParticipant1Id();
	int getParticipant2Id();
	
//	/**
//	 * The strength of the contact is at the moment defined as the probability 
//	 * of the contact occurring.
//	 */
//	double getProximityDuration();
	
	/** Get the other participant in a contact */
	default int getParticipant(int id) {
		if (id == getParticipant1Id()) return getParticipant2Id(); 
		if (id == getParticipant2Id()) return getParticipant1Id();
		throw new RuntimeException("Not a participant");
	}
	
	/** Get the other participant in a contact */
	default PersonHistory getParticipant(PersonHistory one) {
		int id = getParticipant(one.getEntity().getId());
		int time = one.getTime();
		return one.getEntity().getOutbreak().getPersonHistoryByIdAndTime(id,time)
				.get();
	}
	
	/** Get the other participant in a contact */
	default PersonHistory getParticipant(PersonState one) {
		int id = getParticipant(one.getEntity().getId());
		return one.getEntity().getOutbreak().getPersonById(id).flatMap(p -> p.getCurrentHistory()).get();
	}
	
	/** Get the other participant in a contact */
	default PersonState getParticipantState(PersonTemporalState one) {
		int id = getParticipant(one.getEntity().getId());
		return one.getEntity().getOutbreak().getPersonById(id).map(p -> p.getCurrentState()).get();
	};
	
	public static PersonStateContacts contactNetwork(Outbreak outbreak) {
		//Do the contact network here? and pass it as a parameter to the
		//person updateState
		ThreadSafeArray<SocialRelationship> network = outbreak.getSocialNetwork();
		PersonStateContacts out = new PersonStateContacts(
				outbreak.getPeople().size(),
				network.size() / outbreak.getPeople().size() * 4
		);
		
		network.parallelStream().forEach(r -> {
			Sampler sampler = Sampler.getSampler();
			PersonState one = r.getSource(outbreak).getCurrentState();
			PersonState two = r.getTarget(outbreak).getCurrentState();
			
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
				
				out.write(oneref).put(tworef,contact);
				out.write(tworef).put(oneref,contact);
				
				asExposure(contact, one, two).ifPresent(e -> out.writeExp(oneref).put(tworef,e));
				asExposure(contact, two, one).ifPresent(e -> out.writeExp(tworef).put(oneref,e));
			}
		});
		
		return out;
		

	}
	
	/**
	 * Is a contact an exposure? This is a directional relationship so is called
	 * two times for each contact. 
	 */
	public static Optional<Exposure> asExposure(Contact contact, PersonState infectee, PersonState infector) {
		
		Sampler sampler = Sampler.getSampler();
		
		if (!infector.isInfectious()) return Optional.empty();
		
		// This is where transmission rate / susceptability plays a role.
		double trans =   
			Conversions.scaleProbabilityByOR(
				infector.getAdjustedTransmissibility(),
				infectee.getSusceptibilityModifier()
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
						// to happen. Probability of transmission depends on
						// whether contact coughs, dose depends on how much 
						// virus they cough over you.
						// See PersonState#getContactExposure for where this is
						// picked up and fed into the in host model.
						.setExposure(infector.getNormalisedViralLoad())
						//.setTransmissionProbability(trans)
						.build()
				);
	}
	
	

}
