package io.github.ai4ci.functions;

import java.io.Serializable;

/**
 * Interface for mathematical interpolation functions.
 * 
 * <p>Provides interpolation capabilities with numerical differentiation support
 * for use in mathematical modeling and curve fitting operations.
 */
public interface Interpolator extends Serializable {
	
	/** Increment used for numerical differentiation */
	double DX = 0.00001; 
	
	static void checkInputs(double[] x, double[] y) {
		 if (x.length != y.length) {
			throw new IllegalArgumentException("x and y arrays must have the same length");
		}
		if (x.length < 2) {
			throw new IllegalArgumentException("x and y arrays must have at least 2 elements");
		}
		if (java.util.Arrays.stream(x).anyMatch(v -> Double.isNaN(v) || Double.isInfinite(v)) ||
		    java.util.Arrays.stream(y).anyMatch(v -> Double.isNaN(v) || Double.isInfinite(v))) {
			throw new IllegalArgumentException("x and y arrays must not contain NaN or infinite values");
		}
		for (int i = 0; i < x.length - 1; i++) {
			if (x[i] >= x[i + 1]) {
				throw new IllegalArgumentException("x array must be strictly increasing");
			}
		}
	}
	
	/**
	 * Interpolates the function at the given point.
	 * 
	 * @param x the interpolation point
	 * @return the interpolated value
	 */
	double interpolate(double x);
	
	/**
	 * Computes the numerical derivative at the given point.
	 * 
	 * @param x the point at which to compute the derivative
	 * @return the numerical derivative using central difference method
	 */
	default double differential(double x) {
		double dy = (interpolate(x+DX)-interpolate(x-DX))/(2*DX);
		return dy;
	}

	/**
	 * Clamps a value between specified lower and upper bounds.
	 * 
	 * @param low the lower bound
	 * @param x the value to clamp
	 * @param high the upper bound
	 * @return the clamped value between low and high
	 */
	static double squish(double low, double x, double high) {
		return (Math.min(Math.max(x, low), high));
	}
	
}