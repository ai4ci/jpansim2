package io.github.ai4ci.util;

import java.io.Serializable;

/**
 * Performs spline interpolation given a set of control points.
 * 
 */
public class SplineInterpolator implements Serializable {

	private final double[] mX;
	private final double[] mY;
	private final double[] mM;

	private SplineInterpolator(double[] x, double[] y, double[] m) {
		mX = x;
		mY = y;
		mM = m;
	}

	/**
	 * Creates a monotone cubic spline from a given set of control points.
	 * 
	 * The spline is guaranteed to pass through each control point exactly. Moreover, assuming the control points are
	 * monotonic (Y is non-decreasing or non-increasing) then the interpolated values will also be monotonic.
	 * 
	 * This function uses the Fritsch-Carlson method for computing the spline parameters.
	 * http://en.wikipedia.org/wiki/Monotone_cubic_interpolation
	 * 
	 * @param x The X component of the control points, strictly increasing.
	 * @param y The Y component of the control points
	 * @throws IllegalArgumentException
	 *             if the X or Y arrays are null, have different lengths or have fewer than 2 values.
	 */
	public static SplineInterpolator createMonotoneCubicSpline(double[] x, double[] y) {
		if (x == null || y == null || x.length != y.length || x.length < 2) {
			throw new IllegalArgumentException("There must be at least two control "
					+ "points and the arrays must be of equal length.");
		}

		final int n = x.length;
		double[] d = new double[n - 1]; // could optimize this out
		double[] m = new double[n];

		// Compute slopes of secant lines between successive points.
		for (int i = 0; i < n - 1; i++) {
			double h = x[i + 1] - x[i];
			if (h <= 0f) {
				throw new IllegalArgumentException("The control points must all "
						+ "have strictly increasing X values.");
			}
			d[i] = (y[i + 1] - y[i]) / h;
		}

		// Initialize the tangents as the average of the secants.
		m[0] = d[0];
		for (int i = 1; i < n - 1; i++) {
			m[i] = (d[i - 1] + d[i]) * 0.5f;
		}
		m[n - 1] = d[n - 2];

		// Update the tangents to preserve monotonicity.
		for (int i = 0; i < n - 1; i++) {
			if (d[i] == 0f) { // successive Y values are equal
				m[i] = 0f;
				m[i + 1] = 0f;
			} else {
				double a = m[i] / d[i];
				double b = m[i + 1] / d[i];
				double h = (double) Math.hypot(a, b);
				if (h > 9f) {
					double t = 3f / h;
					m[i] = t * a * d[i];
					m[i + 1] = t * b * d[i];
				}
			}
		}
		return new SplineInterpolator(x, y, m);
	}

	/**
	 * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of the spline.
	 * 
	 * @param x
	 *            The X value.
	 * @return The interpolated Y = f(X) value.
	 */
	public double interpolateDifferential(double x) {
		// Handle the boundary cases.
		final int n = mX.length;
		if (Double.isNaN(x)) {
			return x;
		}
		if (x <= mX[0]) {
			return mM[0];
		}
		if (x >= mX[n - 1]) {
			return mM[n - 1];
		}

		// Find the index 'i' of the last point with smaller X.
		// We know this will be within the spline due to the boundary tests.
		int i = 0;
		while (x >= mX[i + 1]) {
			i += 1;
			if (x == mX[i]) {
				return mM[i];
			}
		}

		double p0 = mY[i];
		double m0 = mM[i];
		double p1 = mY[i+1];
		double m1 = mM[i+1];
		
		// Perform cubic Hermite spline interpolation.
		double scale = mX[i + 1] - mX[i];
		double t = (x - mX[i]) / scale;
		double t2 = t * t;
		double h00 = 6 * t2 - 6 * t;
		double h10 = 3 * t2 - 4 * t + 1;
		double h01 = - 6 * t2 + 6 * t;
		double h11 = 3 * t2 - 2 * t;
		
		// https://math.stackexchange.com/questions/2444650/cubic-hermite-spline-derivative
		// and https://github.com/thibauts/cubic-hermite-spline/blob/master/index.js
		// Correct scaling: https://github.com/liuyxpp/CubicHermiteSpline.jl/blob/master/src/univariate.jl
		
		return (h00 * p0 + h10 * m0 * scale +
	    		h01 * p1 + h11 * m1 * scale) / scale;
	}

	/**
	 * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of the spline.
	 * 
	 * @param x
	 *            The X value.
	 * @return The interpolated Y = f(X) value.
	 */
	public double interpolate(double x) {
		// Handle the boundary cases.
		final int n = mX.length;
		if (Double.isNaN(x)) {
			return x;
		}
		if (x <= mX[0]) {
			return mY[0];
		}
		if (x >= mX[n - 1]) {
			return mY[n - 1];
		}

		// Find the index 'i' of the last point with smaller X.
		// We know this will be within the spline due to the boundary tests.
		int i = 0;
		while (x >= mX[i + 1]) {
			i += 1;
			if (x == mX[i]) {
				return mY[i];
			}
		}

		// Perform cubic Hermite spline interpolation.
		double p0 = mY[i];
		double m0 = mM[i];
		double p1 = mY[i+1];
		double m1 = mM[i+1];
		
		double scale = mX[i + 1] - mX[i];
		double t = (x - mX[i]) / scale;
		
		double t2 = t * t;
	    double it = 1 - t;
	    double it2 = it * it;
	    double tt = 2 * t;
	    double h00 = (1 + tt) * it2;
	    double h10 = t * it2;
	    double h01 = t2 * (3 - tt);
	    double h11 = t2 * (t - 1);
	    
	    return h00 * p0 + h10 * m0 * scale +
	    		h01 * p1 + h11 * m1 * scale;
		
//		return (p0 * (1 + 2 * t) + scale * m0 * t) * (1 - t) * (1 - t)
//				+ (p1 * (3 - 2 * t) + scale * m1 * (t - 1)) * t * t;
		
		
	}
	
	// For debugging.
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		final int n = mX.length;
		str.append("[");
		for (int i = 0; i < n; i++) {
			if (i != 0) {
				str.append(", ");
			}
			str.append("(").append(mX[i]);
			str.append(", ").append(mY[i]);
			str.append(": ").append(mM[i]).append(")");
		}
		str.append("]");
		return str.toString();
	}
}
