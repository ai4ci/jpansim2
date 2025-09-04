package io.github.ai4ci.config.inhost;

import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.OutbreakBaseline;
import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.HistogramDistribution;
import io.github.ai4ci.util.Sampler;

@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonSubTypes({ @Type(value = ImmutableStochasticModel.class, name = "stochastic"),
		@Type(value = ImmutablePhenomenologicalModel.class, name = "phenomenological"),
		@Type(value = ImmutableMarkovStateModel.class, name = "markov"),
		@Type(value = PartialStochasticModel.class, name = "stochastic.modifier"),
		@Type(value = PartialPhenomenologicalModel.class, name = "phenomenological.modifier"),
		@Type(value = PartialMarkovStateModel.class, name = "markov.modifier") })
public interface InHostConfiguration extends Serializable {

	static Logger log = LoggerFactory.getLogger(InHostConfiguration.class);

	static final double LIMIT = 0.999;

	
	
	private static double[][] transmissionFromLoad(double[][] viralLoad, double transmissionParameter) {
		int samples = viralLoad.length;
		int duration = viralLoad[0].length;
		double[][] trans = new double[samples][duration];
		for (int n = 0; n < samples; n++) {
			for (int i = 0; i < duration; i++) {
				trans[n][i] = 
						OutbreakBaseline.transmissibilityFromViralLoad(	
								viralLoad[n][i],
								transmissionParameter
						);
			}
		}
		return trans;
	}
	
	/**
	 * Infectivity profile assumes a contact has occurred and it is the conditional
	 * probability of transmission on that day versus any other particular day. This
	 * is controlled in real life by things like symptoms and behaviour, but in
	 * theory that is controlled for by the condition that transmission has
	 * occurred. This is the difference between the generation time, and the
	 * effective generation time, and parallels R0 and Rt. This is effectively G0
	 * not Gt, and is determined only by the average viral load in a naive host,
	 * following a standard exposure.
	 * 
	 * There is a question as to whether the infectivity profile is solely dependent
	 * on viral load, or there is an element of contact behaviour in here as well.
	 * If the latter then repeated contacts would make viral load a haxard function
	 * of generation time, as people with multiple exposures eventually get
	 * infected. This would tend to make the GT look shorter than the infectious
	 * period.
	 * 
	 * The infectivity profile is the calibrated transmission probability for 
	 * the average case conditioned on a transmission occurring. Although this 
	 * is calculated it is not really used internally, except to calculate an
	 * estimate of $R_t$, and to define the true length of the infective period. 
	 *
	 */
	public static DelayDistribution getInfectivityProfile(
			ExecutionConfiguration execConfig,
			double transmissionParameter,
			int samples, int duration) {
		return getInfectivityProfile(
				getViralLoadProfile(execConfig, samples, duration),
				transmissionParameter
		);
	}
	
	public static DelayDistribution getInfectivityProfile(
			double[][] viralLoad,
			double transmissionParameter
		) {
		
		int samples = viralLoad.length;
		int duration = viralLoad[0].length;
		
		double[][] trans = transmissionFromLoad(viralLoad,transmissionParameter);
		// Sample (1st dimension: samples wise ) average to get transmission probability array
		double[] meanTrans = new double[duration];
		for (int i=0; i<duration; i++) {
			for (int n = 0; n < samples; n++) {
				meanTrans[i] += trans[n][i]/samples;
			}
		}
		DelayDistribution tmp = DelayDistribution.unnormalised( 
				DelayDistribution.trimTail(meanTrans, 1-LIMIT, false)
				); 
		log.debug("Serial interval " + LIMIT + " limit: " + tmp.size() + "; mean duration: " + tmp.expected());
		return tmp;
	}

