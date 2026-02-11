package io.github.ai4ci.config;

import io.github.ai4ci.flow.output.ExportSelector;
import io.github.ai4ci.output.ImmutableContactCSV;
import io.github.ai4ci.output.ImmutableDebugParametersCSV;
import io.github.ai4ci.output.ImmutableInfectivityProfileCSV;
import io.github.ai4ci.output.ImmutableLineListCSV;
import io.github.ai4ci.output.ImmutableOutbreakBehaviourCountCSV;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakContactCountCSV;
import io.github.ai4ci.output.ImmutableOutbreakFinalStateCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonDemographicsCSV;

/**
 * Enumeration of the different exporters available in the system, each
 * associated with an export selector.
 *
 * <p>
 * This enum provides a convenient way to reference and manage the various
 * exporters used for outputting different types of data during the simulation.
 * Each exporter is linked to a specific CSV format and export stage, as defined
 * by their respective selectors.
 *
 * @author Rob Challen
 */
public enum Exporters {

	/**
	 * Exporter for outbreak summary data, using the ImmutableOutbreakCSV format.
	 */
	SUMMARY(
			ExportSelector.of(ImmutableOutbreakCSV.class)
	),
	/** Exporter for line list data, using the ImmutableLineListCSV format. */
	LINELIST(
			ExportSelector.of(ImmutableLineListCSV.class)
	),
	/**
	 * Exporter for historical test data, using the ImmutableOutbreakHistoryCSV
	 * format.
	 */
	HISTORICAL_TESTS(
			ExportSelector.of(ImmutableOutbreakHistoryCSV.class)
	),
	/**
	 * Exporter for contact network data, using the ImmutableContactCSV format.
	 */
	CONTACT_NETWORK(
			ExportSelector.of(ImmutableContactCSV.class)
	),
	/**
	 * Exporter for infectivity profile data, using the
	 * ImmutableInfectivityProfileCSV format.
	 */
	INFECTIVITY_PROFILE(
			ExportSelector.of(ImmutableInfectivityProfileCSV.class)
	),
	/**
	 * Exporter for person demographic data, using the
	 * ImmutablePersonDemographicsCSV format.
	 */
	DEMOGRAPHICS(
			ExportSelector.of(ImmutablePersonDemographicsCSV.class)
	),
	/**
	 * Exporter for debug parameters, using the ImmutableDebugParametersCSV
	 * format.
	 */
	DEBUG_PARAMETERS(
			ExportSelector.of(ImmutableDebugParametersCSV.class)
	),
	/**
	 * Exporter for final state data, using the ImmutableOutbreakFinalStateCSV
	 * format.
	 */
	FINAL_STATE(
			ExportSelector.of(ImmutableOutbreakFinalStateCSV.class)
	),
	/**
	 * Exporter for behaviour count data, using the
	 * ImmutableOutbreakBehaviourCountCSV format.
	 */
	BEHAVIOUR(
			ExportSelector.of(ImmutableOutbreakBehaviourCountCSV.class)
	),
	/**
	 * Exporter for contact count data, using the
	 * ImmutableOutbreakContactCountCSV format.
	 */
	CONTACT_COUNTS(
			ExportSelector.of(ImmutableOutbreakContactCountCSV.class)
	);

	private ExportSelector<?> selector;

	private Exporters(ExportSelector<?> selector) {
		this.selector = selector;
	}

	/**
	 * Get the export selector associated with this exporter.
	 *
	 * @return the ExportSelector instance that defines how to extract and format
	 *         data for this exporter
	 */
	ExportSelector<?> getSelector() { return this.selector; }

}
