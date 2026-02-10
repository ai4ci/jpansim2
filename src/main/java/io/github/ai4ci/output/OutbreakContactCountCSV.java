package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Contact count distribution exported during simulation updates.
 *
 * <p>Main purpose: provide counts of contacts stratified by policy or bucket;
 * records are produced at the UPDATE stage and are aggregated per simulation
 * state and written to a CSV file ({@code contact-counts.csv}).
 *
 * <p>Downstream uses: useful for assessing contact distributions across
 * policies and for calibration diagnostics.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(stage = Stage.UPDATE,value = "contact-counts.csv",size = 16, selector=OutbreakContactCountCSV.Selector.class, writer=CSVWriter.class)
public interface OutbreakContactCountCSV extends CommonCSV.State {

	/**
	 * Policy or bucket associated with the contact counts.
	 *
	 * @return the policy label or contact bucket key
	 */
	public String getPolicy();
	/**
	 * Number of contacts associated with the bucket.
	 *
	 * @return the contacts value representing the contact degree bucket
	 */
	public Long getContacts();
	/**
	 * Frequency count of agents in the bucket.
	 *
	 * @return the number of agents observed with the given contact count
	 */
	public Long getCount();
	
	public class Selector implements Export.Selector {

		@Override
		public Stream<OutbreakContactCountCSV> apply(Outbreak t) {
			return CSVMapper.INSTANCE.toContactCSV(t);
		}

		
	}

}