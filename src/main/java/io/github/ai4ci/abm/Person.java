package io.github.ai4ci.abm;

import java.util.List;
import java.util.Optional;

import org.davidmoten.hilbert.HilbertCurve;
import org.davidmoten.hilbert.SmallHilbertCurve;
import org.immutables.value.Value;

import io.github.ai4ci.Data;
import io.github.ai4ci.abm.mechanics.StateMachine;
import io.github.ai4ci.abm.mechanics.Abstraction.Entity;
import io.github.ai4ci.abm.mechanics.Abstraction.HistoricalStateProvider;
import io.github.ai4ci.util.Ephemeral;

@Value.Modifiable
@Data.Mutable
public abstract class Person implements Entity, HistoricalStateProvider<PersonHistory> {
 
	/** 
	 * Creates a new person and adds them into the outbreak network
	 * TODO: this is not suitable for creatimg people in a multithreaded 
	 * manner. It would be better for the outbreak person list to be an
	 * array with
	 * @param outbreak
	 * @return
	 */
	public static ModifiablePerson createPersonStub(Outbreak outbreak) {
		ModifiablePerson tmp = new ModifiablePerson();
		tmp.setOutbreak(outbreak);
		int id = outbreak.getPeople().put(tmp);
		tmp.setId(id);
		tmp.setNextHistory(Ephemeral.empty());
		tmp.setNextState(Ephemeral.empty());
		tmp.setStateMachine(StateMachine.stub());
		tmp.setDemographic(PersonDemographic.DEFAULT);
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
	
	public abstract Ephemeral<ImmutablePersonState.Builder> getNextState();
	public abstract Ephemeral<ImmutablePersonHistory.Builder> getNextHistory();
	public abstract StateMachine getStateMachine();

	public abstract List<PersonHistory> getHistory();
	
	public long[] getHilbertCoordinates() {
		double size = (double) this.getOutbreak().getSetupConfiguration().getNetworkSize();
		int bits = (int) Math.ceil(0.5*Math.log(size)/Math.log(2));
		SmallHilbertCurve c = HilbertCurve.small().bits(bits).dimensions(2);
		return c.point(this.getId());
	}
	
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
