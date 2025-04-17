package io.github.ai4ci.output;

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
}
