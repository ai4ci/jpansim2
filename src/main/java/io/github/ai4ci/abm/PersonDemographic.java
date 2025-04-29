package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
public interface PersonDemographic extends Serializable {
	
	public static PersonDemographic DEFAULT = ImmutablePersonDemographic.builder().build();

	@Value.Default default double getAge() {return Double.NaN;}
	
	
	
}
