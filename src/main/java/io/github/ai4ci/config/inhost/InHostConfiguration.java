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
import io.github.ai4ci.config.PartialMarkovStateModel;
import io.github.ai4ci.config.PartialPhenomenologicalModel;
import io.github.ai4ci.config.PartialStochasticModel;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.functions.DelayDistribution;
import io.github.ai4ci.functions.EmpiricalDistribution;
import io.github.ai4ci.util.Sampler;

/**
 * Configuration for an in‑host model.
 *
 * <p>This interface is a common supertype for the various in‑host model
 * configurations. It is used to allow the execution configuration to reference
 * any in‑host model configuration, and to allow the initialiser to accept any
 * in‑host model configuration. It also provides some common utility methods for
 * working with in‑host models, such as calculating the infectivity profile and
 * severity profile.
 *
 * <p>Downstream uses include the initialiser
 * {@link io.github.ai4ci.flow.builders.DefaultInHostMarkovStateInitialiser} which
 * accepts an instance of this interface to build an individual's in‑host state.
 * It is also referenced by example code and tests.
 *
 * @author Rob Challen
 */
@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonSubTypes({ @Type(value = ImmutableStochasticModel.class, name = "stochastic"),
		@Type(value = ImmutablePhenomenologicalModel.class, name = "phenomenological"),
		@Type(value = ImmutableMarkovStateModel.class, name = "markov"),
		@Type(value = PartialStochasticModel.class, name = "stochastic.modifier"),
		@Type(value = PartialPhenomenologicalModel.class, name = "phenomenological.modifier"),
		@Type(value = PartialMarkovStateModel.class, name = "markov.modifier") })
public interface InHostConfiguration extends Serializable {

	/** logger for this class */
	static Logger log = LoggerFactory.getLogger(InHostConfiguration.class);

	/**
	 * The limit is the point at which the tail of the distribution is cut off. This
	 * is used to trim the infectivity profile and the severity profile to the point
	 * where 99.9% of transmission or severity has occurred. This is not critical as
	 * long as it is high enough to capture the full profile, but not too high to be
	 * inefficient. The profiles are trimmed to the point where 99.9% of
	 * transmission or severity has occurred, so this is not critical as long as it
	 * is high enough to capture the full profile, but not too high to be
	 * inefficient.
	 */
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
	 * Derived distribution determined by in host viral load model and overall
	 * transmission rate required to get desired R0. <br><br> 
	 * 
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
	 * If the latter then repeated contacts would make viral load a hazard function
	 * of generation time, as people with multiple exposures eventually get
	 * infected. This would tend to make the GT look shorter than the infectious
	 * period.
	 * 
	 * The infectivity profile is the calibrated transmission probability for 
	 * the average case conditioned on a transmission occurring. Although this 
	 * is calculated it is not really used internally, except to calculate an
	 * estimate of $R_t$, and to define the true length of the infective period. 
	 * 
	 * @param execConfig the configuration to get the viral load profile for. This is used to get the in host configuration, but also to get the infection case rate, hospitalisation rate and fatality rate to calculate the severity cutoffs for symptoms, hospitalisation and fatality by the downstream class {@link Calibration}.
	 * @param transmissionParameter the transmission parameter to use to calculate the infectivity profile. This
	 * @param samples the number of samples to use to get the average profile. This should be large enough to get a stable estimate of the profile, but not too large to be inefficient. The profile is trimmed to the point where 99.9% of transmission has occurred, so this is not critical as long as it is large enough.
	 * @param duration the number of time steps to calculate the profile for. This should be
	 * long enough to capture the full profile, but not too long to be inefficient. The profile is trimmed to the point where 99.9% of transmission has occurred, so this is not critical as long as it is long enough.
	 * @return a delay distribution with the average transmission probability at each time post exposure, averaged across all agents, and conditioned on a transmission occurring. The profile is trimmed to the point where 99.9% of transmission has occurred, so this is not critical as long as it is long enough.
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
	
