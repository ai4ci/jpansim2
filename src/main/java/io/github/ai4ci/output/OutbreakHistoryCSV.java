package io.github.ai4ci.output;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "test-positivity.csv",size = 64*64)
/**
 * A retrospective of test positives as determined on a given day. This view 
 * will demonstrate the right censoring of the test positivity and the difference
 * between time and observationTime will show the delay.
 */
public interface OutbreakHistoryCSV extends CommonCSV.State {

	int getObservationTime();
	long getCurrentTestPositivesBySampleDate();
	long getCurrentTestNegativesBySampleDate();
	
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
