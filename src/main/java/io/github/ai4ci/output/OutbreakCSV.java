package io.github.ai4ci.output;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "summary.csv", size = 64)
public interface OutbreakCSV extends CommonCSV.State {

	long getIncidence();
	long getCumulativeInfections(); 
	long getInfectedCount();
	long getSymptomaticCount();
	double getAverageMobility();
	double getAverageViralLoad();
	double getAverageCompliance();
	long getTestPositives();
	long getTestNegatives();
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
