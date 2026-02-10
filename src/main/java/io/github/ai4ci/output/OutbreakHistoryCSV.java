package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "test-positivity.csv",size = 64*64, selector = OutbreakHistoryCSV.Selector.class, writer=CSVWriter.class)
/**
 * A retrospective of test positives as determined on a given day.
 *
 * <p>Main purpose: provide a time‑observation matrix showing test positivity
 * by sample date and result date to illustrate right censoring and reporting
 * delay. Produces multiple rows per simulation day and is therefore suitable
 * for testing delay analyses.
 *
 * <p>Downstream uses: analysis of testing delay distributions and reporting
 * completeness, used by test reporting and delay calibration routines.
 *
 * @author Rob Challen
 */
public interface OutbreakHistoryCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakHistoryCSV> apply(Outbreak o) {
			return o.getHistory().stream().map(CSVMapper.INSTANCE::toCSV);
		}
	}
	
	/**
	 * Observation time (result day) associated with this row.
	 *
	 * @return the observation or result time used to anchor the test positivity
	 * count; this is typically the day the test result became available
	 */
	int getObservationTime();
	/**
	 * Number of positive tests counted by sample date for the associated time.
	 *
	 * @return the count of test positives by sample date that are recorded for
	 * the observation time (may be right censored depending on delay)
	 */
	long getCurrentTestPositivesBySampleDate();
	/**
	 * Number of negative tests counted by sample date for the associated time.
	 *
	 * @return the count of test negatives by sample date that are recorded for
	 * the observation time
	 */
	long getCurrentTestNegativesBySampleDate();
	
	//TODO: Testing reporting delay is only delay to result being available and not for data reporting delay 
	//TODO: There are no reporting delays for death or admission.
	//TODO: There is no time dependence in reporting delay at weekends
	

}