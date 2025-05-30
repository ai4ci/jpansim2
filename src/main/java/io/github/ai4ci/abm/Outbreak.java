package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.jgrapht.graph.SimpleWeightedGraph;

import io.github.ai4ci.Data;
import io.github.ai4ci.abm.mechanics.StateMachine;
import io.github.ai4ci.abm.mechanics.Abstraction.Entity;
import io.github.ai4ci.abm.mechanics.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Ephemeral;
import io.github.ai4ci.util.ThreadSafeArray;

/**
 * The main outbreak class is a mutable structure holding the state of the whole 
 * simulation, including configuration, social network, etc.
 */
@Value.Modifiable
@Data.Mutable
public abstract class Outbreak implements Entity, HistoricalStateProvider<OutbreakHistory>, Cloneable {
 
	public abstract String getUrn();
	public abstract ThreadSafeArray<Person> getPeople();
	public abstract SetupConfiguration getSetupConfiguration();
	public abstract ExecutionConfiguration getExecutionConfiguration();
	public abstract OutbreakBaseline getBaseline();
	public abstract Ephemeral<ImmutableOutbreakState.Builder> getNextState();
	public abstract Ephemeral<ImmutableOutbreakHistory.Builder> getNextHistory();
	public abstract StateMachine getStateMachine();
	
	public abstract OutbreakState getCurrentState();
	public abstract List<OutbreakHistory> getHistory();
	
	public abstract SimpleWeightedGraph<Person, SocialRelationship> getSocialNetwork();
	//public abstract NetworkBuilder<PersonHistory, PersonHistory.Infection> getInfections();
	
	Optional<Person> getPersonById(int id) {
		if (id >= getPeople().size()) return Optional.empty(); 
		return Optional.ofNullable(getPeople().get(id));
	};
	
	// N.B. these must be final to prevent immutables inserting a completeness check 
	// that means that all partially constructed objects are regarded as the same
	// as each other...
	public final int hashCode() {return super.hashCode();}
	public final boolean equals(Object another) {return super.equals(another);}
	
	public String toString() {
		return getUrn();
	}
	
	public Optional<OutbreakHistory> getCurrentHistory() {
		return getHistory().stream().findFirst();
	}
	
	public static ModifiableOutbreak createOutbreakStub() {
		ModifiableOutbreak tmp = new ModifiableOutbreak();
		tmp.setNextHistory(Ephemeral.empty());
		tmp.setNextState(Ephemeral.empty());
		tmp.setStateMachine(StateMachine.stub());
		return tmp;
	}
	public Optional<PersonHistory> getPersonHistoryByIdAndTime(int id, int time) {
		return this.getPersonById(id).flatMap(p -> p.getHistoryEntry(time));
	}; 
	
	
}
