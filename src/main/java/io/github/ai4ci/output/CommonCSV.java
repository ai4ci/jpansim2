package io.github.ai4ci.output;

import io.github.ai4ci.flow.CSVWriter;

public interface CommonCSV  {

	public interface Model extends CSVWriter.Writeable {
		String getModelName();
		int getModelReplica();
		String getExperimentName();
	}
	
	public interface Execution extends Model {
		int getExperimentReplica();
	}
	
	public interface State extends Execution {
		int getTime();
	}
	
}