	/**
	 * Calculate the infectivity profile from a viral load profile and a
	 * transmission parameter.
	 * 
	 * <p>
	 * This is used to calculate the infectivity profile from the viral load profile
	 * calculated by {@link #getViralLoadProfile(ExecutionConfiguration, int, int)},
	 * which is used to calibrate the transmission parameter to get the desired R0,
	 * and to calculate the infectivity profile. It is not used directly in the
	 * model, which calculates transmission from the viral load of each individual
	 * agent. The profile is trimmed to the point where 99.9% of transmission has
	 * occurred, so this is not critical as long as it is long enough.
	 * 
	 * @param viralLoad             the viral load profile to use to calculate the
	 *                              infectivity profile.
	 * @param transmissionParameter the transmission parameter to use to calculate
	 *                              the infectivity profile.
	 * @return the infectivity profile calculated from the viral load profile and
	 *         the transmission parameter.
	 */
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
	 * For a configuration gets an average viral load profile. This is used
	 * to calibrate the transmission parameter to get the desired R0, and to calculate
	 * the infectivity profile. It is not used directly in the model, which calculates
	 * transmission from the viral load of each individual agent.
	 * 
	 * This is used downstream in classes like {@link OutbreakBaseline} to calculate the transmission parameter
	 * to get the desired R0, and to calculate the infectivity profile, and in
	 * {@link Calibration} to calculate the severity cutoffs.
	 * 
	 * @param execConfig the configuration to get the viral load profile for
	 * @param samples the number of samples to use to get the average profile.
	 * @param duration the number of time steps to calculate the profile for. This should be long enough to capture the full profile, but not too long to be inefficient. The profile is trimmed to the point where 99.9% of transmission has occurred, so this is not critical as long as it is long enough.
	 * 
	 * @return an array with sample as first dimension and time post exposure and second dimension
	 * is the average viral load across all the agents at that time post exposure
	 */
	public static double[][] getViralLoadProfile(ExecutionConfiguration execConfig,
			int samples, int duration) {
		//TODO: need to switch this to Stream<double[]> and calculate transmission
		// from viral load for each profile separately because the average tends
		// to under represent when cut off of 1 is applied later. Interestingly 
		// this probably is why it used to work as the cutoff was before averaging.
		InHostConfiguration config = execConfig.getInHostConfiguration();
		
		Sampler rng = Sampler.getSampler();
		double[][] load = new double[samples][duration];
		for (int n = 0; n < samples; n++) {
			InHostModelState<?> state = InHostModelState.test(config, execConfig, rng);
			// viral exposure at t=0.
			// This is a standard unit dose.
			state = state.update(rng, 1D, 0);
			for (int i = 0; i < duration; i++) {
				load[n][i] = state.getNormalisedViralLoad();
				state = state.update(rng, 0, 0);
			}
		}
		return load;
	}

