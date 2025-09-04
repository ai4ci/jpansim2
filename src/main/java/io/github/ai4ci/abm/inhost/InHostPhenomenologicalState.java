package io.github.ai4ci.abm.inhost;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

/**
 * A phenomenological in-host model that tracks viral load and immune activity over time,
 * supporting multiple exposures (e.g., reinfection or vaccination).
 *
 * <p>The model uses a <i>biphasic logistic function</i> to describe the rise and fall of viral load
 * and immune response following each exposure. It supports superposition of multiple exposure events,
 * where each contributes independently to the overall viral load and immunity.
 *
 * <p>The total viral load is computed as the <i>complementary probability</i> of no viral presence
 * from any exposure:
 * \[
 * V(t) = 1 - \prod_{e \in \text{exposures}} \left(1 - v_e(t)\right)
 * \]
 * where \( v_e(t) \) is the viral load contribution from exposure \( e \), modeled using a shifted
 * biphasic logistic curve. This formulation assumes independent contributions and corresponds to
 * the probability that at least one exposure is active at time \( t \).
 *
 * <p>Similarly, immune activity is aggregated as:
 * \[
 * I(t) = 1 - \prod_{e \in \text{exposures}} \left(1 - i_e(t)\right)
 * \]
 * where \( i_e(t) \) is the immune response triggered by exposure \( e \).
 *
 * <p>This approach naturally captures:
 * <ul>
 *   <li>Boosting of immunity from repeated exposures,</li>
 *   <li>Sub-threshold infections that contribute to immunity without causing disease,</li>
 *   <li>Waning immunity over time.</li>
 * </ul>
 *
 * <p>The model is parameterized using observable clinical features (onset time, peak delay, etc.)
 * and calibrated via logistic regression on key points of the curve.
 *
 * @see ExposureModel
 * @see BiPhasicLogistic
 */
@Value.Immutable
public interface InHostPhenomenologicalState extends InHostModelState<PhenomenologicalModel> {
	
	static Logger log = LoggerFactory.getLogger(InHostPhenomenologicalState.class);
	
	BiPhasicLogistic getViralLoadModel();
	BiPhasicLogistic getImmunityModel();
	@Value.Redacted List<ExposureModel> getExposures();
	double getInfectiousnessCutoff();
	int getTime();
	
	/**
	 * Computes the total viral load at the current time step by combining contributions
	 * from all prior exposures using probabilistic superposition.
	 *
	 * <p>Each exposure \( e \) contributes a viral load curve \( v_e(t) \), which is zero before
	 * the exposure time and follows a biphasic logistic function afterward. The overall viral load
	 * is computed as the complement of the probability that <i>none</i> of the exposures are active:
	 * \[
	 * V(t) = 1 - \prod_{e} \left(1 - v_e(t)\right)
	 * \]
	 * This formulation assumes independence between exposure events and corresponds to the union
	 * of their individual effects.
	 *
	 * @return the combined viral load at the current time, in normalized units
	 */
	@Value.Derived default double getViralLoad() {
		return 1 - this.getExposures().stream()
		.mapToDouble(e -> e.getExposureViralLoad(getTime(), getViralLoadModel()))
		.map(vl -> 1-vl)
		.reduce((d1,d2) -> d1*d2)
		.orElse(1);
	}
	
	/**
	 * Computes the total immune activity at the current time by aggregating responses
	 * from all prior exposures.
	 *
	 * <p>Similar to viral load, immune activity is modeled as:
	 * \[
	 * I(t) = 1 - \prod_{e} \left(1 - i_e(t)\right)
	 * \]
	 * where \( i_e(t) \) is the immune response from exposure \( e \), modeled using a logistic growth
	 * and decay process. This captures immune boosting and partial protection from sub-infectious doses.
	 *
	 * <p>The immune response is not reset by new infections; instead, it accumulates,
	 * reflecting immunological memory.
	 *
	 * @return the combined immune activity at the current time, in normalized units
	 */
	@Value.Derived default double getImmuneActivity() {
		return 1-this.getExposures().stream()
		.mapToDouble(e -> e.getExposureImmuneActivity(getTime(), getImmunityModel()))
		.map(vl -> 1-vl)
		.reduce((d1,d2) -> d1*d2)
		.orElse(1);
	}
	
