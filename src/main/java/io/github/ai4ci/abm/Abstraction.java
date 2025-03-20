package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

public interface Abstraction {

	public interface Entity extends Serializable {
		@Value.NonAttribute String getUrn();
	}
	
	// This has potential to be problematic if multiple entities are sampling
	// on the same thread and keep resetting the seed
//	public interface SamplingEntity extends Entity {
//		default Sampler getSampler() {return Sampler.getSampler(getUrn());}
//	}
	
	public interface TemporalState<E extends Entity> extends Serializable {
		
		E getEntity();
		Integer getTime();
		
		@Value.Derived default String getUrn() {
			return getEntity().getUrn()+":step:"+getTime();
		}
	}
	
	
	
	public interface Named {
		String getName();
	}
	
	public interface HistoricalStateProvider<H extends TemporalState<?>> {
		
		public List<H> getHistory();
		
		public default Optional<H> getHistory(int delay) {
			if (delay < 0) return Optional.empty();
			if (this.getHistory().size() > delay) return Optional.empty();
			return Optional.of(this.getHistory().get(delay));
		}
		
		public default Optional<H> getHistoryEntry(int time) {
			return getHistory().stream().filter(
					h -> h.getTime() == time
			).findFirst();
		}
		
	}
}
