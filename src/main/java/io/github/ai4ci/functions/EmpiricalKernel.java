package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.functions.Coordinates.DuplicateResolution;

/**
 * An empirical kernel function.
 *
 * <p>This interface describes a kernel function defined by a set of discrete
 * points. It includes methods to compute the minimum and maximum x values, and
 * to construct an interpolator for the points. The interpolator is used to
 * compute the raw value of the kernel at any given x.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalKernel.class)
@JsonDeserialize(as = ImmutableEmpiricalKernel.class)
public interface EmpiricalKernel extends Serializable, KernelFunction {
 
	/**
	 * The x values of the kernel function.
	 *
	 * <p>These values are used to construct the interpolator and to compute the
	 * minimum and maximum x values. They should be provided in ascending order
	 * for consistency, but the implementation will handle unsorted input.
	 * @return an array of integers representing the x values of the kernel function.
	 */
	int[] getT();
	
	/**
	 * The y values of the kernel function.
	 *
	 * <p>These values are used to construct the interpolator. They should
	 * correspond to the x values in {@link #getT()} and be of the same length.
	 * @return an array of doubles representing the y values of the kernel function.
	 * 
	 */
	double[] getY();
	
	/**
	 * The link function applied to the y values.
	 *
	 * <p>This function is used to transform the y values before interpolation.
	 * The default is {@link LinkFunction#NONE}, which means no transformation
	 * is applied. Other link functions can be used to model different types of
	 * relationships between x and y, such as logarithmic or logistic links.
	 * @return the link function applied to the y values, defaulting to NONE.
	 */
	@Value.Default default LinkFunction getLink() {return LinkFunction.NONE;}
	
	/**
	 * Computes the minimum x value of the kernel function.
	 *
	 * <p>This method computes the minimum by streaming the x values and finding
	 * the minimum. It returns 0 if the array is empty, but in practice the
	 * array should contain valid x values for a meaningful kernel function.
	 * @return the minimum x value of the kernel function, or 0 if the array is empty.
	 */
	@Value.Default default int getMinimum() {
		return Arrays.stream(getT()).min().orElse(0);
	}
	
	/**
	 * Computes the maximum x value of the kernel function.
	 *
	 * <p>This method computes the maximum by streaming the x values and finding
	 * the maximum. It returns 0 if the array is empty, but in practice the
	 * array should contain valid x values for a meaningful kernel function.
	 * @return the maximum x value of the kernel function, or 0 if the array is empty.
	 */
	@Value.Default default int getMaximum() {
		return Arrays.stream(getT()).max().orElse(0);
	}
	
	/**
	 * Constructs an interpolator for the kernel function based on the x and y
	 * values.
	 *
	 * <p>This method creates a {@link Coordinates} object from the x and y
	 * values, and then constructs a cubic spline interpolator using the
	 * {@link SplineInterpolator}. The interpolator can be used to compute the
	 * raw value of the kernel at any given x.
	 *
	 * <p>Note: The choice of cubic spline interpolation is a design decision;
	 * other interpolation methods could be used depending on the desired
	 * properties of the kernel function.
	 * @return an interpolator for the kernel function based on the x and y values.
	 */
	@JsonIgnore
	@Value.Derived 
	default Interpolator getInterpolator() {

		Coordinates tmp = ImmutableCoordinates.builder()
			.setX(Arrays.stream(getT()).asDoubleStream().toArray())
			.setY(getY())
			.setXMin(getMinimum())
			.setXMax(getMaximum())
			.setXLink(LinkFunction.NONE)
			.setYLink(getLink())
			.setIncreasing(false)
			.setResolveDuplicates(DuplicateResolution.MEAN)
			.build();
	
//		if (getX().length < 20) {
		
		return SplineInterpolator.createCubicSpline(
				tmp.getHx(), tmp.getHy()
		);
		
//		} else {
//			return LoessInterpolator.createLoessInterpolator(tmp.getHx(), tmp.getHy());
//		}
	}
	
	/**
	 * Computes the raw value of the kernel function at a given x.
	 *
	 * <p>This method uses the interpolator to compute the value at x, and then
	 * applies the inverse of the link function to transform it back to the
	 * original scale. The result is the raw value of the kernel function at x.
	 * @param x the input value for which to compute the kernel function
	 * @return the raw value of the kernel function at x
	 */
	default double rawValue(int x) {
		return getLink().invFn(getInterpolator().interpolate((double) x));
	}
	
	
	
}
