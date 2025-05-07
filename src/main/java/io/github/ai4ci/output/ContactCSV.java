package io.github.ai4ci.output;


import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;

@Value.Immutable
@Export(stage = Stage.UPDATE,value = "contacts.csv",size = 64*64)
public interface ContactCSV extends CommonCSV.State {

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
