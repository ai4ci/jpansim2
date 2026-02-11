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
import io.github.ai4ci.util.Binomial;

/**
 * Mapper responsible for converting domain model objects into CSV DTOs.
 *
 * <p>
 * This abstract MapStruct mapper centralises the logic used to produce
 * CSV-friendly immutable transfer objects for reporting and downstream
 * analysis. Typical inputs are outbreak, person and history objects and outputs
 * are immutable CSV types defined in this package (for example
 * {@code ImmutableOutbreakCSV}, {@code ImmutableLineListCSV}).
 * </p>
 *
 * <p>
 * Downstream uses: the CSV DTOs produced by this mapper are consumed by
 * reporting and analysis routines external to the core simulation. See the
 * {@code io.github.ai4ci.output} package for the DTO types and the
 * {@link io.github.ai4ci.abm.Outbreak} and {@link Person} classes which provide
 * the inputs to the mapper.
 * </p>
 *
 * @author Rob Challen
 */
@Mapper(
		builder = @Builder(buildMethod = "build"),
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		imports = { Calibration.class }
)
public abstract class CSVMapper {

	/**
	 * Shared mapper instance obtained from MapStruct. Use this to access the
	 * mapping functions from elsewhere in the codebase.
	 */
	public static CSVMapper INSTANCE = Mappers.getMapper(CSVMapper.class);

	/**
	 * Convert a project Binomial object to a probability for CSV output.
	 *
	 * @param binom the Binomial to convert
	 * @return the probability represented by the Binomial
	 */
	public double fromBinom(Binomial binom) {
		return binom.probability();
	}

	/**
	 * Produce an infectivity profile CSV stream for the supplied outbreak. Each
	 * element represents the probability of transmission at a given time lag
	 * tau.
	 *
	 * @param outbreak the outbreak from which to derive the profile
	 * @return stream of {@link InfectivityProfileCSV} records
	 */
	public Stream<InfectivityProfileCSV> infectivityProfile(Outbreak outbreak) {
		var dd = outbreak.getBaseline().getInfectivityProfile();
		return IntStream.range(0, (int) dd.size())
				.mapToObj(i -> this.toIP(outbreak, i, dd.density(i)));
	}

	/**
	 * Map outbreak behaviour counts to CSV DTOs.
	 *
	 * @param outbreak the outbreak state providing behaviour counts
	 * @return stream of behaviour count CSV records
	 */
	public Stream<OutbreakBehaviourCountCSV> toBehaviourCSV(Outbreak outbreak) {
		var cs = outbreak.getCurrentState();
		return cs.getBehaviourCounts().entrySet().stream()
				.map(kv -> this.toBehaviourCSV(cs, kv.getKey(), kv.getValue()));
	}

	/**
	 * Map outbreak behaviour counts to CSV DTOs.
	 *
	 * @param state     outbreak state providing behaviour counts
	 * @param behaviour the type of behaviour (e.g. "home", "work", "other")
	 * @param count     the count of agents exhibiting this behaviour in the
	 *                  outbreak state
	 * @return CSV DTO for the behaviour count
	 */
	public abstract ImmutableOutbreakBehaviourCountCSV toBehaviourCSV(
			OutbreakState state, String behaviour, Long count
	);

	/**
	 * Map outbreak contact counts to CSV DTOs.
	 *
	 * @param t outbreak instance containing contact counts
	 * @return stream of contact count CSV records
	 */
	public Stream<OutbreakContactCountCSV> toContactCSV(Outbreak t) {
		var cs = t.getCurrentState();
		return cs.getContactCounts().entrySet().stream()
				.map(kv -> this.toContactCSV(cs, kv.getKey(), kv.getValue()));
	}

	/**
	 * Map outbreak contact counts to CSV DTOs.
	 *
	 * @param state    outbreak state providing contact counts
	 * @param contacts the type of contacts (e.g. "home", "work", "other")
	 * @param count    the count of contacts of this type in the outbreak state
	 * @return CSV DTO for the contact count
	 */
	public abstract ImmutableOutbreakContactCountCSV toContactCSV(
			OutbreakState state, Long contacts, Long count
	);

	/**
	 * Convert a person's contacts (from their current history snapshot) into CSV
	 * contact records. Only contacts where this person is participant1 are
	 * exported.
	 *
	 * @param person the person whose contacts to export
	 * @return stream of contact CSV records
	 */
	public Stream<ImmutableContactCSV> toContacts(Person person) {
		return person.getCurrentHistory().stream().flatMap(
				ph -> Arrays.stream(ph.getTodaysContacts())
						.filter(c -> c.getParticipant1Id() == person.getId())
						.map(c -> this.toCSV(ph, c))
		);
	}

