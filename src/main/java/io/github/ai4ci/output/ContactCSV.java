package io.github.ai4ci.output;

import org.immutables.value.Value;

@Value.Immutable
public interface ContactCSV extends CommonCSV {

	int getId();
	int getContactId();
	boolean isDetected();
	
}
