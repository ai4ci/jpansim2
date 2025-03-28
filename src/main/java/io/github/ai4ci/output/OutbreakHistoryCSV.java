package io.github.ai4ci.output;

import org.immutables.value.Value;

@Value.Immutable
public interface OutbreakHistoryCSV extends CommonCSV {

	int getObservationTime();
	long getCurrentTestPositivesBySampleDate();
	long getCurrentTestNegativesBySampleDate();
	
}
