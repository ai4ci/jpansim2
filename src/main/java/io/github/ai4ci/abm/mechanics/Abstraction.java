package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleUnaryOperator;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.abm.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.util.ImmutableFixedValueFunction;
import io.github.ai4ci.config.setup.PartialAgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.inhost.PartialPhenomenologicalModel;
import io.github.ai4ci.config.inhost.PartialStochasticModel;
import io.github.ai4ci.config.setup.PartialWattsStrogatzConfiguration;
import io.github.ai4ci.util.EmpiricalFunction.Link;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution;
import io.github.ai4ci.util.ImmutableEmpiricalFunction;
import io.github.ai4ci.util.ImmutableResampledDistribution;
import io.github.ai4ci.util.ImmutableSimpleDistribution;
import io.github.ai4ci.util.ImmutableTransformedDistribution;
import io.github.ai4ci.util.ModelNav;
import io.github.ai4ci.util.ResampledDistribution;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.util.TransformedDistribution;

public interface Abstraction {

	@JsonTypeInfo(use = Id.DEDUCTION)
	
	@JsonSubTypes( {
		@Type(PartialWattsStrogatzConfiguration.class), 
		@Type(PartialExecutionConfiguration.class), 
		@Type(PartialAgeStratifiedNetworkConfiguration.class),
		@Type(PartialStochasticModel.class),
		@Type(PartialPhenomenologicalModel.class)
	})
	public interface Modification<X> {
		@Value.NonAttribute X self();
	}
 
	@JsonTypeInfo(use = Id.DEDUCTION)
	@JsonSubTypes( {
		@Type(ImmutableEmpiricalFunction.class), 
		@Type(ImmutableFixedValueFunction.class)
	})
	public interface SimpleFunction {
		double value(double y);
	}

	public interface Entity extends Serializable {
		@Value.NonAttribute String getUrn();
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
	
	@JsonTypeInfo(use = Id.DEDUCTION)
	@JsonSubTypes( {
		@Type(ImmutableSimpleDistribution.class),
		@Type(ImmutableEmpiricalDistribution.class)
	})
	public interface Distribution {
		
		int PRECISION = 10000;
		double DX = 0.00001; 
		
		double getCentral();
		double getCumulative(double x);
		double getMedian();
		
		double sample(Sampler rng);
		
		default double sample() {
			return sample(Sampler.getSampler());
		}
		
		default double getDensity(double x) {
			return (getCumulative(x+DX)-getCumulative(x-DX))/(2*DX); 
		}
		
		default ResampledDistribution combine(Distribution with, BiFunction<Double,Double,Double> using) {
			return ImmutableResampledDistribution.builder()
					.setFirst(this)
					.setSecond(with)
					.setCombiner(using)
					.build();
		};
		
		default TransformedDistribution transform(Link link) {
			return transform(link.fn, link.invFn);
		}
		
		default TransformedDistribution transform(DoubleUnaryOperator link) {
			return transform(link, (d) -> {
				throw new RuntimeException("Inverse not implemented");});
		}
		
		default TransformedDistribution transform(DoubleUnaryOperator link, DoubleUnaryOperator inverse) {
			return ImmutableTransformedDistribution.builder()
					.setBaseDistribution(this)
					.setLink(link)
					.setInverseLink(inverse)
					.build();
		};

	}
	
	public interface TemporalState<E extends Entity> extends Serializable {
		
		E getEntity();
		Integer getTime();
		
		@Value.Derived default String getUrn() {
			return getEntity().getUrn()+":step:"+getTime();
		}
		
		default String getModelName() {
			return getEntity().getModelName();
		}
		
		default Integer getModelReplica() {
			return getEntity().getModelReplica();
		}
		
		default String getExperimentName() {
			return getEntity().getExperimentName();
		}
		
		default Integer getExperimentReplica() {
			return getEntity().getExperimentReplica();
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
			if (this.getHistory().size() <= delay) return Optional.empty();
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
