package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.TestResult.Indication;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.flow.DuckDBWriter;

@Value.Immutable
@Export(stage = Stage.UPDATE,value = "cases.duckdb",size = 64*64, selector = PersonTestsCSV.Selector.class, writer=DuckDBWriter.class)
public interface PersonTestsCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<PersonTestsCSV> apply(Outbreak o) {
			return o.getPeople().stream()
					.flatMap(p -> p.getCurrentHistory().stream())
					.flatMap(ph -> 
						ph.getTodaysResults().stream()
							.map(
								test -> CSVMapper.INSTANCE.toCSV(test,ph) 
							)
					);
		}
	}
	
	int getSampleTime();
	int getResultTime();
	Indication getIndication();
	int getId();
	String getType();
	double getViralLoadSample();
	double getViralLoadTruth();
	Result getFinalResult();
	
}
