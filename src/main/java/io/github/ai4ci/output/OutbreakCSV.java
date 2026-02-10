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
 * <p>Main purpose: provide a compact per‑time summary of outbreak metrics such
 * as incidence, prevalence and averages. Records are produced at the UPDATE
 * stage and are written per simulation state (one row per time step) to a CSV
 * file ({@code summary.csv}).
 *
 * <p>Downstream uses: used for plotting time series, quick diagnostics and as
 * an input to aggregate analyses across replications.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(stage = Stage.UPDATE, value = "summary.csv", size = 64, selector = OutbreakCSV.Selector.class, writer=CSVWriter.class)
public interface OutbreakCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toCSV(o.getCurrentState()));
		}
	}
	
	/** @return incident cases occurring during the current time step */
	long getIncidence();
	/** @return current prevalence proportion in the population */
	double getPrevalence();
	/** @return cumulative infections since simulation start */
	long getCumulativeInfections(); 
	/** @return cumulative deaths since simulation start */
	long getCumulativeDeaths();
	/** @return cumulative mobility reduction summed over agents */
	double getCumulativeMobilityDecrease();
	/** @return current number of hospitalised agents */
	long getHospitalisedCount(); 
	/** @return current number of infected agents */
	long getInfectedCount();
	/** @return current number of symptomatic agents */
	long getSymptomaticCount();
	/** @return average mobility across agents at this time step */
	double getAverageMobility();
	/** @return average viral load across agents at this time step */
	double getAverageViralLoad();
	/** @return average immune activity across agents at this time step */
	double getAverageImmuneActivity();
	/** @return average compliance across agents at this time step */
	double getAverageCompliance();
	/** @return test positives by result date for this time step */
	long getTestPositivesByResultDate();
	/** @return test negatives by result date for this time step */
	long getTestNegativesByResultDate();
	/** @return human readable trigger value associated with active policy */
	String getTriggerValue();
	/** @return numeric lockdown trigger threshold in use */
	double getLockdownTrigger();
	/** @return effective reproduction number (Rt) estimated for this time */
	double getRtEffective();
	/** @return active policy label for the current time step */
	String getPolicy();
	
}