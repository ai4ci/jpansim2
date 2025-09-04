package io.github.ai4ci.config.geography;

import org.immutables.value.Value;

import io.github.ai4ci.Import;

@Value.Immutable
@Import("msoa.csv")
public interface MSOA {

	@Import.Id String getId();
	String getName();
	
}
