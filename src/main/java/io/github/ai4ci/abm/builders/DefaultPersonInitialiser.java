package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.util.Sampler;

public interface DefaultPersonInitialiser {

	default void initialisePerson(ImmutablePersonState.Builder builder, Person person,
			Sampler rng) {
		ExecutionConfiguration params = person.getOutbreak().getExecutionConfiguration();
		SetupConfiguration configuration = person.getOutbreak().getSetupConfiguration();
		// PersonBaseline baseline = person.getBaseline();
		double limit = ((double) configuration.getInitialImports())/configuration.getNetworkSize();
		
		builder
			.setTransmissibilityModifier(1.0)
			.setMobilityModifier(1.0)
			.setImmuneModifier(1.0)
			.setComplianceModifier(1.0)
			.setSusceptibilityModifier(1.0)
			
			.setContactDetectedProbability( params.getContactDetectedProbability().sample(rng) )
			// .setScreeningInterval( params.getScreeningPeriod().sample(rng) )
			.setInHostModel(
					params.getInHostConfiguration().initialise(person, rng)
			)
			.setImportationExposure(
					rng.uniform() < limit ? 2.0 : 0
			)
			.setImmunisationDose(0D);
		
	}
	
}
