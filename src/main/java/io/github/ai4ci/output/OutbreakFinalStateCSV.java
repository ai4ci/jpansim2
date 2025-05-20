package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.FINISH, value = "final-state.csv", size = 64, selector = OutbreakFinalStateCSV.Selector.class)
public interface OutbreakFinalStateCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<OutbreakFinalStateCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toFinalCSV(o.getCurrentState()));
		}
	}
	
	long getCumulativeInfections();
	long getCumulativeAdmissions();
	long getCumulativeDeaths();
	double getMaximumPrevalence();
	long getMaximumIncidence();
	long getMaximumHospitalBurden();
	
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