	/**
	 * For a configuration gets an average viral load profile. This is not a
	 * probability. A linear function of this defines the probability of
	 * transmission but this needs to be calibrated to get a population R0. The
	 * connection between viral load and infectivity profile is actually a hazard
	 * function.
	 * @return an array with sample as first dimension and time and second dimension
	 */
	public static double[][] getViralLoadProfile(ExecutionConfiguration execConfig,
			int samples, int duration) {
		//TODO: need to switch this to Stream<double[]> and calculate transmission
		// from viral load for each profile seperately because the average tends
		// to underrepresent when cut off of 1 is applied later. Interestingly 
		// this probably is why it used to work as the cutoff was before averaging.
		InHostConfiguration config = execConfig.getInHostConfiguration();
		
		Sampler rng = Sampler.getSampler();
		double[][] load = new double[samples][duration];
		for (int n = 0; n < samples; n++) {
			InHostModelState<?> state = InHostModelState.test(config, execConfig, rng);
			for (int i = 0; i < duration; i++) {
				state = state.update(rng, i == 1 ? 1D : 0D, // viralExposure
						0);
				load[n][i] = state.getNormalisedViralLoad();
			}
		}
		return load;
	}

	/**
	 * Determine the statistical distribution of maximum severity in a homogenous
	 * population, exposed with unit exposure.
	 * 
	 * @return an empirical distribution
	 */
	public static EmpiricalDistribution getPeakSeverity(InHostConfiguration config, ExecutionConfiguration execConfig,
			int samples, int duration) {
		Sampler rng = Sampler.getSampler();
		double[] x = new double[samples];
		for (int i = 0; i < samples; i++) {

			InHostModelState<?> state = InHostModelState.test(config, execConfig, rng);
			double max = 0;
			for (int j = 0; j < duration; j++) {
				state = state.update(rng, j == 0 ? 1D : 0D, // viralExposure
						0);
				if (state.getNormalisedSeverity() > max)
					max = state.getNormalisedSeverity();
			}
			x[i] = max;
		}

		return EmpiricalDistribution.fromData(x);
	}

	default double getSeveritySymptomsCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return Calibration.inferSeverityCutoff(outbreak, configuration.getInfectionCaseRate());
	}

	/**
	 * lets say 40% asymptomatic and case hosp rate of 10%. The IHR overall is 10%
	 * of the 60% symptomatic, so 6% The cutoff is the people that don;t get
	 * hospitalised so 94% quantile.
	 */
	default double getSeverityHospitalisationCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return Calibration.inferSeverityCutoff(outbreak, configuration.getInfectionHospitalisationRate());
	}

	default double getSeverityFatalityCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return Calibration.inferSeverityCutoff(outbreak, configuration.getInfectionFatalityRate());
	}

	public static DelayDistribution getSeverityProfile(ExecutionConfiguration execConfig,
			int samples, int duration) {
		InHostConfiguration config = execConfig.getInHostConfiguration();
		Sampler rng = Sampler.getSampler();
		double[] symptom = new double[duration];
		for (int n = 0; n <= samples; n++) {

			InHostModelState<?> state = InHostModelState.test(config, execConfig, rng);
			for (int i = 0; i < duration; i++) {
				state = state.update(rng, i == 0 ? 1D : 0D, // viralExposure
						0);
				symptom[i] = symptom[i] + state.getNormalisedSeverity();
			}
		}
		double[] cumulative = new double[duration];

		for (int i = 0; i < duration; i++) {
			symptom[i] = symptom[i] / samples;
			cumulative[i] = symptom[i] + (i == 0 ? 0 : cumulative[i - 1]);

		}
		int cutoff = 0;
		double average = 0;
		for (int i = 0; i < duration; i++) {
			if (cumulative[i] / cumulative[duration - 1] > LIMIT) {
				cutoff = i;

				break;
			}
			average += symptom[i] * i;
		}
		average = average / cutoff;
		log.debug("Symptom distribution " + LIMIT + " limit: " + cutoff + "; mean duration: " + average);
		return DelayDistribution.unnormalised(Arrays.copyOfRange(symptom, 0, cutoff));
	}

	

}
