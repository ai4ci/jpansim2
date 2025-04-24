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
	double getProximityDuration();
	
	default PersonHistory getParticipant(PersonHistory one) {
		int id = one.getEntity().getId() == getParticipant1Id() ? getParticipant1Id() : getParticipant2Id();
		int time = one.getTime();
		return one.getEntity().getOutbreak().getPersonHistoryByIdAndTime(id,time)
				.get();
	}
	
	default PersonHistory getParticipant(PersonState one) {
		int id = one.getEntity().getId() == getParticipant1Id() ? getParticipant1Id() : getParticipant2Id();
		return one.getEntity().getOutbreak().getPersonById(id).flatMap(p -> p.getCurrentHistory()).get();
	}
	
	public static PersonStateContacts contactNetwork(Outbreak outbreak) {
		//Do the contact network here? and pass it as a parameter to the
		//person updateState
		SimpleWeightedGraph<Person, Person.Relationship> network = outbreak.getSocialNetwork();
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
			// jointProb is a contact intensity. This can be used to 
			// estimate contact observation probability
			double jointMobility = 
					// Conversions.scaleProbability(
							// This will be the lowest common value 
							one.getAdjustedMobility()*two.getAdjustedMobility(); //,
							// This is just a way of introducing noise into
							// the probability.
							// sampler.gamma(1, 0.1)
					// );
			// TODO: connectedness quantile is a proxy for the context of a contact
			// If we wanted to control this a different set of fetaures of the 
			// relationship could be used. At the moment this overloads mobility
			// with type of contact, but in reality WORK contacts may be less
			// Significant that home contacts. This is where we would implement
			// something along these lines.
			// TODO: No randomness in here at the moment
			if (jointMobility > r.getConnectednessQuantile()) {
				// This is a contact. 
				// Contact transmission probability depends on lowest transmissibility
				
				Integer oneref = one.getEntity().getId();
				Integer tworef = two.getEntity().getId();
				
				double jointDetect = one.getContactDetectedProbability()*two.getContactDetectedProbability();
				boolean detected = sampler.bern(jointDetect);
				
				Contact contact = ImmutableContact.builder()
					.setDetected(detected)
					.setParticipant1Id(oneref)
					.setParticipant2Id(tworef)
					.setProximityDuration(jointMobility - r.getConnectednessQuantile())
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
