package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "test-positivity.csv",size = 64*64, selector = OutbreakHistoryCSV.Selector.class)
/**
 * A retrospective of test positives as determined on a given day. This view 
 * will demonstrate the right censoring of the test positivity and the difference
 * between time and observationTime will show the delay. This produces a row
 * for each combination of day in simulation and day of observation, so has 
 * multiple rows per simulation day. As such it is only really suitable for the
 * testing to show the delay distribution.
 */
public interface OutbreakHistoryCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakHistoryCSV> apply(Outbreak o) {
			return o.getHistory().stream().map(CSVMapper.INSTANCE::toCSV);
		}
	}
	
	int getObservationTime();
	long getCurrentTestPositivesBySampleDate();
	long getCurrentTestNegativesBySampleDate();
	
	//TODO: Testing reporting delay is only delay to result being available and not for data reporting delay 
	//TODO: There are no reporting delays for death or admission.
	//TODO: There is no time dependence in reporting delay at weekends
	
//	default String header() {
//		return CommonCSV.super.header()+",observationTime,currentTestPositivesBySampleDate,currentTestNegativesBySampleDate";
//	}
//	
//	default String row() {
//		return CommonCSV.super.row()+","+csvFrom(
//			this.getObservationTime(),
//			this.getCurrentTestPositivesBySampleDate(),
//			this.getCurrentTestNegativesBySampleDate()
//		);
//	}
}
