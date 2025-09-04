package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import io.github.ai4ci.Data;
import io.github.ai4ci.abm.mechanics.Abstraction.Entity;
import io.github.ai4ci.abm.mechanics.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.abm.mechanics.StateMachine;
import io.github.ai4ci.util.Ephemeral;

/**
 * The main Person class is a mutable holder that contains immutable data 
 * pertaining to baseline, current state, recent history etc. 
 */
@Value.Modifiable
@Data.Mutable
public abstract class Person implements Entity, HistoricalStateProvider<PersonHistory> {
 
	/** 
	 * Creates a new person and adds them into the outbreak network.
	 */
	public static ModifiablePerson createPersonStub(Outbreak outbreak) {
		ModifiablePerson tmp = new ModifiablePerson();
		tmp.setOutbreak(outbreak);
		int id = outbreak.getPeople().put(tmp);
		tmp.setId(id);
		tmp.setNextHistory(Ephemeral.empty());
		tmp.setNextState(Ephemeral.empty());
		tmp.setStateMachine(StateMachine.stub());
		tmp.setDemographic(PersonDemographic.stub(tmp));
		return tmp;
	}
	
	public String getUrn() {
		return getOutbreak().getUrn()+":person:"+getId();
	}
	public abstract Integer getId();
	public abstract Outbreak getOutbreak();
	public abstract PersonBaseline getBaseline();
	public abstract PersonState getCurrentState();
	public abstract PersonDemographic getDemographic();
	
	/*
	 * Only populated during the update cycle, and copied from the t-1 state. 
	 * This is where changes to the future t state are made.
	 */
	public abstract Ephemeral<ImmutablePersonState.Builder> getNextState();
	
	/*
	 * Only populated during the update cycle largely as a mapping from the t-1 
	 * state. Historical events are added most notably test sampling.
	 */
	public abstract Ephemeral<ImmutablePersonHistory.Builder> getNextHistory();
	
	/*
	 * The person's behavioural state machine.
	 */
	public abstract StateMachine getStateMachine();

	/**
	 * Reverse time ordered list of historical states (recent first).
	 */
	public abstract List<PersonHistory> getHistory();
	
	
	
	/**
	 * Gets the first history item unless there is no history. If called during
	 * update cycle this will get the history at the same time as the current state.
	 * If called after it will be the previous day.
	 */
	public Optional<PersonHistory> getCurrentHistory() {
		if (getHistory().size() == 0) return Optional.empty();
		return Optional.of(getHistory().get(0));
	}
	
	public final int hashCode() {return super.hashCode();}
	public final boolean equals(Object another) {return super.equals(another);}
	
	public String toString() {
		return getUrn();
	}
}
