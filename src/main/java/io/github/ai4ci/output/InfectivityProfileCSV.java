package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.BASELINE,value = "ip.csv",size = 16, selector=InfectivityProfileCSV.Selector.class)
public interface InfectivityProfileCSV extends CommonCSV.Model {

	static class Selector implements Export.Selector {
		@Override
		public Stream<InfectivityProfileCSV> apply(Outbreak o) {
			return CSVMapper.INSTANCE.infectivityProfile(o);
		}
	}
	
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
