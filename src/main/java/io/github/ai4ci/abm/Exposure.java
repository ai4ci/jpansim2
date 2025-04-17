package io.github.ai4ci.abm;

import org.immutables.value.Value;

@Value.Immutable
public interface Exposure {
	
	double getExposure();
	
//	/**
//	 * This is already accounted for in the presence of the transmission event
//	 * and it is not clear if we need to remember it.
//	 * @return
//	 */
//	double getTransmissionProbability();
	
	int getExposerId();
	
	default PersonHistory getExposer(PersonTemporalState one) {
		int time = one.getTime();
		return one.getEntity().getOutbreak().getPersonHistoryByIdAndTime(getExposerId(),time)
				.get();
	}
}
