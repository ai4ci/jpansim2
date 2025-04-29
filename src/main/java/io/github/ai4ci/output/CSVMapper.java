package io.github.ai4ci.output;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.OutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonBaseline;
import io.github.ai4ci.abm.PersonDemographic;
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
	public abstract ImmutablePersonStateCSV toCSV(PersonState state);
	
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
	
	//@Mapping(target= "id", source="person.entity.id")
	@Mapping(target="experimentName",source = "person.currentState.experimentName")
	@Mapping(target="experimentReplica",source = "person.currentState.experimentReplica")
	@Mapping(target="modelName",source = "person.currentState.modelName")
	@Mapping(target="modelReplica",source = "person.currentState.modelReplica")
	@Mapping(target="time",source = "person.currentState.time")
	protected abstract ImmutablePersonDemographicsCSV toCSV(Person person, PersonDemographic demog, PersonBaseline baseline);
	 
	public ImmutablePersonDemographicsCSV toDemog(Person person) {
		return toCSV(person, person.getDemographic(), person.getBaseline());
	}
	
	@Mapping(target= "id", source = "contact.participant1Id")
	@Mapping(target= "contactId", source = "contact.participant2Id")
	protected abstract ImmutableContactCSV toCSV(PersonHistory person, Contact contact);
	
	public Stream<ImmutableContactCSV> toContacts(Person person) {
		return person.getCurrentHistory()
				.stream()
				.flatMap(ph -> 
					Arrays.stream(ph.getTodaysContacts())
						.filter(c -> c.getParticipant1Id() == person.getId())
						.map(c -> toCSV(ph,c))
				);
	}
}
