package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.*;

import io.github.ai4ci.flow.CSVWriter;

public interface CommonCSV  {

	public interface Model extends CSVWriter.Writeable{
		String getModelName();
		String getExperimentName();
		
	}
	
	public interface Execution extends Model {
		int getModelReplica();
		int getExperimentReplica();
	}
	
	public interface State extends Execution {
		int getTime();
	}
	
	
	// String getUrn();
	
	
//	default String header() {
//		return "modelName,modelReplica,experimentName,experimentReplica,time";
//	}
//	
//	default String row() {
//		return csvFrom(
//			this.getModelName(),
//			this.getModelReplica(),
//			this.getExperimentName(),
//			this.getExperimentReplica(),
//			this.getTime()
//		);
//	}
}
