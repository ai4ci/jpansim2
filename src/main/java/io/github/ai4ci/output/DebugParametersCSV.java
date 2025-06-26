package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.CSVWriter;

@Value.Immutable
@Export(stage = Stage.START, value = "debug.csv", size = 64, selector = DebugParametersCSV.Selector.class, writer=CSVWriter.class)
public interface DebugParametersCSV extends CommonCSV.Execution {
	
	static class Selector implements Export.Selector {
		@Override
		public Stream<DebugParametersCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toCSV(o, o.getBaseline()));
		}
	}
	
	double getViralLoadTransmissibilityParameter();
	double getSeveritySymptomsCutoff();
	double getSeverityHospitalisationCutoff();
	double getSeverityDeathCutoff();
	int getInfectiveDuration();
	double getAverageContactDegree();
	double getPercolationThreshold();
}
