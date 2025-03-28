package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.util.ModelNav;

public interface Abstraction {

	@JsonTypeInfo(use = Id.DEDUCTION)
	@JsonSubTypes( {@Type(PartialSetupConfiguration.class), @Type(PartialExecutionConfiguration.class)})
	public interface Modification {}



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
		
		default String getModelName() {
			return ModelNav.modelSetup(this).getName();
		}
		
		default Integer getModelReplica() {
			return ModelNav.modelSetup(this).getReplicate();
		}
		
		default String getExperimentName() {
			return ModelNav.modelParam(this).getName();
		}
		
		default Integer getExperimentReplica() {
			return ModelNav.modelParam(this).getReplicate();
		}
	}
	
	
	
	public interface Named {
		String getName();
	}
	
	public interface Replica {
		@JsonIgnore @Value.Default default Integer getReplicate() {return 0;}
	}
	
	public interface HistoricalStateProvider<H extends TemporalState<?>> {
		
		public List<H> getHistory();
		
		public default Optional<H> getHistory(int delay) {
			if (delay < 0) return Optional.empty();
			if (this.getHistory().size() < delay) return Optional.empty();
			return Optional.of(this.getHistory().get(delay));
		}
		
		public default Optional<H> getHistoryEntry(int time) {
			if (getHistory().size() == 0) return Optional.empty();
			int currentTime = getHistory().get(0).getTime();
			int delay = currentTime - time;
			return getHistory(delay);
		}
		
	}
}
