package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.DuckDBWriter;

@Value.Immutable
@Export(stage = Stage.UPDATE, value = "linelist.duckdb",size = 64*64, selector = LineListCSV.Selector.class, writer=DuckDBWriter.class)
public interface LineListCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<LineListCSV> apply(Outbreak o) {
			return o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV);
		}
	}
	
	int getPersonId();
	String getBehaviour();
	boolean isInfectious(); 
	boolean isSymptomatic();
	boolean isRequiringHospitalisation();
	boolean isDead();
	boolean isIncidentHospitalisation();
	boolean isIncidentInfection();
	
	double getNormalisedSeverity();
	double getNormalisedViralLoad();
	double getImmuneActivity();
	double getContactExposure();
	double getPresumedLocalPrevalence();
	double getTrueLocalPrevalence();
	double getProbabilityInfectiousToday();
	double getLogOddsInfectiousToday();
	long getContactCount();
	long getExposureCount();
	
	double getAdjustedMobility();
	double getAdjustedTransmissibility();
	double getAdjustedCompliance();
	double getAdjustedAppUseProbability();
	
	
	
//	default String header() {
//		return CSVUtil.headers(this.getClass());
//	}
//	
//	default String row() {
//		return CommonCSV.super.row()+","+csvFrom(
//			this.getPersonId(),
//			this.getBehaviour(),
//			this.isInfectious(),
//			this.isSymptomatic(),
//			this.getNormalisedSeverity(),
//			this.getNormalisedViralLoad(),
//			this.getContactExposure(),
//			this.getPresumedLocalPrevalence(),
//			this.getProbabilityInfectiousToday(),
//			this.getContactCount(),
//			this.getExposureCount()
//		);
//	}
}
