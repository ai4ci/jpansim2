package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

@Value.Immutable
public interface ContactCSV extends CommonCSV {

	int getId();
	int getContactId();
	boolean isDetected();
	
	
	default String header() {
		return CommonCSV.super.header()+",id,contactId,detected";
	}
	
	default String row() {
		return CommonCSV.super.row()+","+csvFrom(
			this.getId(),
			this.getContactId(),
			this.isDetected()
		);
	}
}
