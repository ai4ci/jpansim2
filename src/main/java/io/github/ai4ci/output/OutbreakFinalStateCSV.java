package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.CSVWriter;

@Value.Immutable
@Export(stage = Stage.FINISH, value = "final-state.csv", size = 64, selector = OutbreakFinalStateCSV.Selector.class, writer=CSVWriter.class)
public interface OutbreakFinalStateCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakFinalStateCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toFinalCSV(o.getCurrentState()));
		}
	}
	
	long getCumulativeInfections();
	long getCumulativeAdmissions();
	long getCumulativeDeaths();
	double getMaximumPrevalence();
	long getMaximumIncidence();
	long getTimeToMaximumIncidence();
	long getMaximumHospitalBurden();
	
	double getCumulativeMobilityDecrease();
	double getCumulativeComplianceDecrease();
	
}
