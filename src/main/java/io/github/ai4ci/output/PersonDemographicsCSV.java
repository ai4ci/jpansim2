package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.DuckDBWriter;

@Value.Immutable
@Export(stage = Stage.START,value = "demog.duckdb",size = 16, selector = PersonDemographicsCSV.Selector.class, writer=DuckDBWriter.class)
public interface PersonDemographicsCSV extends CommonCSV.Execution {
	
	static class Selector implements Export.Selector {
		@Override
		public Stream<PersonDemographicsCSV> apply(Outbreak o) {
			return o.getPeople().stream().map(CSVMapper.INSTANCE::toDemog);
		}
	}
	
	int getId();
	double getAge();
	double getMobilityBaseline();
	double getAppUseProbability();
	double getComplianceBaseline();
	double getLocationX();
	double getLocationY();
	
	// double getExpectedContactDegree();
	

}
