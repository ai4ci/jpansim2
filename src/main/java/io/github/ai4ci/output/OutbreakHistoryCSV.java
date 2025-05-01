package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

@Value.Immutable
public interface OutbreakHistoryCSV extends CommonCSV {

	int getObservationTime();
	long getCurrentTestPositivesBySampleDate();
	long getCurrentTestNegativesBySampleDate();
	
	default String header() {
		return CommonCSV.super.header()+",observationTime,currentTestPositivesBySampleDate,currentTestNegativesBySampleDate";
	}
	
	default String row() {
		return CommonCSV.super.row()+","+csvFrom(
			this.getObservationTime(),
			this.getCurrentTestPositivesBySampleDate(),
			this.getCurrentTestNegativesBySampleDate()
		);
	}
}
