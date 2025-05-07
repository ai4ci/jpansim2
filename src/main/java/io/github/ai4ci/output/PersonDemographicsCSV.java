package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;

@Value.Immutable
@Export(stage = Stage.START,value = "demog.csv",size = 16)
public interface PersonDemographicsCSV extends CommonCSV.Execution {
	
	int getId();
	double getAge();
	Double getMobilityBaseline();
	Double getAppUseProbability();
	Double getComplianceBaseline();
	
//	default String header() {
//		return CommonCSV.super.header()+"id,age,mobilityBaseline,appUseProbability,complianceBaseline";
//	}
//	
//	default String row() {
//		return CommonCSV.super.row()+","+csvFrom(
//			this.getId(),
//			this.getAge(),
//			this.getMobilityBaseline(),
//			this.getComplianceBaseline(),
//			this.getAppUseProbability(),
//			this.getComplianceBaseline()
//		);
//	}
}
