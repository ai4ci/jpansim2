package io.github.ai4ci.abm;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutableInHostPhenomenologicalState.Builder;
import io.github.ai4ci.config.ExposureModel;
import io.github.ai4ci.config.ExposureModel.BiPhasicLogistic;
import io.github.ai4ci.config.InHostConfiguration.PhenomenologicalModel;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface InHostPhenomenologicalState extends InHostModelState<PhenomenologicalModel> {
	
	static Logger log = LoggerFactory.getLogger(InHostPhenomenologicalState.class);
	
	BiPhasicLogistic getViralLoadModel();
	BiPhasicLogistic getImmunityModel();
	List<ExposureModel> getExposures();
	Integer getTime();
	
	@Value.Derived default double getViralLoad() {
		return 1 - this.getExposures().stream()
		.mapToDouble(e -> e.getExposureViralLoad(getTime(), getViralLoadModel()))
		.map(vl -> 1-vl)
		.reduce((d1,d2) -> d1*d2)
		.orElse(1);
	}
	
	@Value.Derived default double getImmuneActivity() {
		return 1-this.getExposures().stream()
		.mapToDouble(e -> e.getExposureImmuneActivity(getTime(), getImmunityModel()))
		.map(vl -> 1-vl)
		.reduce((d1,d2) -> d1*d2)
		.orElse(1);
	}
	
	PhenomenologicalModel getConfig();
	
	default double getNormalisedViralLoad() {
		double tmp = Conversions.rateRatio( getViralLoad(), this.getConfig().getInfectiousnessCutoff() );
		return tmp < 1 ? 0 : tmp;
	};
	
	/**
	 * infected but not necessarily infectious.
	 */
	default boolean isInfected() {
		return getViralLoad() > this.getConfig().getInfectiousnessCutoff() / 10;  
	}
	
	default double getNormalisedSeverity() {
		return Conversions.rateRatio( getViralLoad(), this.getConfig().getSymptomCutoff() );
	};

	
	static InHostPhenomenologicalState initialise(PhenomenologicalModel config, Sampler rng, int time) {
		double incubation = config.getIncubationPeriod().sample(rng);
		return ImmutableInHostPhenomenologicalState.builder()
				.setTime(time)
				.setConfig(config)
				.setViralLoadModel(
						BiPhasicLogistic.calibrateViralLoad(
								incubation, //onsetTime
								config.getIncubationToPeakViralLoadDelay().sample(rng), // peakTime, 
								config.getPeakToRecoveryDelay().sample(rng), // double duration, 
								config.getInfectiousnessCutoff(),// double thresholdLevel, 
								config.getApproxPeakViralLoad().sample(rng)// double peakLevel
						)
				)
				.setImmunityModel(
						BiPhasicLogistic.calibrateImmuneActivity(
								config.getPeakImmuneResponseDelay().sample(rng), // peakTime, 
								config.getApproxPeakImmuneResponse().sample(rng),// peakLevel, 
								config.getImmuneWaningHalfLife().sample(rng) // halfLife
						)
				)
				.build();
	}
	
	default InHostPhenomenologicalState update(Sampler sampler, double virionDose, double immunisationDose) { //, double immuneModifier) {
		// Overall modifiers are a number around 1:
		// Double hostImmunity = immuneModifier;
		
		double virionsDx = this.getConfig().getInfectiousnessCutoff();
		int time = this.getTime();
		double virionExposure = Conversions.scaleProbabilityByRR(virionsDx, virionDose);
		
		// Check for exposures that are no longer relevant from the purposes
		// of contributing to overall immunity... I should probably refactor
		// this.
		List<ExposureModel> tmp = this.getExposures().stream()
				.filter(em -> !em.isIrrelevant(time+1, this.getImmunityModel()))
				.collect(Collectors.toList());
		
		ImmutableInHostPhenomenologicalState.Builder out = ImmutableInHostPhenomenologicalState.builder().from(this)
				.setExposures(tmp);
		
		// add new exposure from today
		if (virionExposure > 0) {
			ExposureModel tmp2 =  ExposureModel.createExposure(
					virionExposure,
					this.getImmuneActivity(),
					// Conversions.scaleProbability(this.getImmuneActivity(), hostImmunity),
					this.getViralLoadModel(),
					time
				);
			out.addExposure(tmp2);
		}
		
		return out
			.setTime(time+1)
			.build();
	};
	
	
	
	
}
