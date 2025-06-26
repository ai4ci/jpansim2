package io.github.ai4ci.output;


import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.DuckDBWriter;

@Value.Immutable
@Export(stage = Stage.UPDATE,value = "contacts.duckdb",size = 64*64, selector = ContactCSV.Selector.class, writer=DuckDBWriter.class)
public interface ContactCSV extends CommonCSV.State {
	
	static class Selector implements Export.Selector {
		@Override
		public Stream<ContactCSV> apply(Outbreak o) {
			return o.getPeople().stream().flatMap(CSVMapper.INSTANCE::toContacts);
		}
	}

	int getId();
	int getContactId();
	boolean isDetected();
	
}