	/**
	 * Map debug parameters for an outbreak into a CSV DTO. The mapping includes
	 * calibrated values derived via {@link Calibration}.
	 *
	 * @param outbreak the outbreak
	 * @param baseline outbreak baseline parameters
	 * @return CSV DTO containing debug parameters
	 */
	@Mapping(
			target = "averageContactDegree",
			expression = "java(Calibration.averageContactDegree(outbreak))"
	)
	@Mapping(
			target = "percolationThreshold",
			expression = "java(Calibration.percolationThreshold(outbreak))"
	)
	public abstract ImmutableDebugParametersCSV toCSV(
			Outbreak outbreak, OutbreakBaseline baseline
	);

	/**
	 * Convert an outbreak history record to a CSV DTO. The observation time is
	 * taken from the entity current state time.
	 *
	 * @param history outbreak history record
	 * @return CSV DTO for the outbreak history record
	 */
	@Mapping(
			target = "observationTime",
			source = "entity.currentState.time"
	)
	public abstract ImmutableOutbreakHistoryCSV toCSV(OutbreakHistory history);

	/**
	 * Convert an outbreak state into a CSV DTO representing the outbreak.
	 *
	 * @param state the outbreak state
	 * @return CSV DTO for the outbreak
	 */
	public abstract ImmutableOutbreakCSV toCSV(OutbreakState state);

	/**
	 * Map a person and their demographic and baseline information to a CSV DTO
	 * for demographic exports. The mapper extracts the person id and demographic
	 * attributes for inclusion in the CSV.
	 *
	 * @param person   the person to map
	 * @param demog    the person's demographic information
	 * @param baseline the person's baseline information
	 * @return CSV DTO for the person's demographics
	 */
	protected abstract ImmutablePersonDemographicsCSV toCSV(
			Person person, PersonDemographic demog, PersonBaseline baseline
	);

	/**
	 * Map a person history and contact to a CSV DTO for contact exports. The
	 * mapper extracts the person id and contact participant ids for inclusion in
	 * the CSV.
	 *
	 * @param person  the person whose history is being mapped
	 * @param contact the contact to map, which should involve the person as
	 *                participant1
	 * @return CSV DTO for the contact
	 */
	@Mapping(
			target = "id",
			source = "contact.participant1Id"
	)
	@Mapping(
			target = "contactId",
			source = "contact.participant2Id"
	)
	protected abstract ImmutableContactCSV toCSV(
			PersonHistory person, Contact contact
	);

	/**
	 * Map a person state to a line list CSV DTO. The mapper extracts the person
	 * id and risk model log odds for inclusion in the line list.
	 *
	 * @param state the person state to map
	 * @return line list CSV DTO
	 */
	@Mapping(
			target = "personId",
			source = "entity.id"
	)
	@Mapping(
			target = "logOddsInfectiousToday",
			source = "riskModel.logOddsInfectiousToday"
	)
	public abstract ImmutableLineListCSV toCSV(PersonState state);

	/**
	 * Map a test result and corresponding person history to a CSV DTO for test
	 * results. The mapper extracts the person id and test time from the person
	 * history, and the test name from the test result parameters.
	 *
	 * @param t  the test result to map
	 * @param ph the corresponding person history providing context for the test
	 * @return CSV DTO for the test result
	 */
	@Mapping(
			target = "time",
			source = "ph.time"
	)
	@Mapping(
			target = "id",
			source = "ph.entity.id"
	)
	@Mapping(
			target = "type",
			source = "t.testParams.testName"
	)
	@Mapping(
			target = "sampleTime",
			source = "t.time"
	)
	public abstract ImmutablePersonTestsCSV toCSV(
			TestResult t, PersonHistory ph
	);

	/**
	 * Convenience: map person demographics to the CSV DTO used for demographic
	 * exports.
	 *
	 * @param person the person whose demographics to export
	 * @return demographic CSV DTO
	 */
	public ImmutablePersonDemographicsCSV toDemog(Person person) {
		return this.toCSV(person, person.getDemographic(), person.getBaseline());
	}

	/**
	 * Map the current outbreak state to a CSV DTO representing the final state
	 * of the outbreak. This is used for final state exports and includes
	 * cumulative counts and other summary statistics.
	 *
	 * @param currentState the current outbreak state
	 * @return CSV DTO for the final outbreak state
	 */
	public abstract ImmutableOutbreakFinalStateCSV toFinalCSV(
			OutbreakState currentState
	);

	/**
	 * Map an outbreak's infectivity profile to a CSV DTO. Each record represents
	 * the probability of transmission at a given time lag tau.
	 *
	 * @param outbreak    the outbreak from which to derive the profile
	 * @param tau         the time lag for this profile entry
	 * @param probability the probability of transmission at this time lag
	 * @return CSV DTO for the infectivity profile entry
	 */
	protected abstract ImmutableInfectivityProfileCSV toIP(
			Outbreak outbreak, int tau, double probability
	);

	/**
	 * Map an outbreak to a CSV DTO representing the outbreak configuration. This
	 * includes parameters from the outbreak baseline and other setup parameters
	 * that define the initial conditions of the outbreak.
	 *
	 * @param outbreak the outbreak to map
	 * @return CSV DTO for the outbreak configuration
	 */
	public abstract ImmutableOutbreakConfigurationJson toJson(Outbreak outbreak);
}
