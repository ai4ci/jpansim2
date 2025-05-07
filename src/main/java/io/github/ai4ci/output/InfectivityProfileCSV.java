package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.flow.CSVWriter;

@Value.Immutable
@Export(stage = Stage.BASELINE,value = "ip.csv",size = 16)
public interface InfectivityProfileCSV extends CommonCSV.Model {

	int getTau();
	double getProbability();
	
//	default String header() {
//		return "experiment,tau,probability";
//	}
//	
//	default String row() {
//		return csvFrom(
//			this.getExperiment(),
//			this.getTau(),
//			this.getProbability()
//		);
//	}
}