	/**
	 * Returns the viral load normalized relative to the infectiousness cutoff.
	 *
	 * <p>The normalization is defined as:
	 * \[
	 * V_{\text{norm}}(t) = \frac{V(t)}{V_{\text{cutoff}}}
	 * \]
	 * where \( V_{\text{cutoff}} \) is the minimum viral load required for infectiousness.
	 * This ratio is used in transmission models to compute transmissibility.
	 *
	 * @return normalized viral load relative to the infectiousness threshold
	 */
	default double getNormalisedViralLoad() {
		double tmp = Conversions.rateRatio( getViralLoad(), this.getInfectiousnessCutoff() );
		return tmp;
	};
	
	/**
	 * Computes a proxy for disease severity as the maximum viral load across all active exposures.
	 *
	 * <p>In this phenomenological model, severity is assumed to be driven by the peak viral load
	 * from any single exposure event:
	 * \[
	 * S(t) = \max_{e} v_e(t)
	 * \]
	 * This reflects the idea that clinical outcomes are dominated by the most severe infection event,
	 * even in the presence of multiple exposures.
	 *
	 * @return the highest viral load contribution from any single exposure at current time
	 */
	default double getNormalisedSeverity() {
		// In this model the viral load is the same as severity, but there is a 
		// question as to how to combine multiple exposures.
		
		return this.getExposures().stream()
				.mapToDouble(e -> e.getExposureViralLoad(getTime(), getViralLoadModel()))
				.max()
				.orElse(0);
	};

