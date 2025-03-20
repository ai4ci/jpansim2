package io.github.ai4ci.abm;

import java.util.Optional;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class HistoryMapper {
	 
	public static HistoryMapper MAPPER = Mappers.getMapper( HistoryMapper.class );
	
	@Mapping( target = "previous", expression = "java(currentHistory(currentState, 1))")
	@Mapping( target = "todaysContacts", expression = "java(new java.util.ArrayList<>())")
	@Mapping( target = "todaysTests", expression = "java(new java.util.ArrayList<>())")
	abstract PersonHistory createHistory(PersonState currentState);
	
	
	@Mapping( target = "previous", expression = "java(currentHistory(currentState, 1))")
	abstract OutbreakHistory createHistory(OutbreakState currentState);
	
	@Mapping( target = "personId", source = "entity.id")
	abstract PersonHistoryReference createReference(PersonState history);
	
	Integer personStateId( PersonState source ) {
		return source.getEntity().getId();
	};
	
	Optional<PersonHistory> currentHistory( PersonState currentState, int offset) {
		int time = currentState.getTime()-offset;
		return currentState.getEntity().getHistoryEntry( time );
	};
	
	
	Optional<OutbreakHistory> currentHistory( OutbreakState currentState, int offset) {
		int time = currentState.getTime()-offset;
		return currentState.getEntity().getHistoryEntry( time );
	};
	
//	@BeanMapping(mappingControl = DeepClone.class, resultType = ModifiablePerson.class)
//	@Mapping( target = "nextHistory", expression = "java(person.super().getNextHistory())")
//	@Mapping( target = "nextState", expression = "java(person.super().getNextState())")
//    public abstract Person clonePerson(Person person) ;
//	
//	@BeanMapping(mappingControl = DeepClone.class, resultType = ModifiableOutbreak.class)
//    public abstract Outbreak cloneOutbreak(Outbreak person) ;
//	
//	public StateMachine map(StateMachine input) {
//		return SerializationUtils.clone(input);
//	};
//	
//	public BehaviourState map(BehaviourState input) {
//		return input;
//	};
	
}