package io.github.ai4ci.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.ai4ci.util.Conversions;

/**
 * Annotation used to declare the intended scaling behaviour for a configuration
 * parameter accessor.
 *
 * <p>
 * Parameters annotated with {@code @Scale} indicate how a base value should be
 * transformed when an adjustment factor is applied. This is used by the
 * configuration execution code (for example in
 * {@code io.github.ai4ci.config.execution.DemographicAdjustment} and the
 * generated {@code PartialDemographicAdjustment} classes) to interpret
 * adjustment values consistently.
 * </p>
 *
 * <p>
 * The annotation can express three scaling strategies via the
 * {@link Scale.ScaleType} enum: ODDS, FACTOR and POWER (see
 * {@link ScaleType#scale(double,double,ScaleType)} for behaviour and formulae).
 * In common usage ODDS is appropriate when the parameter is a probability and
 * the adjustment should act on odds; FACTOR multiplies a numeric baseline;
 * POWER raises the baseline to a power. The implementation for the odds scaling
 * delegates to
 * {@link io.github.ai4ci.util.Conversions#scaleProbabilityByOR(double,double)}.
 * </p>
 *
 * @author Rob Challen
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scale {
	/**
	 * The available scaling strategies.
	 */
	enum ScaleType {
		/**
		 * Scale probabilities on the odds scale. Use this for parameters that
		 * represent probabilities so that multipliers act on odds rather than on
		 * raw probabilities. The computation performed by
		 * {@link ScaleType#scale(double,double,ScaleType)} delegates to
		 * {@link Conversions#scaleProbabilityByOR(double,double)}.
		 *
		 * <p>
		 * Formula (probability p, factor f):
		 * </p>
		 * \[ p' = \frac{f\cdot \dfrac{p}{1-p}}{1 + f\cdot \dfrac{p}{1-p}} \]
		 */
		ODDS,

		/**
		 * Multiply the base value by a factor. Suitable for rates or other
		 * continuous measures where a multiplicative change is required.
		 *
		 * <p>
		 * Formula (base b, factor f):
		 * </p>
		 * \[ b' = b \times f \]
		 */
		FACTOR,

		/**
		 * Raise the base value to the given power. Useful for elastic or
		 * non-linear transformations.
		 *
		 * <p>
		 * Formula (base b, exponent f):
		 * </p>
		 * \[ b' = b^{f} \]
		 */
		POWER;

		/**
		 * Apply the selected scaling to a numeric base value.
		 *
		 * <p>
		 * Behaviour:
		 * <ul>
		 * <li>{@link #FACTOR} returns {@code base * factor}.</li>
		 * <li>{@link #ODDS} applies odds scaling via
		 * {@link Conversions#scaleProbabilityByOR(double,double)}.</li>
		 * <li>{@link #POWER} returns {@code Math.pow(base, factor)}.</li>
		 * </ul>
		 *
		 * @param base   the base value to be scaled
		 * @param factor the adjustment factor or exponent
		 * @param scale  the scaling strategy to apply
		 * @return the scaled value
		 */
		public static double scale(double base, double factor, ScaleType scale) {
			switch (scale) {
			case FACTOR:
				return base * factor;
			case ODDS:
				return Conversions.scaleProbabilityByOR(base, factor);
			case POWER:
				return Math.pow(base, factor);
			}
			throw new RuntimeException();
		}
	}

	/**
	 * The scaling strategy to use. Defaults to {@link ScaleType#ODDS}.
	 *
	 * @return the scale type
	 */
	Scale.ScaleType value() default Scale.ScaleType.ODDS;
}