	/**
	 * Advances the in-host state by one time step, updating exposures and immune/viral dynamics.
	 *
	 * <p>The update process:
	 * <ol>
	 *   <li>Removes exposures that are no longer relevant (immune response has waned below 1%),</li>
	 *   <li>Processes a new exposure (if any), scaling the initial viral dose by host susceptibility,</li>
	 *   <li>Creates a new {@link ExposureModel} with appropriate growth and decay offsets,</li>
	 *   <li>Increments the time counter.</li>
	 * </ol>
	 *
	 * <p>The new exposure's initial viral load is:
	 * \[
	 * v_0 = \text{Conversions.scaleProbabilityByRR}(V_{\text{cutoff}}, \text{virionDose})
	 * \]
	 * which adjusts the baseline infectious dose by the exposure intensity.
	 *
	 * @param sampler unused in this deterministic model (reserved for stochastic extensions)
	 * @param virionDose the magnitude of new viral exposure (relative to infectious dose)
	 * @param immunisationDose currently unused; reserved for explicit vaccine modeling
	 * @return a new state advanced by one time unit with updated exposures
	 */
	default InHostPhenomenologicalState update(Sampler sampler, double virionDose, double immunisationDose) { //, double immuneModifier) {
		// Overall modifiers are a number around 1:
		// Double hostImmunity = immuneModifier;
		
		double virionsDx = this.getInfectiousnessCutoff();
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
	
	/**
	 * Represents a single exposure event (infection or vaccination) that contributes to
	 * viral load and immune response.
	 *
	 * <p>Each exposure is characterized by:
	 * <ul>
	 *   <li>Timing: when it occurred,</li>
	 *   <li>Dose: normalized exposure level,</li>
	 *   <li>Immune context: immune activity at time of exposure,</li>
	 *   <li>Temporal offsets: shifts in the growth and decay phases of the response.</li>
	 * </ul>
	 *
	 * <p>The viral load from an exposure at time \( t \) is:
	 * \[
	 * v_e(t) = f(t - t_e + g_e; r_g, s_g) \cdot \left(1 - f(t - t_e + d_e; r_d, s_d)\right)
	 * \]
	 * where:
	 * <ul>
	 *   <li> \( t_e \): exposure time,</li>
	 *   <li> \( g_e \): growth phase offset,</li>
	 *   <li> \( d_e \): decay phase offset,</li>
	 *   <li> \( f(x; r, s) = \frac{1}{1 + \exp(-r(x - s))} \): logistic function,</li>
	 *   <li> \( r_g, s_g \): growth rate and midpoint,</li>
	 *   <li> \( r_d, s_d \): decay rate and midpoint.</li>
	 * </ul>
	 *
	 * <p>The immune response is modeled similarly but without the decay modulation.
	 */
	@Value.Immutable
	public static interface ExposureModel extends Serializable {
		
		double MIN_IMMUNE_ACTIVITY = 0.001D;
			
		public int getExposureTime();
		public double getNormalisedExposure();
		public double getImmuneActivityAtExposure();
		double getGrowthOffset();
		double getDecayOffset();
		
		/**
		 * Computes the viral load contribution of this exposure at a given time.
		 *
		 * <p>If \( t \leq t_e \), returns 0. Otherwise:
		 * \[
		 * v_e(t) = f(t - t_e + g_e; r_g, s_g) \cdot \left(1 - f(t - t_e + d_e; r_d, s_d)\right)
		 * \]
		 * This biphasic form models rapid viral growth followed by immune-mediated clearance.
		 *
		 * @param time the current time step
		 * @param model the biphasic logistic model defining growth and decay dynamics
		 * @return the viral load from this exposure at the given time
		 */
		default double getExposureViralLoad(int time, BiPhasicLogistic model) {
			if (time <= this.getExposureTime()) return 0;
			return model.adjusted(
					time-getExposureTime(), 
					getGrowthOffset(), getDecayOffset());
		}
		
		/**
		 * Computes the immune activity contribution of this exposure at a given time.
		 *
		 * <p>If \( t \leq t_e \), returns 0. Otherwise:
		 * \[
		 * i_e(t) = f(t - t_e; r_g, s_g) \cdot \left(1 - f(t - t_e; r_d, s_d)\right)
		 * \]
		 * This represents the unadjusted immune trajectory initiated by the exposure.
		 *
		 * @param time the current time step
		 * @param model the biphasic logistic model for immune dynamics
		 * @return the immune activity from this exposure at the given time
		 */
		default double getExposureImmuneActivity(int time, BiPhasicLogistic model) {
			if (time <= this.getExposureTime()) return 0;
			return model.unadjusted(time-getExposureTime()); 
		}
		
		default boolean isIrrelevant(int time, BiPhasicLogistic model) {
			return this.getExposureTime() + 
					BiPhasicLogistic.invF(0.99,model.getDecayRate(),model.getDecayTime()) < time;
		};

		/**
		 * Factory method to create a new exposure with calibrated temporal offsets.
		 *
		 * <p>The growth offset \( g_e \) is computed so that the viral load at time 0 of the exposure
		 * matches the scaled dose:
		 * \[
		 * g_e = t_g - \frac{1}{r_g} \log\left(\frac{1 - v_0}{v_0}\right)
		 * \]
		 * where \( v_0 = \text{dose} \cdot \text{unit} \), and \( \text{unit} = f(0; r_g, s_g) \).
		 *
		 * <p>The decay offset \( d_e \) is set to ensure the immune activity at exposure matches
		 * the current level (but never shifted backward):
		 * \[
		 * d_e = \max\left(0, t_d - \frac{1}{r_d} \log\left(\frac{1 - I(t)}{I(t)}\right)\right)
		 * \]
		 *
		 * @param exposure normalized exposure dose (relative to infectious dose)
		 * @param immuneActivity current immune activity at time of exposure
		 * @param viralLoad model defining the shape of the response
		 * @param time time of exposure
		 * @return a new exposure model with calibrated offsets
		 */
		public static ExposureModel createExposure(double exposure, double immuneActivity, BiPhasicLogistic viralLoad, int time) {
			
			double dose = exposure * viralLoad.getUnit();
			
			// Shift the growth curve backwards such that the growth curve starts at the dose
			// at time zero. For exposures <1 this will be a shift forward and a 
			// negative number
			double growthOffset = BiPhasicLogistic.invF(dose, 
					viralLoad.getGrowthRate(), 
					viralLoad.getGrowthTime());
			
			// Shift the decay curve such that the decay component is the same as
			// the current immune activity. Never shift it forwards
			double decayOffset = Math.max(0,BiPhasicLogistic.invF(
					immuneActivity, 
					viralLoad.getDecayRate(), 
					viralLoad.getDecayTime()));
			
			return ImmutableExposureModel.builder()
				.setDecayOffset(decayOffset)
				.setExposureTime(time)
				.setGrowthOffset(growthOffset)
				.setImmuneActivityAtExposure(immuneActivity)
				.setNormalisedExposure(exposure)
				.build();
		}
		
	}
	
	/**
	 * A biphasic logistic model for viral load or immune response dynamics.
	 *
	 * <p>The response is modeled as the product of a logistic growth phase and a logistic decay phase:
	 * \[
	 * y(t) = f(t; r_g, s_g) \cdot \left(1 - f(t; r_d, s_d)\right)
	 * \]
	 * where:
	 * <ul>
	 *   <li> \( f(t; r, s) = \frac{1}{1 + \exp(-r(t - s))} \) is the logistic function,</li>
	 *   <li> \( r_g, s_g \): growth rate and midpoint,</li>
	 *   <li> \( r_d, s_d \): decay rate and midpoint.</li>
	 * </ul>
	 *
	 * <p>This produces a unimodal curve with controllable rise, peak, and fall times.
	 * The model supports calibration from clinical data (onset, peak, duration).
	 */
	@Value.Immutable
	public static interface BiPhasicLogistic extends Serializable {
		
		double getGrowthRate();
		double getGrowthTime();
		double getDecayRate();
		double getDecayTime();
		
		@Value.Derived default double getUnit() {
			return f(0, getGrowthRate(), getGrowthTime());
		}
		
		/**
		 * Computes the logistic rate \( r \) given two points \( (x_1, y_1) \), \( (x_2, y_2) \):
		 * \[
		 * r = \frac{\log\left(\frac{1 - y_2}{y_2}\right) - \log\left(\frac{1 - y_1}{y_1}\right)}{x_1 - x_2}
		 * \]
		 * This is derived from the inverse logistic formula.
		 *
		 * @param x1 first time point
		 * @param y1 first value (0 \lt y1 \lt 1)
		 * @param x2 second time point
		 * @param y2 second value (0 \lt y2 \lt 1)
		 * @return the rate parameter \( r \gt 0 \)
		 * @throws RuntimeException if the computed rate is non-positive
		 */
		public static double rate(double x1,double y1,double x2,double y2) {
			double tmp = (log((1-y2)/y2) - log((1-y1)/y1))/(x1 - x2);
			if (tmp < 0) {
				throw new RuntimeException();
			}
			return tmp;
		}
		
		public static double time(double x1,double y1,double x2,double y2) {
			double r = rate(x1,y1,x2,y2);
			return 1/r * log((1-y1)/y1) + x1;
		}
		
		/**
		 * Logistic function: \( f(x; r, s) = \frac{1}{1 + \exp(-r(x - s))} \)
		 *
		 * @param x input value
		 * @param r growth/decay rate
		 * @param s midpoint (inflection point)
		 * @return logistic output in (0,1)
		 */
		private static double f(double x, double r, double s) {
			return 1/(1+exp(-r*(x-s)));
		}
		
		/**
		 * Inverse logistic function: solves \( f(x; r, s) = y \) for \( x \):
		 * \[
		 * x = s - \frac{1}{r} \log\left(\frac{1 - y}{y}\right)
		 * \]
		 *
		 * @param y target output (0 \lt y \lt 1)
		 * @param r rate parameter
		 * @param s midpoint
		 * @return the input \( x \) that yields output \( y \)
		 */
		public static double invF(double y, double r, double s) {
			return s - log((1-y)/y) / r;
		}
		
		/**
		 * Computes the biphasic logistic response with shifted growth and decay phases:
		 * \[
		 * y(x) = f(x; r_g, s_g - g) \cdot \left(1 - f(x; r_d, s_d - d)\right)
		 * \]
		 * where \( g \) and \( d \) are offsets that shift the curve in time.
		 *
		 * @param x time since exposure
		 * @param growthOffset shift in growth phase
		 * @param decayOffset shift in decay phase
		 * @return the adjusted response value
		 */
		default double adjusted(double x, double growthOffset, double decayOffset) {
			return f(x, this.getGrowthRate(), this.getGrowthTime()-growthOffset) * 
				(1-f(x, this.getDecayRate(), this.getDecayTime()-decayOffset));
		}
		
		/**
		 * Calibrates a biphasic logistic model for viral load using clinical parameters:
		 *
		 * <ul>
		 *   <li> \( t_{\text{onset}} \): time of symptom onset,</li>
		 *   <li> \( \Delta t_{\text{peak}} \): delay to peak viral load,</li>
		 *   <li> \( \Delta t_{\text{post}} \): duration from peak to recovery threshold,</li>
		 *   <li> \( y_{\text{threshold}} \): threshold for detectable virus,</li>
		 *   <li> \( y_{\text{peak}} \): peak viral load (square-root transformed).</li>
		 * </ul>
		 *
		 * The square root of \( y_{\text{peak}} \) is used to stabilize the logistic fit.
		 *
		 * @return a calibrated {@link BiPhasicLogistic} model
		 */
		public static BiPhasicLogistic calibrateViralLoad(
				double onsetTime, double peakDelay, double postPeakDuration, 
				double thresholdLevel, double peakLevel
			) {
			peakLevel = sqrt(peakLevel);
			return ImmutableBiPhasicLogistic.builder()
				.setGrowthRate(BiPhasicLogistic.rate(onsetTime,thresholdLevel, onsetTime+peakDelay, peakLevel))
				.setGrowthTime(BiPhasicLogistic.time(onsetTime,thresholdLevel, onsetTime+peakDelay, peakLevel))
				.setDecayRate(BiPhasicLogistic.rate( onsetTime+peakDelay, 1-peakLevel, onsetTime+peakDelay+postPeakDuration, 1-thresholdLevel))
				.setDecayTime(BiPhasicLogistic.time( onsetTime+peakDelay, 1-peakLevel, onsetTime+peakDelay+postPeakDuration, 1-thresholdLevel))
				.build();
		}
		
		/**
		 * Calibrates a biphasic logistic model for immune activity:
		 *
		 * <ul>
		 *   <li> \( t_{\text{peak}} \): time of peak immune response,</li>
		 *   <li> \( I_{\text{peak}} \): peak immune level,</li>
		 *   <li> \( t_{\text{half}} \): half-life of immune activity.</li>
		 * </ul>
		 *
		 * The decay phase is calibrated from \( I_{\text{peak}} \) to \( I_{\text{peak}}/2 \)
		 * over \( t_{\text{half}} \) days.
		 *
		 * @return a calibrated {@link BiPhasicLogistic} model for immune dynamics
		 */
		public static BiPhasicLogistic calibrateImmuneActivity(
				double peakTime, double peakLevel, double halfLife
			) {
			double halfPeakLevel =peakLevel/2;
			peakLevel = sqrt(peakLevel);
			return ImmutableBiPhasicLogistic.builder()
				.setGrowthRate(BiPhasicLogistic.rate(0,0.01, peakTime, peakLevel))
				.setGrowthTime(BiPhasicLogistic.time(0,0.01, peakTime, peakLevel))
				.setDecayRate(BiPhasicLogistic.rate(peakTime, 1-peakLevel, peakTime+halfLife, 1-halfPeakLevel))
				.setDecayTime(BiPhasicLogistic.time(peakTime, 1-peakLevel, peakTime+halfLife, 1-halfPeakLevel))
				.build();
		}
		
		/**
		 * Computes the unadjusted biphasic logistic response at time \( x \):
		 * \[
		 * y(x) = f(x; r_g, s_g) \cdot \left(1 - f(x; r_d, s_d)\right)
		 * \]
		 *
		 * @param x time since exposure
		 * @return the unshifted response value
		 */
		default double unadjusted(int x) {
			return f(x, this.getGrowthRate(), this.getGrowthTime()) * 
				(1-f(x, this.getDecayRate(), this.getDecayTime()));
		}
		
	}
	
}
