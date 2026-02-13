package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.functions.Coordinates.DuplicateResolution;

/**
 * A simple empirical function defined by a set of (x,y) coordinates.
 *
 * <p>
 * This interface describes a simple empirical function defined by a set of
 * (x,y) coordinates. It includes parameters for interpolation and link function
 * transformations. Implementations are used when defining empirical functions
 * such as the age contact odds function in
 * {@link io.github.ai4ci.config.setup.AgeStratifiedDemography}.
 *
 * <p>
 * Downstream uses include any place where an empirical function is required,
 * such as the age contact odds function in
 * {@link io.github.ai4ci.config.setup.AgeStratifiedDemography}. The default
 * interpolation method is cubic spline, but other methods may be used if
 * desired.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalFunction.class)
@JsonDeserialize(as = ImmutableEmpiricalFunction.class)
public interface EmpiricalFunction extends Serializable, SimpleFunction {

	/**
	 * Compute the differential of the empirical function at a given X value.
	 * <p>
	 * This method computes the differential of the empirical function at a given
	 * X by applying the chain rule to the interpolated value and its derivative.
	 * It uses the derivative of the link function and the derivative of the
	 * interpolator to compute the overall differential at the specified X.
	 *
	 * @param x the X value at which to compute the differential of the empirical
	 *          function
	 * @return the differential of the empirical function at the specified X
	 */
	@Override
	default double differential(double x) {
		return this.getLink()
			.derivInvFn(
				this.getInterpolator()
					.interpolate(x)
			) * this.getInterpolator()
				.differential(x);

	}

	/**
	 * The interpolation method used to interpolate between the (x,y)
	 * coordinates.
	 *
	 * @return the interpolation method used to interpolate between the (x,y)
	 *         coordinates
	 */
	@Value.Default
	default Interpolators getInterpolationType() {
		return Interpolators.CUBIC_SPLINE;
	}

	/**
	 * The interpolator created from the coordinates and interpolation type.
	 * <p>
	 * This method constructs an interpolator based on the (x,y) coordinates and
	 * the specified interpolation type. It applies the link function to the Y
	 * coordinates before interpolation. The resulting interpolator can be used
	 * to compute interpolated values for any X within the range of the
	 * coordinates.
	 *
	 * @return the interpolator created from the coordinates and interpolation
	 *         type
	 */
	@JsonIgnore @Value.Derived
	default Interpolator getInterpolator() {

		Coordinates tmp = ImmutableCoordinates.builder()
			.setX(this.getX())
			.setY(this.getY())
			.setXMin(this.getMinimum())
			.setXMax(this.getMaximum())
			.setXLink(LinkFunction.NONE)
			.setYLink(this.getLink())
			.setIncreasing(false)
			.setResolveDuplicates(DuplicateResolution.MEAN)
			.build();

		return this.getInterpolationType()
			.create(tmp.getHx(), tmp.getHy());

	}

	/**
	 * The link function used to transform the Y coordinates before
	 * interpolation.
	 *
	 * @return the link function used to transform the Y coordinates before
	 *         interpolation
	 */
	@Value.Default
	default LinkFunction getLink() { return LinkFunction.NONE; }

	/**
	 * The maximum X value of the empirical function, derived from the
	 * coordinates.
	 *
	 * @return the maximum X value of the empirical function
	 */
	@Value.Default
	default double getMaximum() {
		return Arrays.stream(this.getX())
			.max()
			.getAsDouble();
	}

	/**
	 * The minimum X value of the empirical function, derived from the
	 * coordinates.
	 *
	 * @return the minimum X value of the empirical function
	 */
	@Value.Default
	default double getMinimum() {
		return Arrays.stream(this.getX())
			.min()
			.getAsDouble();
	}

	/**
	 * The (x,y) coordinates defining the empirical function.
	 *
	 * @return the X coordinates defining the empirical function
	 */
	double[] getX();

	/**
	 * The (x,y) coordinates defining the empirical function.
	 *
	 * @return the Y coordinates defining the empirical function
	 */
	double[] getY();

	/**
	 * Evaluate the empirical function at a given X value.
	 * <p>
	 * This method computes the value of the empirical function at a given X by
	 * interpolating the Y values at the coordinates and then applying the
	 * inverse link function to the interpolated value. The result is the value
	 * of the empirical function at the specified X.
	 *
	 * @param x the X value at which to evaluate the empirical function
	 * @return the value of the empirical function at the specified X
	 *
	 */
	@Override
	default double value(double x) {
		return this.getLink()
			.invFn(
				this.getInterpolator()
					.interpolate(Interpolator.squish(getMinimum(), x, getMaximum()))
			);
	}

}
