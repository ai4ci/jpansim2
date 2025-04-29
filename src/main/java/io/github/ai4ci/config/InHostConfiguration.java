package io.github.ai4ci.config;

import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.abm.InHostModelState;
import io.github.ai4ci.abm.InHostPhenomenologicalState;
import io.github.ai4ci.abm.InHostStochasticState;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.Sampler;

@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes({
	@Type(ImmutableStochasticModel.class), 
	@Type(ImmutablePhenomenologicalModel.class)
})
public interface InHostConfiguration extends Serializable {

	static Logger log = LoggerFactory.getLogger(InHostConfiguration.class);
	
	static final double LIMIT = 0.99;
	
	public default InHostModelState<?> initialise(Person person, Sampler rng) {
		throw new RuntimeException("Should be using the subtype implementations");
	};

	public static InHostModelState<?> initialise(InHostConfiguration config, Sampler rng, int time) {
		InHostModelState<?> state;
		if (config instanceof StochasticModel) {
			StochasticModel s = (StochasticModel) config;
			state = InHostStochasticState.initialise(s, rng, 0);
		} else if (config instanceof PhenomenologicalModel) {
			PhenomenologicalModel s = (PhenomenologicalModel) config;
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
	 *  
	 *  There is a question as to whether the infectivity profile is solely
	 *  dependent on viral load, or there is an element of contact behaviour in 
	 *  here as well. If the latter then repeated contacts would make viral load
	 *  a haxard function of generation time, as people with multiple exposures
	 *  eventually get infected. This would tend to make the GT look shorter than
	 *  the infectious period.
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
