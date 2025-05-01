package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

@Value.Immutable
public interface PersonDemographicsCSV extends CommonCSV {
	
	int getId();
	double getAge();
	Double getMobilityBaseline();
	Double getAppUseProbability();
	Double getComplianceBaseline();
	
	default String header() {
		return CommonCSV.super.header()+"id,age,mobilityBaseline,appUseProbability,complianceBaseline";
	}
	
	default String row() {
		return CommonCSV.super.row()+","+csvFrom(
			this.getId(),
			this.getAge(),
			this.getMobilityBaseline(),
			this.getComplianceBaseline(),
			this.getAppUseProbability(),
			this.getComplianceBaseline()
		);
	}
}
