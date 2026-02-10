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

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.Contact;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.OutbreakBaseline;
import io.github.ai4ci.abm.OutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonBaseline;
import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.functions.DelayDistribution;
import io.github.ai4ci.util.Binomial;

@Mapper(
		builder = @Builder(buildMethod = "build"),
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, 
		imports = {Calibration.class}
)
public abstract class CSVMapper {
 
	public static CSVMapper INSTANCE = Mappers.getMapper( CSVMapper.class );
	
	@Mapping(target= "personId", source="entity.id")
	@Mapping(target="logOddsInfectiousToday", source="riskModel.logOddsInfectiousToday")
	public abstract ImmutableLineListCSV toCSV(PersonState state);
	
	public abstract ImmutableOutbreakCSV toCSV(OutbreakState state);
	
	public double fromBinom(Binomial binom) {
		return binom.probability();
	}
	
	@Mapping(target = "observationTime", source="entity.currentState.time")
	public abstract ImmutableOutbreakHistoryCSV toCSV(OutbreakHistory history);
	
	public abstract ImmutableOutbreakFinalStateCSV toFinalCSV(OutbreakState currentState);
	
	@Mapping(target = "averageContactDegree", expression = "java(Calibration.averageContactDegree(outbreak))")
	@Mapping(target = "percolationThreshold", expression = "java(Calibration.percolationThreshold(outbreak))")
	public abstract ImmutableDebugParametersCSV toCSV(Outbreak outbreak, OutbreakBaseline baseline);
	
	public Stream<InfectivityProfileCSV> infectivityProfile(Outbreak outbreak) {
		DelayDistribution dd = outbreak.getBaseline().getInfectivityProfile();
		return IntStream
			.range(0,  (int) dd.size())
			.mapToObj(i -> toIP(outbreak, i, dd.density(i)));
	};
	
	protected abstract ImmutableInfectivityProfileCSV toIP(Outbreak outbreak, int tau, double probability);
	
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
	
	//protected abstract HashMap<String,Object> toMap(ImmutableContactCSV csv);
	
	public abstract ImmutableOutbreakConfigurationJson toJson(Outbreak outbreak);
	
	public Stream<OutbreakBehaviourCountCSV> toBehaviourCSV(Outbreak outbreak) {
		var cs = outbreak.getCurrentState();
		return cs.getBehaviourCounts().entrySet().stream().map(kv -> toBehaviourCSV(cs, kv.getKey(), kv.getValue()));
	}
	
	public abstract ImmutableOutbreakBehaviourCountCSV toBehaviourCSV(OutbreakState state, String behaviour, Long count);

	public Stream<OutbreakContactCountCSV> toContactCSV(Outbreak t) {
		var cs = t.getCurrentState();
		return cs.getContactCounts().entrySet().stream().map(kv -> toContactCSV(cs, kv.getKey(), kv.getValue()));
	}
	
	public abstract ImmutableOutbreakContactCountCSV toContactCSV(OutbreakState state, Long contacts, Long count);

	@Mapping(target="time", source="ph.time") 
	@Mapping(target="id", source="ph.entity.id")
	@Mapping(target="type", source="t.testParams.testName")
	@Mapping(target="sampleTime", source="t.time")
	public abstract ImmutablePersonTestsCSV toCSV(TestResult t, PersonHistory ph);
}
