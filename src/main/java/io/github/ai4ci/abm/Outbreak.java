package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.immutables.value.Value;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.Data;
import io.github.ai4ci.abm.mechanics.Abstraction.Entity;
import io.github.ai4ci.abm.mechanics.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.abm.mechanics.StateMachine;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * The main outbreak class is a mutable structure holding the state of the whole 
 * simulation, including configuration, social network, etc. It is a mutable
 * holder that contains largely immutable data structures that are operated on
 * across multiple threads.
 */
@Value.Modifiable
@Data.Mutable
public interface Outbreak extends Entity, HistoricalStateProvider<OutbreakHistory>, Cloneable {
 
	String getUrn();
	ThreadSafeArray<Person> getPeople();
	SetupConfiguration getSetupConfiguration();
	ExecutionConfiguration getExecutionConfiguration();
	OutbreakBaseline getBaseline();
	Ephemeral<ImmutableOutbreakState.Builder> getNextState();
	Ephemeral<ImmutableOutbreakHistory.Builder> getNextHistory();
	StateMachine getStateMachine();
	
	OutbreakState getCurrentState();
	List<OutbreakHistory> getHistory();
	
	ThreadSafeArray<SocialRelationship> getSocialNetwork();
	
	default int getPopulationSize() {
		return getSetupConfiguration().getNetwork().getNetworkSize();
	}
	
	default Optional<Person> getPersonById(int id) {
		if (id >= getPeople().size()) return Optional.empty(); 
		return Optional.ofNullable(getPeople().get(id));
	};
	
	// N.B. these must be final to prevent immutables inserting a completeness check 
	// that means that all partially constructed objects are regarded as the same
	// as each other...
	
	@JsonIgnore
	@Value.Derived default int hash() {return UUID.randomUUID().hashCode();}
	
	default boolean equality(Object another) {
		if (another != null && another instanceof Outbreak) {
			return 	this.hash() == ((Outbreak) another).hash();
		}
		return false;
	}
	
	default String print() {
		return getUrn();
	}
	
	default Optional<OutbreakHistory> getCurrentHistory() {
		return getHistory().stream().findFirst();
	}
	
	public static ModifiableOutbreak createOutbreakStub() {
		ModifiableOutbreak tmp = new ModifiableOutbreak();
		tmp.setNextHistory(Ephemeral.empty());
		tmp.setNextState(Ephemeral.empty());
		tmp.setStateMachine(StateMachine.stub());
		return tmp;
	}
	default Optional<PersonHistory> getPersonHistoryByIdAndTime(int id, int time) {
		return this.getPersonById(id).flatMap(p -> p.getHistoryEntry(time));
	}; 
	
	
}
