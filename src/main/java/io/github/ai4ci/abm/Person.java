package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;
import org.jgrapht.graph.DefaultWeightedEdge;

import io.github.ai4ci.abm.Abstraction.Entity;
import io.github.ai4ci.abm.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.util.Data;
import io.github.ai4ci.util.Ephemeral;

@Value.Modifiable
@Data.Mutable
public abstract class Person implements Entity, HistoricalStateProvider<PersonHistory> {

	public String getUrn() {
		return getOutbreak().getUrn()+":person:"+getId();
	}
	public abstract Integer getId();
	public abstract Outbreak getOutbreak();
	public abstract PersonBaseline getBaseline();
	public abstract PersonState getCurrentState();
	
	protected abstract Ephemeral<ImmutablePersonState.Builder> getNextState();
	protected abstract Ephemeral<ImmutablePersonHistory.Builder> getNextHistory();
	public abstract StateMachine getStateMachine();

	public abstract List<PersonHistory> getHistory();
	
	public static class Relationship extends DefaultWeightedEdge implements Serializable {
		public double getConnectednessQuantile() {
			return this.getWeight();
		}
	}
	
	
	
	public Optional<PersonHistory> getCurrentHistory() {
		if (getHistory().size() == 0) return Optional.empty();
		return Optional.of(getHistory().get(0));
	}
	
	public final int hashCode() {return super.hashCode();}
	public final boolean equals(Object another) {return super.equals(another);}
	
	public static ModifiablePerson createPersonStub(Outbreak outbreak) {
		ModifiablePerson tmp = new ModifiablePerson();
		tmp.setOutbreak(outbreak);
		outbreak.getPeople().add(tmp);
		tmp.setId(outbreak.getPeople().indexOf(tmp));
		tmp.setNextHistory(Ephemeral.empty());
		tmp.setNextState(Ephemeral.empty());
		tmp.setStateMachine(StateMachine.stub());
		return tmp;
	}
	
	public String toString() {
		return getUrn();
	}
}
