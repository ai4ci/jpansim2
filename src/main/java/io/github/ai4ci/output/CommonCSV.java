package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.*;

import io.github.ai4ci.flow.CSVWriter;

public interface CommonCSV extends CSVWriter.Writeable {

	String getModelName();
	int getModelReplica();
	String getExperimentName();
	int getExperimentReplica();
	int getTime();
	// String getUrn();
	
	
	default String header() {
		return "modelName,modelReplica,experimentName,experimentReplica,time";
	}
	
	default String row() {
		return csvFrom(
			this.getModelName(),
			this.getModelReplica(),
			this.getExperimentName(),
			this.getExperimentReplica(),
			this.getTime()
		);
	}
}
