package io.github.ai4ci.output;

import org.immutables.value.Value;

@Value.Immutable
public interface PersonDemographicsCSV extends CommonCSV {
	
	int getId();
	double getAge();
	Double getMobilityBaseline();
	Double getAppUseProbability();
	Double getComplianceBaseline();
	
}