	/**
	 * Determine the statistical distribution of maximum severity in a homogenous
	 * population, exposed with unit exposure. This is used to determine the
	 * severity cutoffs for symptoms, hospitalisation and fatality by the downstream
	 * class {@link Calibration}.
	 * 
	 * @param config     the in host configuration to use to get the severity
	 *                   profile
	 * @param execConfig the execution configuration to use to get the severity
	 *                   profile. This is used to get the in host configuration, but
	 *                   also to get the infection case rate, hospitalisation rate
	 *                   and fatality rate to calculate the severity cutoffs for
	 *                   symptoms, hospitalisation and fatality by the downstream
	 *                   class {@link Calibration}.
	 * @param samples    the number of samples to use to get the empirical
	 *                   distribution.
	 * @param duration   the number of time steps to calculate the severity profile
	 *                   for. This should be long enough to capture the full
	 *                   profile, but not too long to be inefficient. The profile is
	 *                   trimmed to the point where 99.9% of severity has occurred,
	 *                   so this is not critical as long as it is long enough.
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
			state = state.update(rng, 1D, 0);
			for (int j = 0; j < duration; j++) {
				if (state.getNormalisedSeverity() > max)
					max = state.getNormalisedSeverity();
				state = state.update(rng, 0, 0);
			}
			x[i] = max;
		}

		return EmpiricalDistribution.fromData(x);
	}

	/**
	 * The cutoff is the severity level above which people get symptoms. This is
	 * calibrated to get the desired infection case rate (ICR) for the outbreak. The
	 * ICR is the proportion of all infected people that get symptoms, so it is the
	 * proportion of people above the cutoff. For example, if there are lets say 40%
	 * asymptomatic, the cutoff is the 60% quantile.
	 * 
	 * @param outbreak      the outbreak to get the severity cutoff for. This is
	 *                      used to get the infection case rate (ICR) to calibrate
	 *                      the cutoff to.
	 * @param configuration the execution configuration to get the severity cutoff
	 *                      for. This is used to get the infection case rate (ICR)
	 *                      to calibrate the cutoff to.
	 * @return the severity cutoff for symptoms. This is the severity level above
	 *         which people get symptoms, calibrated to get the desired infection
	 *         case rate (ICR) for the outbreak.
	 */
	default double getSeveritySymptomsCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return Calibration.inferSeverityCutoff(outbreak, configuration.getInfectionCaseRate());
	}

	/**
	 * The cutoff is the severity level above which people get hospitalised. This is
	 * calibrated to get the desired infection hospitalisation rate (IHR) for the
	 * outbreak. The IHR is the proportion of all infected people that get
	 * hospitalised, so it is the proportion of people above the cutoff. For
	 * example, if there are lets say 40% asymptomatic and case hosp rate of 10%.
	 * The IHR overall is 10% of the 60% symptomatic, so 6% The cutoff is the people
	 * that don;t get hospitalised so 94% quantile.
	 * 
	 * @param outbreak      the outbreak to get the severity cutoff for. This is
	 *                      used to get the infection hospitalisation rate (IHR) to
	 *                      calibrate the cutoff to.
	 * @param configuration the execution configuration to get the severity cutoff
	 *                      for. This is used to
	 * @return the severity cutoff for hospitalisation. This is the severity level
	 *         above which people get hospitalised, calibrated to get the desired
	 *         infection hospitalisation rate (IHR) for the outbreak.
	 */
	default double getSeverityHospitalisationCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return Calibration.inferSeverityCutoff(outbreak, configuration.getInfectionHospitalisationRate());
	}

	/**
	 * The cutoff is the severity level above which people get fatal outcomes.
	 * 
	 * <p>
	 * This is calibrated to get the desired infection fatality rate (IFR) for the
	 * outbreak. The IFR is the proportion of all infected people that get fatal
	 * outcomes, so it is the proportion of people above the cutoff.
	 * 
	 * @param outbreak      the outbreak to get the severity cutoff for.
	 * @param configuration the execution configuration to get the severity cutoff
	 *                      for.
	 * @return the severity cutoff for fatality. This is the severity level above
	 *         which people get hospitalised, calibrated to get the desired
	 *         infection fatality rate (IFR) for the outbreak.
	 */
	default double getSeverityFatalityCutoff(Outbreak outbreak, ExecutionConfiguration configuration) {
		return Calibration.inferSeverityCutoff(outbreak, configuration.getInfectionFatalityRate());
	}

	/**
	 * Get the severity profile for a homogenous population, exposed with unit
	 * exposure. This is used to determine the severity cutoffs for symptoms,
	 * hospitalisation and fatality by the downstream class {@link Calibration}. The
	 * severity profile is the average severity at each time post exposure, averaged
	 * across all agents. The profile is trimmed to the point where 99.9% of
	 * severity has occurred, so this is not critical as long as it is long enough.
	 * 
	 * @param execConfig the execution configuration to use to get the severity
	 *                   profile. This is used to get the in host configuration, but
	 *                   also to get the infection case rate, hospitalisation rate
	 *                   and fatality rate to calculate the severity cutoffs for
	 *                   symptoms, hospitalisation and fatality by the downstream
	 *                   class {@link Calibration}.
	 * @param samples    the number of samples to use to get the average profile.
	 *                   This should be large enough to get a stable estimate of the
	 *                   profile, but not too large to be inefficient. The profile
	 *                   is trimmed to the point where 99.9% of severity has
	 *                   occurred, so this is not critical as long as it is large
	 *                   enough.
	 * @param duration   the number of time steps to calculate the severity profile
	 *                   for. This should be long enough to capture the full
	 *                   profile, but not too long to be inefficient. The profile is
	 *                   trimmed to the point where 99.9% of severity has occurred,
	 *                   so this is not critical as long as it is long enough.
	 * @return a delay distribution with the average severity at each time post
	 *         exposure, averaged across all agents. The profile is trimmed to the
	 *         point where 99.9% of severity has occurred, so this is not critical
	 *         as long as it is long enough.
	 */
	public static DelayDistribution getSeverityProfile(ExecutionConfiguration execConfig,
			int samples, int duration) {
		InHostConfiguration config = execConfig.getInHostConfiguration();
		Sampler rng = Sampler.getSampler();
		double[] symptom = new double[duration];
		for (int n = 0; n <= samples; n++) {

			InHostModelState<?> state = InHostModelState.test(config, execConfig, rng);
			state = state.update(rng, 1D, 0D);
			for (int i = 0; i < duration; i++) {
				symptom[i] = symptom[i] + state.getNormalisedSeverity();
				state = state.update(rng, 0D, 0);
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
