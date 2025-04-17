package io.github.ai4ci.output;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import io.github.ai4ci.abm.OutbreakBaseline;
import io.github.ai4ci.abm.OutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.DelayDistribution;

@Mapper(
		builder = @Builder(buildMethod = "build"),
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class CSVMapper {
	
	public static CSVMapper INSTANCE = Mappers.getMapper( CSVMapper.class );
	
//	@Mapping(target= "personId", source="entity.id")
//	public abstract ImmutablePersonCSV toCSV(PersonHistory history);
	
	@Mapping(target= "personId", source="entity.id")
	public abstract ImmutablePersonCSV toCSV(PersonState state);
	
	public abstract ImmutableOutbreakCSV toCSV(OutbreakState state);
	
	@Mapping(target = "observationTime", source="entity.currentState.time")
	public abstract ImmutableOutbreakHistoryCSV toCSV(OutbreakHistory history);
	
//	@Mapping(target = "rtForward", ignore = true)
//	@Mapping(target = "observationTime", ignore = true)
//	public abstract ImmutableOutbreakFinalStateCSV.Builder commonCSV(@MappingTarget ImmutableOutbreakFinalStateCSV.Builder builder, OutbreakState state);
//	
//	public Stream<ImmutableOutbreakFinalStateCSV> finalCSV(OutbreakState state) {
//		return IntStream
//			.range(0,  state.getTime())
//			.mapToObj(i -> 
//				commonCSV(ImmutableOutbreakFinalStateCSV.builder(), state)
//					.setObservationTime(i)
////					.setRtForward(
////						state.getRtForward().get(i)
////					)
//					.build()
//			);
//	};
	
	public Stream<ImmutableInfectivityProfileCSV> infectivityProfile(ExecutionConfiguration outbreak) {
		DelayDistribution dd = outbreak.getInfectivityProfile();
		return IntStream
			.range(0,  (int) dd.size())
			.mapToObj(i -> 
			ImmutableInfectivityProfileCSV.builder()
					.setExperiment(outbreak.getName())
					.setTau(i)
					.setProbability(dd.density(i))
				.build()
			);
	};
	
	public ImmutablePersonCSV toCSV(Person person) {
		return toCSV(person.getCurrentState());
		// we could call instead a 2 input mapping function with Person and PersonHistory
		// if we need to flatten here, or use getEntity method in PersonHistory in a
		// @Mapping annotation.
	};
}
