package io.github.ai4ci.output;

import static io.github.ai4ci.util.CSVUtil.csvFrom;

import org.immutables.value.Value;

@Value.Immutable
public interface OutbreakCSV extends CommonCSV {

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
	
	default String header() {
		return CommonCSV.super.header()+",incidence,cumulativeInfections,infectedCount,"+
			"symptomaticCount,averageMobility,averageViralLoad,averageCompliance,testPositives,testNegatives,presumedTestPositivePrevalence,rtEffective,policy";
	}
	
	default String row() {
		return CommonCSV.super.row()+","+csvFrom(
			this.getIncidence(),
			this.getCumulativeInfections(),
			this.getInfectedCount(),
			this.getSymptomaticCount(),
			this.getAverageMobility(),
			this.getAverageViralLoad(),
			this.getTestPositives(),
			this.getTestNegatives(),
			this.getPresumedTestPositivePrevalence(),
			this.getRtEffective(),
			this.getPolicy()
		);
	}
}
