//package io.github.ai4ci.abm;
//
//import org.immutables.value.Value;
//import static io.github.ai4ci.abm.HistoryMapper.MAPPER;
//import java.util.Optional;
//
//@Value.Immutable
//public interface PersonHistoryReference {
//
//	// Have to be careful not to include stuff in here that can change with 
//	// time as this is a copy at a single point in time.
//	
//	int getPersonId();
//	int getTime();
//	double getNormalisedViralLoad();
//	
//	// double getProbabilityInfectiousToday();
//	
//	default Optional<PersonHistory> getHistory(Outbreak outbreak) {
//		return outbreak.getPersonById(getPersonId())
//			.flatMap(p -> p.getHistoryEntry(getTime()));
//	}
//	
//	default Optional<PersonHistory> getCurrent(Outbreak outbreak) {
//		return outbreak.getPersonById(getPersonId())
//			.flatMap(p -> p.getCurrentHistory());
//	}
//	
//	public static PersonHistoryReference from(PersonState history) {
//		return MAPPER.createReference(history);
//	}
//	default boolean isInfectious() {
//		return this.getNormalisedViralLoad() > 1;
//	}
//	
//	
//	
//}
