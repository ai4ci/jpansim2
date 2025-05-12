package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.util.SimpleDistribution;

@Value.Immutable
//@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.FALSE, defaultImpl = ImmutableStochasticModel.class)
//@JsonSubTypes({
//	@Type(value=ImmutableStochasticModel.class, name="complete"), 
//	@Type(value=PartialStochasticModel.class, name="partial")
//})
@JsonSerialize(as = ImmutableStochasticModel.class)
@JsonDeserialize(as = ImmutableStochasticModel.class)
public interface StochasticModel extends InHostConfiguration {
	
//	@Partial @Value.Immutable 
//	@JsonSerialize(as = PartialStochasticModel.class)
//	@JsonDeserialize(as = PartialStochasticModel.class)
//	public interface _PartialStochasticModel extends StochasticModel, Abstraction.Modification<StochasticModel>{
//		default _PartialStochasticModel self() {return this;}
//	}
	
	public static ImmutableStochasticModel DEFAULT = ImmutableStochasticModel.builder()
			.setTargetCellCount(10000)
			.setImmuneTargetRatio( SimpleDistribution.logNorm(1D, 0.1))
			.setImmuneActivationRate( SimpleDistribution.logNorm(1D, 0.1))
			.setImmuneWaningRate( SimpleDistribution.logNorm(1D/150, 0.01))
			.setInfectionCarrierProbability( SimpleDistribution.point(0D))
			.setTargetRecoveryRate( SimpleDistribution.logNorm( 1D/7, 0.1) )
			.setBaselineViralInfectionRate( 1D )
			.setBaselineViralReplicationRate( 4D )
			.setVirionsDiseaseCutoff( 1000 )
			// .setTargetSymptomsCutoff( 0.2 )
			.build();
	
	Integer getTargetCellCount();
	SimpleDistribution getImmuneTargetRatio();
	SimpleDistribution getImmuneActivationRate();
	SimpleDistribution getImmuneWaningRate();
	SimpleDistribution getInfectionCarrierProbability();
	SimpleDistribution getTargetRecoveryRate();
	
	Double getBaselineViralInfectionRate();
	Double getBaselineViralReplicationRate();
	/**
	 * Given the parameters for the virus, what is the unit of virions. This is
	 * a calibration parameter, and defines the limit of what is considered 
	 * disease. It should be the lowest value that is transmissible and defines
	 * a patient as "infected" it does not follow that an uninfected patient
	 * does not have a small in-host viral load.
	 * @return the cutoff
	 */
	Integer getVirionsDiseaseCutoff();
	
	/**
	 * The proportion of target cells that are required to be inoperational
	 * (i.e. in infected or removed state) before the patient exhibits 
	 * symptoms. 
	 * @return a percentage cutoff, larger numbers means fewer symptoms.
	 */
	// Double getTargetSymptomsCutoff(); removed for IFR etc.
	
	
	
}
