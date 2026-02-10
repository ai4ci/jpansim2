package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
/**
 * A contact between two people.
 * 
 * A contact is always associated with a {@link PersonHistory} which tells you
 * when it happened. There are a lot of contacts recorded so this is optimised
 * for small storage. and contains the bare minimum. Participants in the contact
 * are resolved by id.
 *
 * @see Exposure
 */
public interface Contact extends Serializable {

	/**
	 * Whether the contact was detected by a smart agent. This is used to determine
	 * whether the contact should be included in contact tracing analyses and
	 * whether it should be included in contact network statistics. The contact may
	 * have been detected by a smart agent if either participant is a smart agent
	 * and the contact was detected by that agent. Note that a contact may be
	 * detected by one agent but not the other, so this is not necessarily
	 * symmetric.
	 *
	 * @return Was a contact detected by a smart agent
	 */
	boolean isDetected();

	/**
	 * Get the unique identifier of the first participant in the contact.
	 *
	 * @return the person id of the first participant; corresponds to the {@code id}
	 *         field of the source person record
	 */
	int getParticipant1Id();

	/**
	 * Get the unique identifier of the second participant in the contact.
	 *
	 * @return the person id of the second participant; corresponds to the
	 *         {@code id} field of the contacted person record
	 */
	int getParticipant2Id();

//	/**
//	 * The strength of the contact is at the moment defined as the probability
//	 * of the contact occurring.
//	 */
//	double getProximityDuration();

	/**
	 * Get the other participant in a contact
	 *
	 * @param id the person id of one participant in the contact
	 * @return the person id of the other participant in the contact
	 */
	default int getParticipant(int id) {
		if (id == getParticipant1Id()) {
			return getParticipant2Id();
		}
		if (id == getParticipant2Id()) {
			return getParticipant1Id();
		}
		throw new RuntimeException("Not a participant");
	}

	/**
	 * Get the most recent history of the other participant in a contact
	 *
	 * @param one the history of one participant in the contact, used to resolve the
	 *            other participant and the time of the contact
	 * @return the most recent history of the other participant in the contact, as
	 *         of the time of the contact
	 */
	default PersonHistory getParticipant(PersonHistory one) {
		var id = getParticipant(one.getEntity().getId());
		int time = one.getTime();
		return one.getEntity().getOutbreak().getPersonHistoryByIdAndTime(id, time).get();
	}

	/**
	 * Get the most recent history of the other participant in a contact
	 *
	 * @param one the state of one participant in the contact, used to resolve the
	 *            other participant and the time of the contact
	 * @return the most recent history of the other participant in the contact, as
	 *         of the time of the contact. Note that this is not necessarily the
	 *         same as the current history of the other participant, since the
	 *         contact may have occurred in the past. The current history is
	 *         returned if the contact occurred at the current time step.
	 *
	 */
	default PersonHistory getParticipant(PersonState one) {
		var id = getParticipant(one.getEntity().getId());
		return one.getEntity().getOutbreak().getPersonById(id).flatMap(p -> p.getCurrentHistory()).get();
	}

	/**
	 * Get the current state of the other participant in a contact
	 *
	 * @param one the state of one participant in the contact, used to resolve the
	 *            other participant and the time of the contact
	 * @return the current state of the other participant in the contact. Note that
	 *         this is not necessarily the same as the state of the other
	 *         participant at the time of the contact, since the contact may have
	 *         occurred in the past. The state at the time of the contact is
	 *         returned if the contact occurred at the current time step.
	 *
	 */
	default PersonState getParticipantState(PersonTemporalState one) {
		var id = getParticipant(one.getEntity().getId());
		return one.getEntity().getOutbreak().getPersonById(id).map(p -> p.getCurrentState()).get();
	};

}
