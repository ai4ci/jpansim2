package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.immutables.value.Value;
import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.mXparser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.functions.Coordinates.DuplicateResolution;

/**
 * A mathematical function that can be evaluated at runtime. This is used to
 * represent functions that are defined by the user as part of the configuration
 * of the model, such as the viral load to transmission probability mapping.
 *
 * <p>
 * The function is defined by a String expression containing `x` as the
 * parameter, which is parsed and evaluated using the mXparser library. The
 * parsed expression is derived from the original String and is not serialized,
 * ensuring that only the user-defined expression is stored in the
 * configuration.
 *
 * @see io.github.ai4ci.abm.OutbreakBaseline#transmissibilityFromViralLoad(double,
 *      double)
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMathematicalFunction.class)
@JsonDeserialize(as = ImmutableMathematicalFunction.class)
public interface MathematicalFunction extends Serializable, SimpleFunction {

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
	 * A univariate mathematical function as a String containing `x` as the
	 * parameter and returning a value of y on the link scale. Supported
	 * expressions are described here:
	 * https://mathparser.org/mxparser-math-collection
	 *
	 * @return the function expression on the link scale.
	 */
	String getFXExpression();

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
	 * A link function used to transform the Y coordinates after evaluation.
	 *
	 * @return the link function used to transform the Y coordinates
	 */
	@Value.Default
	default LinkFunction getLink() { return LinkFunction.NONE; }

	/**
	 * The maximum support of the expression
	 *
	 * @return the maximum support of the expression
	 */
	double getMaximum();

	/**
	 * The minimum support value of the expression
	 *
	 * @return the minimum support for the expression
	 */
	double getMinimum();

	/**
	 * The (x,y) coordinates defining the knot points.
	 *
	 * @return the X coordinates defining the knot points
	 */
	@Value.Derived
	default double[] getX() {
		return IntStream.rangeClosed(0, 100)
			.mapToDouble(
				i -> getMinimum() + i / 100.0 * (getMaximum() - getMinimum())
			)
			.toArray();
	};

	/**
	 * The (x,y) coordinates defining the knot points.
	 *
	 * @return the Y coordinates defining the knot points (true scale)
	 */
	@Value.Derived
	default double[] getY() {
		var xArg = new Argument("x");
		mXparser.disableUlpRounding();
		mXparser.disableCanonicalRounding();
		mXparser.disableAlmostIntRounding();
		var exp = new Expression(this.getFXExpression(), xArg);
		return Arrays.stream(getX())
			.map(x -> {
				exp.setArgumentValue("x", x);
				return exp.calculate();
			})
			// FX is on the link scale. We need to calculate the true scale for
			// setting coordinates object (in getInterpolator())
			// This is a bit inefficient but only happens once.
			.map(y -> getLink().invFn(y))
			.toArray();
	}

	/**
	 * Evaluate the interpolated mathematical expression at a given X value.
	 * <p>
	 * This method computes the value of the expression at a given X by
	 * interpolating the Y values at the coordinates and then applying the
	 * inverse link function to the interpolated value. The result is the value
	 * of the mathematical function at the specified X.
	 *
	 * @param x the X value at which to evaluate the expression
	 * @return the approximate value of the expression at the specified X
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
