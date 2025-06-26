package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.CSVWriter;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "summary.csv", size = 64, selector = OutbreakCSV.Selector.class, writer=CSVWriter.class)
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
	double getCumulativeMobilityDecrease();
	long getHospitalisedCount(); 
	long getInfectedCount();
	long getSymptomaticCount();
	double getAverageMobility();
	double getAverageViralLoad();
	double getAverageImmuneActivity();
	double getAverageCompliance();
	long getTestPositivesByResultDate();
	long getTestNegativesByResultDate();
	String getTriggerValue();
	double getLockdownTrigger();
	double getRtEffective();
	String getPolicy();
	
}
