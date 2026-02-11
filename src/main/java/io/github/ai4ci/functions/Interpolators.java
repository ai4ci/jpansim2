package io.github.ai4ci.functions;

/**
 * Enumeration of supported interpolation methods for interpolation of functions
 * from discrete data points.
 *
 * <p>
 * Each enum constant provides a method to create an {@link Interpolator}
 * instance for the given x and y data points.
 *
 * @author Rob Challen
 */
public enum Interpolators {

	/** Interpolator for monotone cubic spline interpolation. */
	MONOTONIC_CUBIC_SPLINE,
	/** Interpolator for cubic spline interpolation. */
	CUBIC_SPLINE,
	/** Interpolator for LOESS interpolation. */
	LOESS,
	/** Interpolator for linear interpolation. */
	LINEAR;

	private Interpolators() {}

	/**
	 * Create an Interpolator instance for the given x and y data points using
	 * the interpolation method represented by this enum constant.
	 *
	 * @param x the x data points
	 * @param y the y data points corresponding to the x data points
	 * @return an Interpolator instance that can be used to interpolate values
	 *         based on the given data points and interpolation method
	 * @throws IllegalArgumentException if this enum constant does not correspond
	 *                                  to a known interpolation method
	 */
	public Interpolator create(double[] x, double[] y) {
		switch (this) {
		case MONOTONIC_CUBIC_SPLINE:
			return SplineInterpolator.createMonotoneCubicSpline(x, y);
		case CUBIC_SPLINE:
			return SplineInterpolator.createCubicSpline(x, y);
		case LOESS:
			return LoessInterpolator.createLoessInterpolator(x, y);
		case LINEAR:
			return LinearInterpolator.createLinearInterpolator(x, y);
		default:
			throw new IllegalArgumentException("unknown interpolator: " + this);
		}
	}

}
