package io.github.ai4ci.output;

import org.immutables.value.Value;

@Value.Immutable
public interface PersonCSV extends CommonCSV {

	int getPersonId();
	String getBehaviour();
	boolean isInfectious(); 
	boolean isSymptomatic();
	double getNormalisedSeverity();
	double getNormalisedViralLoad();
	double getContactExposure();
	double getPresumedLocalPrevalence();
	double getProbabilityInfectiousToday();
	
}
