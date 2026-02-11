package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Per‑state outbreak summary exported during simulation updates.
 *
 * <p>
 * Main purpose: provide a compact per‑time summary of outbreak metrics such as
 * incidence, prevalence and averages. Records are produced at the UPDATE stage
 * and are written per simulation state (one row per time step) to a CSV file
 * ({@code summary.csv}).
 *
 * <p>
 * Downstream uses: used for plotting time series, quick diagnostics and as an
 * input to aggregate analyses across replications.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(
		stage = Stage.UPDATE,
		value = "summary.csv",
		size = 64,
		selector = OutbreakCSV.Selector.class,
		writer = CSVWriter.class
)
public interface OutbreakCSV extends CommonCSV.State {

	/**
	 * Selector to extract OutbreakCSV records from the outbreak state during the
	 * UPDATE stage. This is used by the Export mechanism to determine which
	 * records to write to the CSV file.
	 */
	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toCSV(o.getCurrentState()));
		}
	}

	/**
	 * Average compliance across agents at this time step.
	 *
	 * @return average compliance across agents at this time step
	 */
	double getAverageCompliance();

	/**
	 * Immune activity is a composite measure of the immune response of agents
	 *
	 * @return average immune activity across agents at this time step
	 */
	double getAverageImmuneActivity();

	/**
	 * Mobility is a composite measure of the movement and contact behaviour of
	 * agents
	 *
	 * @return average mobility across agents at this time step
	 */
	double getAverageMobility();

	/**
	 * In host viral load average.
	 *
	 * @return average viral load across agents at this time step
	 */
	double getAverageViralLoad();

	/**
	 * Cumulative deaths since simulation start.
	 *
	 * @return cumulative deaths since simulation start
	 */
	long getCumulativeDeaths();

	/**
	 * Cumulative infections since simulation start.
	 *
	 * @return cumulative infections since simulation start
	 */
	long getCumulativeInfections();

	/**
	 * Reduction in mobility compared to baseline, summed across agents.
	 *
	 * @return cumulative mobility reduction summed over agents
	 */
	double getCumulativeMobilityDecrease();

	/**
	 * Currently hospitalised.
	 *
	 * @return current number of hospitalised agents
	 */
	long getHospitalisedCount();

	/**
	 * Incidence cases occurring during the current time step.
	 *
	 * @return incident cases occurring during the current time step
	 */
	long getIncidence();

	/**
	 * Currently infected (not observed)
	 *
	 * @return current number of infected agents
	 */
	long getInfectedCount();

	/**
	 * The value of the trigger metric used to determine whether to enter
	 * lockdown, if the active policy is a lockdown policy.
	 *
	 * @return numeric lockdown trigger threshold in use
	 */
	double getLockdownTrigger();

	/**
	 * Current active policy model state.
	 *
	 * @return active policy label for the current time step
	 */
	String getPolicy();

	/**
	 * True prevalence proportion in the population (not observed)
	 *
	 * @return current prevalence proportion in the population
	 */
	double getPrevalence();

	/**
	 * True effective reproduction number (Rt) estimated for this time step (not
	 * observed)
	 *
	 * @return effective reproduction number (Rt) estimated for this time
	 */
	double getRtEffective();

	/**
	 * True symptomatic (not observed)
	 *
	 * @return current number of symptomatic agents
	 */
	long getSymptomaticCount();

	/**
	 * Count of negatives by result date.
	 *
	 * @return test negatives by result date for this time step
	 */
	long getTestNegativesByResultDate();

	/**
	 * Count of positives by result date.
	 *
	 * @return test positives by result date for this time step
	 */
	long getTestPositivesByResultDate();

	/**
	 * The description of the trigger value used to determine whether to enter
	 * lockdown, if the active policy is a lockdown policy. This is a human
	 * readable description of the trigger value, such as "Incidence per 100k" or
	 * "Hospitalisations per 100k".
	 *
	 * @return human readable trigger value associated with active policy
	 */
	String getTriggerValue();

}