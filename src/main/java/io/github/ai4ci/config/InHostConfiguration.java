package io.github.ai4ci.config;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import io.github.ai4ci.abm.InHostModelState;
import io.github.ai4ci.abm.InHostPhenomenologicalState;
import io.github.ai4ci.abm.InHostStochasticState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.SimpleDistribution;
import io.github.ai4ci.util.Sampler;

@JsonTypeInfo(use = Id.SIMPLE_NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonSubTypes( {@Type(ImmutableStochasticModel.class), @Type(ImmutablePhenomenologicalModel.class)})
@Value.Immutable(copy=false)
public interface InHostConfiguration extends Serializable {

	static Logger log = LoggerFactory.getLogger(InHostConfiguration.class);
	
	static final double LIMIT = 0.99;
	
	@Value.Immutable(copy=false)
	public interface StochasticModel extends InHostConfiguration {
		
		public static StochasticModel DEFAULT = ImmutableStochasticModel.builder()
				.setTargetCellCount(10000)
				.setImmuneTargetRatio( SimpleDistribution.logNorm(1D, 0.1))
				.setImmuneActivationRate( SimpleDistribution.logNorm(1D, 0.1))
				.setImmuneWaningRate( SimpleDistribution.logNorm(1D/150, 0.01))
				.setInfectionCarrierProbability( SimpleDistribution.point(0D))
				.setTargetRecoveryRate( SimpleDistribution.logNorm( 1D/7, 0.1) )
				.setBaselineViralInfectionRate( 1D )
				.setBaselineViralReplicationRate( 4D )
				.setVirionsDiseaseCutoff( 1000 )
				.setTargetSymptomsCutoff( 0.2 )
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
		Double getTargetSymptomsCutoff();
		
		public default InHostStochasticState initialise(Person person, Sampler rng) {
			InHostConfiguration config = person.getOutbreak().getExecutionConfiguration().getInHostConfiguration();
			int time = person.getOutbreak().getCurrentState().getTime();
			return InHostStochasticState.initialise((StochasticModel) config, rng, time);
		}
		
//		public static ImmutableStochasticModel copyOf(InHostConfiguration source) {
//			return ConfigMerger.INSTANCE.mapper((ImmutableStochasticModel) source);
//		}

	}

	@Value.Immutable(copy=false)
	public interface PhenomenologicalModel extends InHostConfiguration {
		
		public static PhenomenologicalModel DEFAULT = ImmutablePhenomenologicalModel.builder()
				
				.setSymptomCutoff(0.5)
				.setInfectiousnessCutoff(0.2)
				
				.setIncubationPeriod(SimpleDistribution.logNorm(5D, 2D))
				.setApproxPeakViralLoad( SimpleDistribution.unimodalBeta(0.5, 0.1))
				.setIncubationToPeakViralLoadDelay(SimpleDistribution.logNorm(2D, 1D))
				.setPeakToRecoveryDelay(SimpleDistribution.logNorm(8D,3D))
				
				.setApproxPeakImmuneResponse( SimpleDistribution.unimodalBeta(0.5, 0.1))
				.setImmuneWaningHalfLife(SimpleDistribution.logNorm(300D, 10D))
				.setPeakImmuneResponseDelay( SimpleDistribution.logNorm(20D, 4D) )
				
				.build();
		
		double getSymptomCutoff();
		double getInfectiousnessCutoff();
		
		SimpleDistribution getIncubationPeriod();
		SimpleDistribution getApproxPeakViralLoad();
		SimpleDistribution getIncubationToPeakViralLoadDelay();
		SimpleDistribution getPeakToRecoveryDelay();
		
		SimpleDistribution getApproxPeakImmuneResponse();
		SimpleDistribution getPeakImmuneResponseDelay();
		SimpleDistribution getImmuneWaningHalfLife();
		
		public default InHostPhenomenologicalState initialise(Person person, Sampler rng) {
			InHostConfiguration config = person.getOutbreak().getExecutionConfiguration().getInHostConfiguration();
			int time = person.getOutbreak().getCurrentState().getTime();
			return InHostPhenomenologicalState.initialise((PhenomenologicalModel) config, rng, time);
		}
		
//		public static ImmutablePhenomenologicalModel copyOf(InHostConfiguration source) {
//			return ConfigMerger.INSTANCE.mapper((ImmutablePhenomenologicalModel) source);
//		}

	}

	public default InHostModelState<?> initialise(Person person, Sampler rng) {
		throw new RuntimeException("Should be using the subtype implementations");
	};

	public static InHostModelState<?> initialise(InHostConfiguration config, Sampler rng, int time) {
		InHostModelState<?> state;
		if (config instanceof InHostConfiguration.StochasticModel s) {
			state = InHostStochasticState.initialise(s, rng, 0);
		} else if (config instanceof PhenomenologicalModel s) {
			state = InHostPhenomenologicalState.initialise(s, rng, 0);
		} else {
			throw new RuntimeException("Undefined model");
		}
		return state;
	}
	
	/**
	 *  Infectivity profile assumes a contact has occurred and it is the
	 *  conditional probability of transmission on that day versus any other 
	 *  particular day. This is controlled in real life by things like 
	 *  symptoms and behaviour, but in theory that is controlled for by the 
	 *  condition that transmission has occurred. This is the difference between
	 *  the generation time, and the effective generation time, and parallels
	 *  R0 and Rt. This is effectively G0 not Gt, and is determined only by the 
	 *  average viral load in a naive host, following a standard exposure.
	 * @param config
	 * @param samples
	 * @param duration
	 * @return
	 */
	public static DelayDistribution getInfectivityProfile(InHostConfiguration config, int samples, int duration) {
		Sampler rng = Sampler.getSampler();
		double[] infectivity = new double[duration];
		for (int n=0; n<= samples; n++) {
			 
			InHostModelState<?> state = InHostConfiguration.initialise(config, rng, 0);
			for (int i=0; i<duration; i++ ) {
				state = state.update( rng, i == 0 ? 1D : 0D, // viralExposure
								0);
				infectivity[i] = infectivity[i] + state.getNormalisedViralLoad();
			}
		}
		double[] cumulative = new double[duration];
		
		for (int i =0; i<duration; i++ ) {
			infectivity[i] = infectivity[i]/samples;
			cumulative[i] = infectivity[i] + (i==0 ? 0 : cumulative[i-1]);
			 
		}
		int cutoff = 0;
		double average = 0;
		for (int i =0; i<duration; i++ ) {
			if (cumulative[i]/cumulative[duration-1] > LIMIT) {
				cutoff = i;
				
				break;
			}
			average += infectivity[i]*i;
		}
		average = average/cutoff;
		log.debug("Serial interval "+LIMIT+" limit: "+cutoff+"; mean duration: "+average);
		return DelayDistribution.unnormalised(Arrays.copyOfRange(infectivity, 0, cutoff));
	}
	
	/**
	 * For a configuration gets an average viral load profile. This is not a 
	 * probability. A linear function of this defines the probability of 
	 * transmission but this needs to be calibrated to get a population R0.
	 * The connection between viral load and infectivity profile is actually 
	 * a hazard function. 
	 * @param config
	 * @param samples
	 * @param duration
	 * @return
	 */
	public static double[] getViralLoadProfile(InHostConfiguration config, int samples, int duration) {
		Sampler rng = Sampler.getSampler();
		double[] load = new double[duration];
		for (int n=0; n<= samples; n++) {
			 
			InHostModelState<?> state = InHostConfiguration.initialise(config, rng, 0);
			for (int i =0; i<duration; i++ ) {
				state = state.update( rng, i == 1 ? 1D : 0D, // viralExposure
								0);
				load[i] = load[i]+state.getNormalisedViralLoad();
			}
		}
		for (int i =0; i<duration; i++ ) {
			load[i] = load[i]/samples;
		}
		return load;
	}
	
//	public static InHostConfiguration copyOf(InHostConfiguration source) {
//		return ConfigMerger.INSTANCE.mapper(source);
//	}
	
	
	
}
