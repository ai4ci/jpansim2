package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.START,value = "demog.csv",size = 16, selector = PersonDemographicsCSV.Selector.class)
public interface PersonDemographicsCSV extends CommonCSV.Execution {
	
	static class Selector implements Export.Selector {
		@Override
		public Stream<PersonDemographicsCSV> apply(Outbreak o) {
			return o.getPeople().stream().map(CSVMapper.INSTANCE::toDemog);
		}
	}
	
	int getId();
	double getAge();
	Double getMobilityBaseline();
	Double getAppUseProbability();
	Double getComplianceBaseline();
	long getHilbertX();
	long getHilbertY();
	
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
