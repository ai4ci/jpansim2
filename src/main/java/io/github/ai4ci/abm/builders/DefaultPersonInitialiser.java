package io.github.ai4ci.abm.builders;

import java.util.Optional;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

public interface DefaultPersonInitialiser extends DefaultInHostStochasticStateInitialiser, DefaultInHostPhenomenologicalStateInitialiser, DefaultInHostMarkovStateInitialiser {

	default ImmutablePersonState initialisePerson(ImmutablePersonState.Builder builder, Person person,
			Sampler rng) {
		ExecutionConfiguration params = 
				ReflectionUtils.modify(
						person.getOutbreak().getExecutionConfiguration(),
						person.getOutbreak().getExecutionConfiguration().getDemographicAdjustment(),
						person.getDemographic()
				);
		
		SetupConfiguration configuration = person.getOutbreak().getSetupConfiguration();
		// PersonBaseline baseline = person.getBaseline();
		double limit = ((double) configuration.getInitialImports())/configuration.getNetworkSize();
		
		builder
			.setTransmissibilityModifier(1.0)
			.setMobilityModifier(1.0)
			.setComplianceModifier(1.0)
			.setSusceptibilityModifier(1.0)
			.setAppUseModifier(1.0)
			
			.setInHostModel(
				initialiseInHostModel(params.getInHostConfiguration(),
						params,
						Optional.ofNullable(person.getDemographic()), rng, person.getOutbreak().getCurrentState().getTime())
			)
			.setImportationExposure(
					rng.uniform() < limit ? 2.0 : 0
			)
			.setImmunisationDose(0D);
		
		return builder.build();
	}
	
	@SuppressWarnings("unchecked")
	default <CFG extends InHostConfiguration> InHostModelState<CFG> initialiseInHostModel(CFG config, ExecutionConfiguration execConfig, Optional<PersonDemographic> person, Sampler rng, int time) {
		if (config instanceof PhenomenologicalModel) return (InHostModelState<CFG>) initialiseInHostModel((PhenomenologicalModel) config, execConfig, person, rng, time);
		if (config instanceof StochasticModel) return (InHostModelState<CFG>) initialiseInHostModel((StochasticModel) config, execConfig, person, rng, time);
		if (config instanceof MarkovStateModel) return (InHostModelState<CFG>) initialiseInHostModel((MarkovStateModel) config, execConfig, person, rng, time);
		throw new RuntimeException("Unknown in host configuration type");
	}
	 
	
	
	
	
	
}
