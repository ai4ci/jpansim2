package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;

@Value.Immutable
@Export(stage = Stage.UPDATE,value = "linelist.csv",size = 64*64, selector = PersonStateCSV.Selector.class)
public interface PersonStateCSV extends CommonCSV.State {

	static class Selector implements Export.Selector {
		@Override
		public Stream<PersonStateCSV> apply(Outbreak o) {
			return o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV);
		}
	}
	
	int getPersonId();
	String getBehaviour();
	boolean isInfectious(); 
	boolean isSymptomatic();
	boolean isRequiringHospitalisation();
	boolean isDead();
	double getNormalisedSeverity();
	double getNormalisedViralLoad();
	double getContactExposure();
	double getPresumedLocalPrevalence();
	double getTrueLocalPrevalence();
	double getProbabilityInfectiousToday();
	double getLogOddsInfectiousToday();
	long getContactCount();
	long getExposureCount();
	
	boolean isIncidentInfection();
	
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
