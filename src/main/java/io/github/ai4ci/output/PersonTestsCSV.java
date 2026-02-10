package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.TestResult.Indication;
import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.flow.output.DuckDBWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Individual test result records exported during simulation updates.
 *
 * <p>Main purpose: represent individual test events and their results. Records
 * are produced during the UPDATE stage and are written per test event to a
 * DuckDB database ({@code cases.duckdb}).
 *
 * <p>Downstream uses: case series, delay analysis, test sensitivity/specificity
 * validation and contact tracing evaluation.
 *
 * @author Rob Challen
 */
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
	
	/**
	 * Time the sample was taken for the test.
	 *
	 * @return the sample time (model time step) when the test specimen was
	 * collected
	 */
	int getSampleTime();
	/**
	 * Time the result became available for the test.
	 *
	 * @return the result time (model time step) when the test result was
	 * recorded
	 */
	int getResultTime();
	/**
	 * Reason or indication for the test.
	 *
	 * @return the indication enum describing why the test was taken
	 */
	Indication getIndication();
	/**
	 * Unique identifier for the person who took the test.
	 *
	 * @return the person id associated with this test event
	 */
	int getId();
	/**
	 * Test type name.
	 *
	 * @return the test type identifier (for example PCR or LFT)
	 */
	String getType();
	/**
	 * Viral load measured in the sample (observed/sampled value).
	 *
	 * @return the viral load measured in the sample taken at {@link #getSampleTime()}
	 */
	double getViralLoadSample();
	/**
	 * The model's true viral load for the person at the sample time.
	 *
	 * @return the ground truth viral load used by the model's in‑host state
	 */
	double getViralLoadTruth();
	/**
	 * Final interpreted test result.
	 *
	 * @return the test result enum indicating positive/negative/indeterminate
	 */
	Result getFinalResult();
	
}