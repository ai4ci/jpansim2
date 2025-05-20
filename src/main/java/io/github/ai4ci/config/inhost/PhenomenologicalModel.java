package io.github.ai4ci.config.inhost;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
import io.github.ai4ci.config.DemographicAdjustment;
import io.github.ai4ci.util.SimpleDistribution;

@Value.Immutable
//@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.FALSE, defaultImpl = ImmutablePhenomenologicalModel.class)
//@JsonSubTypes({
//	@Type(value=ImmutablePhenomenologicalModel.class, name="complete"), 
//	@Type(value=PartialPhenomenologicalModel.class, name="partial")
//})
@JsonSerialize(as = ImmutablePhenomenologicalModel.class)
@JsonDeserialize(as = ImmutablePhenomenologicalModel.class)
public interface PhenomenologicalModel extends InHostConfiguration,  DemographicAdjustment.Phenomenological<Distribution, Double> {
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialPhenomenologicalModel.class)
	@JsonDeserialize(as = PartialPhenomenologicalModel.class)
	public interface _PartialPhenomenologicalModel extends PhenomenologicalModel, Abstraction.Modification<PhenomenologicalModel>{
		default _PartialPhenomenologicalModel self() {return this;}
	}
	
	public static ImmutablePhenomenologicalModel DEFAULT = ImmutablePhenomenologicalModel.builder()
			
			//.setSymptomCutoff(0.5)
			.setInfectiousnessCutoff(0.2)
			
			.setIncubationPeriod(SimpleDistribution.logNorm(5D, 2D))
			.setApproxPeakViralLoad( SimpleDistribution.unimodalBeta(0.5, 0.1))
			.setIncubationToPeakViralLoadDelay(SimpleDistribution.logNorm(2D, 1D))
			.setPeakToRecoveryDelay(SimpleDistribution.logNorm(5D,3D))
			
			.setApproxPeakImmuneResponse( SimpleDistribution.unimodalBeta(0.5, 0.1))
			.setImmuneWaningHalfLife(SimpleDistribution.logNorm(300D, 10D))
			.setPeakImmuneResponseDelay( SimpleDistribution.logNorm(20D, 4D) )
			
			.build();
	
	Double getInfectiousnessCutoff();
	
	Distribution getIncubationPeriod();
	Distribution getApproxPeakViralLoad();
	Distribution getIncubationToPeakViralLoadDelay();
	Distribution getPeakToRecoveryDelay();
	
	Distribution getApproxPeakImmuneResponse();
	Distribution getPeakImmuneResponseDelay();
	Distribution getImmuneWaningHalfLife();
	
}
