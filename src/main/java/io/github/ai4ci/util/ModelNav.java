package io.github.ai4ci.util;

import java.util.Optional;
import java.util.stream.Stream;

import io.github.ai4ci.abm.OutbreakBaseline;
import io.github.ai4ci.abm.OutbreakHistory;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.OutbreakTemporalState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonBaseline;
import io.github.ai4ci.abm.PersonHistory;
import io.github.ai4ci.abm.PersonState;
import io.github.ai4ci.abm.PersonTemporalState;
import io.github.ai4ci.abm.mechanics.Abstraction.TemporalState;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;

public class ModelNav {

	public static OutbreakState modelState(PersonTemporalState person) {
		return person.getEntity().getOutbreak().getCurrentState();
	}
	
	public static OutbreakBaseline modelBase(PersonTemporalState person) {
		return person.getEntity().getOutbreak().getBaseline();
	}
	
	public static ExecutionConfiguration modelParam(PersonTemporalState person) {
		return person.getEntity().getOutbreak().getExecutionConfiguration();
	}
	
	public static ExecutionConfiguration modelParam(OutbreakTemporalState outbreak) {
		return outbreak.getEntity().getExecutionConfiguration();
	}
	
	public static OutbreakState modelState(Person person) {
		return person.getOutbreak().getCurrentState();
	}
	
	public static OutbreakBaseline modelBase(Person person) {
		return person.getOutbreak().getBaseline();
	}
	
	public static ExecutionConfiguration modelParam(Person person) {
		return person.getOutbreak().getExecutionConfiguration();
	}
	
	public static PersonBaseline baseline(PersonTemporalState person) {
		return person.getEntity().getBaseline();
	}
	
	public static PersonBaseline baseline(Person person) {
		return person.getBaseline();
	}
	
	public static Optional<PersonHistory> history(PersonState person) {
		return person.getEntity().getCurrentHistory();
	}
	
	public static Stream<PersonHistory> history(PersonState person, int days) {
		return person.getEntity().getHistory().stream().limit(days);
	}
	
	public static PersonState current(PersonHistory person) {
		return person.getEntity().getCurrentState();
	}

	public static Stream<PersonState> peopleState(OutbreakState current) {
		return current.getEntity().getPeople().parallelStream().map(p -> p.getCurrentState());
	}
	
	
	public static Stream<PersonHistory> peopleCurrentHistory(OutbreakState current) {
		return current.getEntity().getPeople().parallelStream().flatMap(p -> p.getCurrentHistory().stream());
	}

	public static Stream<Person> people(OutbreakState current) {
		return current.getEntity().getPeople().parallelStream();
	}

	public static Optional<OutbreakHistory> history(OutbreakState outbreakState) {
		return outbreakState.getEntity().getCurrentHistory();
	}

	public static Stream<OutbreakHistory> history(OutbreakState outbreakState, int period) {
		return outbreakState.getEntity().getHistory().stream().limit(period);
	}

	public static ExecutionConfiguration modelParam(TemporalState<?> temporalState) {
		if (temporalState instanceof OutbreakTemporalState) {
			return modelParam((OutbreakTemporalState) temporalState);
		} else if (temporalState instanceof PersonTemporalState) {
			return modelParam((PersonTemporalState) temporalState);
		}
		throw new RuntimeException("Unknown temporal state type");
	}

	public static SetupConfiguration modelSetup(TemporalState<?> temporalState) {
		if (temporalState instanceof OutbreakTemporalState) {
			return modelSetup((OutbreakTemporalState) temporalState);
		} else if (temporalState instanceof PersonTemporalState) {
			return modelSetup((PersonTemporalState) temporalState);
		}
		throw new RuntimeException("Unknown temporal state type");
	}

	public static SetupConfiguration modelSetup(OutbreakTemporalState outbreak) {
		return outbreak.getEntity().getSetupConfiguration();
	}
	
	public static SetupConfiguration modelSetup(PersonTemporalState outbreak) {
		return outbreak.getEntity().getOutbreak().getSetupConfiguration();
	}

	/**
	 * The corresponding history entries for this point in time in the outbreak
	 * i.e. A stream of individuals at the same time point. 
	 * @param outbreakHistory
	 * @return
	 */
	public static Stream<PersonHistory> peopleHistory(OutbreakHistory outbreakHistory) {
		int time = outbreakHistory.getTime();
		return 
			outbreakHistory.getEntity().getPeople().stream().flatMap(
					p -> p.getHistoryEntry(time).stream()
			);
	}

	public static OutbreakHistory outbreakHistory(PersonHistory personHistory) {
		int time = personHistory.getTime();
		return personHistory.getEntity().getOutbreak().getHistoryEntry(time).get();
	}

	
	

	
}
