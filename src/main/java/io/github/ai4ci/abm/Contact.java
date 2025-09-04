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
	
	
	
	

}
