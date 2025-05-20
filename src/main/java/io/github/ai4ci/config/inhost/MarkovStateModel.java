package io.github.ai4ci.config.inhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
import io.github.ai4ci.config.DemographicAdjustment;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.SimpleDistribution;

@Value.Immutable
//@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.FALSE, defaultImpl = ImmutableStochasticModel.class)
//@JsonSubTypes({
//	@Type(value=ImmutableStochasticModel.class, name="complete"), 
//	@Type(value=PartialStochasticModel.class, name="partial")
//})
@JsonSerialize(as = ImmutableMarkovStateModel.class)
@JsonDeserialize(as = ImmutableMarkovStateModel.class)
public interface MarkovStateModel extends InHostConfiguration, DemographicAdjustment.Markov<Distribution, Double> {
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialMarkovStateModel.class)
	@JsonDeserialize(as = PartialMarkovStateModel.class)
	public interface _PartialMarkovStateModel extends MarkovStateModel, Abstraction.Modification<MarkovStateModel>{
		default _PartialMarkovStateModel self() {return this;}
	}
	
	public static ImmutableMarkovStateModel DEFAULT = ImmutableMarkovStateModel.builder()
			.setImmuneWaningHalfLife(SimpleDistribution.logNorm(300D, 10D))
			.setIncubationPeriod(SimpleDistribution.logNorm(5D, 2D))
			.setInfectiousDuration(SimpleDistribution.logNorm(8D,3D))
			.setSymptomDuration(SimpleDistribution.logNorm(5D,3D))
			.build();
	
	Distribution getIncubationPeriod();
	
	Distribution getInfectiousDuration(); // not age specfic
	Distribution getSymptomDuration(); // not age specfic
	
	Distribution getImmuneWaningHalfLife();
	
	
	// The markov model has no internal representation of continuous severity 
	// and the cutoffs are arbitrary
	
	default double getSeveritySymptomsCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return 1-configuration.getInfectionCaseRate();
	}
	
	default double getSeverityHospitalisationCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return 1-configuration.getInfectionHospitalisationRate();
	}
	
	default double getSeverityFatalityCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return 1-configuration.getInfectionFatalityRate();
	}
	
}
