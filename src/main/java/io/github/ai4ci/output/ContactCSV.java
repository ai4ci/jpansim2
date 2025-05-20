package io.github.ai4ci.output;


import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.UPDATE,value = "contacts.csv",size = 64*64, selector = ContactCSV.Selector.class)
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
	
	
//	default String header() {
//		return CommonCSV.super.header()+",id,contactId,detected";
//	}
//	
//	default String row() {
//		return CommonCSV.super.row()+","+csvFrom(
//			this.getId(),
//			this.getContactId(),
//			this.isDetected()
//		);
//	}
}
