package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.config.setup.SetupConfiguration;

public interface Partials {

	@Partial
	@Value.Immutable
	@SuppressWarnings("immutables")
	@JsonSerialize(as = PartialExecutionConfiguration.class)
	@JsonDeserialize(as = PartialExecutionConfiguration.class)
	public interface _PartialExecutionConfiguration
			extends ExecutionConfiguration, Modification<ExecutionConfiguration> {
		default _PartialExecutionConfiguration self() {
			return this;
		}
	}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialSetupConfiguration.class)
	@JsonDeserialize(as = PartialSetupConfiguration.class)
	public interface _PartialSetupConfiguration extends SetupConfiguration, Modification<SetupConfiguration> {
		default _PartialSetupConfiguration self() {return this;}
	}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialMarkovStateModel.class)
	@JsonDeserialize(as = PartialMarkovStateModel.class)
	public interface _PartialMarkovStateModel extends MarkovStateModel, Modification<MarkovStateModel>{
		default _PartialMarkovStateModel self() {return this;}
	}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialPhenomenologicalModel.class)
	@JsonDeserialize(as = PartialPhenomenologicalModel.class)
	public interface _PartialPhenomenologicalModel extends PhenomenologicalModel, Modification<PhenomenologicalModel>{
		default _PartialPhenomenologicalModel self() {return this;}
	}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialStochasticModel.class)
	@JsonDeserialize(as = PartialStochasticModel.class)
	public interface _PartialStochasticModel extends StochasticModel, Modification<StochasticModel>{
		default _PartialStochasticModel self() {return this;}
	}
	
	
}
