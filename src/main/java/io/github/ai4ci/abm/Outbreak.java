package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import io.github.ai4ci.abm.Abstraction.Entity;
import io.github.ai4ci.abm.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.util.Data;
import io.github.ai4ci.util.Ephemeral;

@Value.Modifiable
@Data.Mutable
public abstract class Outbreak implements Entity, HistoricalStateProvider<OutbreakHistory> {
 
	public abstract String getUrn();
	public abstract List<Person> getPeople();
	public abstract SetupConfiguration getSetupConfiguration();
	public abstract ExecutionConfiguration getExecutionConfiguration();
	public abstract OutbreakBaseline getBaseline();
	protected abstract Ephemeral<ImmutableOutbreakState.Builder> getNextState();
	protected abstract Ephemeral<ImmutableOutbreakHistory.Builder> getNextHistory();
	protected abstract StateMachine getStateMachine();
	
	public abstract OutbreakState getCurrentState();
	public abstract List<OutbreakHistory> getHistory();
	
	public abstract SimpleWeightedGraph<Person, Person.Relationship> getSocialNetwork();
	public abstract DirectedAcyclicGraph<PersonHistory, PersonHistory.Infection> getInfections();
	
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
}
