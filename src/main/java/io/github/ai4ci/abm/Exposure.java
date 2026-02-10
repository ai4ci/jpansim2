package io.github.ai4ci.abm;

import org.immutables.value.Value;

/**
 * Represents a successful transmission event from an infectious individual
 * (exposer) to another individual (exposee). This immutable value object
 * captures the details of such an exposure, including the amount of virus
 * transmitted and the identifier of the exposer. It is typically stored within
 * the {@link PersonHistory} of the exposee.
 */
@Value.Immutable
public interface Exposure {

	/**
	 * Returns the amount of virus or exposure dose successfully transmitted from
	 * the exposer to the exposee. This value quantifies the intensity of the
	 * exposure event.
	 *
	 * @return The exposure dose received by the exposee.
	 */
	double getExposure();

//	/**
//	 * This is already accounted for in the presence of the transmission event
//	 * and it is not clear if we need to remember it.
//	 * @return
//	 */
//	double getTransmissionProbability();

	/**
	 * Returns the unique identifier of the individual who transmitted the virus
	 * (the exposer). This ID can be used to retrieve the exposer's
	 * {@link PersonHistory} at a given time.
	 *
	 * @return The ID of the exposer.
	 */
	int getExposerId();

	/**
	 * Retrieves the {@link PersonHistory} of the exposer at the current time of the
	 * exposee's temporal state. This method provides access to the historical data
	 * of the individual who caused the exposure.
	 *
	 * @param one The {@link PersonTemporalState} of the exposee, providing context
	 *            for the current time.
	 * @return The {@link PersonHistory} of the exposer at the specified time.
	 * @throws java.util.NoSuchElementException if the exposer's history cannot be
	 *                                          found for the given ID and time.
	 */
	default PersonHistory getExposer(PersonTemporalState one) {
		int time = one.getTime();
		return one.getEntity().getOutbreak().getPersonHistoryByIdAndTime(getExposerId(), time).get();
	}
}