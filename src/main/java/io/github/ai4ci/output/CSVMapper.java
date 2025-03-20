package io.github.ai4ci.output;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import io.github.ai4ci.abm.OutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.PersonState;

@Mapper(
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class CSVMapper {
	
	public static CSVMapper INSTANCE = Mappers.getMapper( CSVMapper.class );
	
//	@Mapping(target= "personId", source="entity.id")
//	public abstract ImmutablePersonCSV toCSV(PersonHistory history);
	
	@Mapping(target= "personId", source="entity.id")
	public abstract ImmutablePersonCSV toCSV(PersonState history);
	
	public abstract ImmutableOutbreakCSV toCSV(OutbreakState history);
	//public abstract ImmutableOutbreakCSV toCSV(OutbreakHistory history);
	
	public ImmutablePersonCSV toCSV(Person person) {
		return toCSV(person.getCurrentState());
		// we could call instead a 2 input mapping function with Person and PersonHistory
		// if we need to flatten here, or use getEntity method in PersonHistory in a
		// @Mapping annotation.
	};
}
