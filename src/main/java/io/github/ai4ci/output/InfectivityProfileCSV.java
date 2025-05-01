package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

import io.github.ai4ci.flow.CSVWriter;

@Value.Immutable
public interface InfectivityProfileCSV extends CSVWriter.Writeable {

	String getExperiment();
	int getTau();
	double getProbability();
	
	default String header() {
		return "experiment,tau,probability";
	}
	
	default String row() {
		return csvFrom(
			this.getExperiment(),
			this.getTau(),
			this.getProbability()
		);
	}
}
