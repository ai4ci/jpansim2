package io.github.ai4ci.abm;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/** Generated mapping from current state to history entries. This copies 
 * information that is needed for any stateful inspection of the model such as
 * changes in infection state. 
 */
@Mapper(
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class HistoryMapper {
	
	// N.B. must be in same directory as data otherwise generated code
	// does not import properly
	
	public static HistoryMapper MAPPER = Mappers.getMapper( HistoryMapper.class );
	
	@Mapping( target = "todaysContacts", expression = "java(new Contact[0])")
	@Mapping( target = "todaysExposures", expression = "java(new Exposure[0])")
	@Mapping( target = "todaysTests", expression = "java(new java.util.ArrayList<>())")
	public abstract PersonHistory createHistory(PersonState currentState);
	
	
	public abstract OutbreakHistory createHistory(OutbreakState currentState);
	
//	public Integer personStateId( PersonState source ) {
//		return source.getEntity().getId();
//	};
	
	
}