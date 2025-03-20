package io.github.ai4ci.abm;

import org.immutables.value.Value;

@Value.Immutable
public interface Contact {

	double getProximityDuration();
	// double getTransmissionRisk();
	boolean isDetected();
	boolean isTransmitted();
	PersonHistoryReference getParticipant();
	
	default PersonHistory getParticipant(Outbreak outbreak) {
		return getParticipant().getHistory(outbreak).get();
	}
	
	
	
	/**
	 * This makes sure that the person has reached the 
	 * viral load threshold to be considered infectious. 
	 * The dose of virus a contact receives is reduced in low impact contacts,
	 * and in situations where masks are worn. 
	 * @return
	 */
	default double getExposure() {
		if (getParticipant().getNormalisedViralLoad() < 1) return 0;
		if (!isTransmitted()) return 0;
		double exposure = getProximityDuration() * getParticipant().getNormalisedViralLoad();
		return exposure;
	}
	
	public default int hash() {
		return getParticipant().hashCode();
	}
}
