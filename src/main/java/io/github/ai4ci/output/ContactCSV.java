package io.github.ai4ci.output;


import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.DuckDBWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Contact tracing records exported during simulation updates.
 *
 * <p>Main purpose: represent individual contact events detected or inferred
 * during an update step of the simulation. Records are produced in the
 * UPDATE stage and are exported per agent to a DuckDB database file
 * ({@code contacts.duckdb}).
 *
 * <p>Downstream uses: consumed by contact tracing analyses, contact network
 * statistics and any downstream exporters that require individual contact
 * events for reconstruction or privacy‑preserving analysis.
 *
 * @author Rob Challen
 */
@Value.Immutable

@Export(stage = Stage.UPDATE,value = "contacts.duckdb",size = 64*64, selector = ContactCSV.Selector.class, writer=DuckDBWriter.class)
public interface ContactCSV extends CommonCSV.State {
	
	/**
	 * Selector class implements Export.Selector to provide contact data extraction
	 * from Outbreak simulations. Transforms person data into contact records
	 * using CSVMapper for CSV serialization.
	 */
	static class Selector implements Export.Selector {
		@Override
		public Stream<ContactCSV> apply(Outbreak o) {
			return o.getPeople().stream().flatMap(CSVMapper.INSTANCE::toContacts);
		}
	}

	/**
	 * Get the unique identifier of the person initiating the contact.
	 *
	 * @return the person id who initiated the contact; corresponds to the
	 * {@code id} field of the source person record
	 */
	int getId();
	/**
	 * Get the unique identifier of the contacted person.
	 *
	 * @return the person id on the receiving end of the contact event
	 */
	int getContactId();
	/**
	 * Whether the contact was detected through contact tracing.
	 *
	 * @return true if the contact was detected, false if it was not detected or
	 * was a simulated/latent contact
	 */
	boolean isDetected();
	
}