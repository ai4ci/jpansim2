package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Final outbreak summary exported at simulation finish.
 *
 * <p>Main purpose: provide end-of-run aggregated metrics such as cumulative
 * counts and maxima. Records are produced at the FINISH stage and are written
 * once per simulation execution to a CSV file ({@code final-state.csv}).
 *
 * <p>Downstream uses: used for final result tables, calibration targets and
 * quick comparison across simulation replications.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(stage = Stage.FINISH, value = "final-state.csv", size = 64, selector = OutbreakFinalStateCSV.Selector.class, writer=CSVWriter.class)
public interface OutbreakFinalStateCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakFinalStateCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toFinalCSV(o.getCurrentState()));
		}
	}
	
	/** @return cumulative infections by the end of simulation */
	long getCumulativeInfections();
	/** @return cumulative hospital admissions by the end of simulation */
	long getCumulativeAdmissions();
	/** @return cumulative deaths by the end of simulation */
	long getCumulativeDeaths();
	/** @return maximum prevalence observed during the simulation */
	double getMaximumPrevalence();
	/** @return maximum incidence observed during the simulation */
	long getMaximumIncidence();
	/** @return time step at which maximum incidence occurred */
	long getTimeToMaximumIncidence();
	/** @return maximum hospital burden observed during the simulation */
	long getMaximumHospitalBurden();
	
	/** @return cumulative mobility decrease summed over the simulation */
	double getCumulativeMobilityDecrease();
	/** @return cumulative compliance decrease summed over the simulation */
	double getCumulativeComplianceDecrease();
	
}