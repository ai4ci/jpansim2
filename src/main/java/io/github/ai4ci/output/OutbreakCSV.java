package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "summary.csv", size = 64, selector = OutbreakCSV.Selector.class)
public interface OutbreakCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toCSV(o.getCurrentState()));
		}
	}
	
	long getIncidence();
	double getPrevalence();
	long getCumulativeInfections(); 
	long getCumulativeDeaths();
	long getHospitalisedCount(); 
	long getInfectedCount();
	long getSymptomaticCount();
	double getAverageMobility();
	double getAverageViralLoad();
	double getAverageCompliance();
	long getTestPositivesByResultDate();
	long getTestNegativesByResultDate();
	double getPresumedTestPositivePrevalence();
	double getRtEffective();
	String getPolicy();
	
//	default String header() {
//		return CommonCSV.super.header()+",incidence,cumulativeInfections,infectedCount,"+
//			"symptomaticCount,averageMobility,averageViralLoad,averageCompliance,testPositives,testNegatives,presumedTestPositivePrevalence,rtEffective,policy";
//	}
//	
//	default String row() {
//		return CommonCSV.super.row()+","+csvFrom(
//			this.getIncidence(),
//			this.getCumulativeInfections(),
//			this.getInfectedCount(),
//			this.getSymptomaticCount(),
//			this.getAverageMobility(),
//			this.getAverageViralLoad(),
//			this.getTestPositives(),
//			this.getTestNegatives(),
//			this.getPresumedTestPositivePrevalence(),
//			this.getRtEffective(),
//			this.getPolicy()
//		);
//	}
}
