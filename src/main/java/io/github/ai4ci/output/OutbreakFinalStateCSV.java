package io.github.ai4ci.output;

import org.immutables.value.Value;

@Value.Immutable
public interface OutbreakFinalStateCSV extends CommonCSV {

	int getObservationTime();
	double getRtForward();
	
}
