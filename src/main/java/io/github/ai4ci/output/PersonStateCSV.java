package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

import io.github.ai4ci.util.CSVUtil;

@Value.Immutable
public interface PersonStateCSV extends CommonCSV {

	int getPersonId();
	String getBehaviour();
	boolean isInfectious(); 
	boolean isSymptomatic();
	double getNormalisedSeverity();
	double getNormalisedViralLoad();
	double getContactExposure();
	double getPresumedLocalPrevalence();
	double getProbabilityInfectiousToday();
	long getContactCount();
	long getExposureCount();
	
	default String header() {
		return CSVUtil.headers(this.getClass());
	}
	
	default String row() {
		return CommonCSV.super.row()+","+csvFrom(
			this.getPersonId(),
			this.getBehaviour(),
			this.isInfectious(),
			this.isSymptomatic(),
			this.getNormalisedSeverity(),
			this.getNormalisedViralLoad(),
			this.getContactExposure(),
			this.getPresumedLocalPrevalence(),
			this.getProbabilityInfectiousToday(),
			this.getContactCount(),
			this.getExposureCount()
		);
	}
}
