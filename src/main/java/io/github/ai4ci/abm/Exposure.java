package io.github.ai4ci.abm;

import org.immutables.value.Value;

@Value.Immutable
/**
 * This is an extension of a contact but involving an infectious individual
 * who successfully transmitted a dose of virus to an exposee. This is 
 * held in the {@link PersonHistory} of the exposee, which holds the time 
 * and other details of the exposure. 
 */
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
