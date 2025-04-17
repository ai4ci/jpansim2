package io.github.ai4ci.output;

import org.immutables.value.Value;

@Value.Immutable
public interface InfectivityProfileCSV {

	String getExperiment();
	int getTau();
	double getProbability();
	
}
