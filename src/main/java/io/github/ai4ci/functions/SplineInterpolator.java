package io.github.ai4ci.functions;

import org.apache.commons.math3.util.FastMath;

/**
 * Performs spline interpolation given a set of control points.
 * This class is here because it needs to be serializable which the 
 * default apache version is not. Oh and also I needed one that did
 * monotonic interpolation for CDFs.
 * 
 * <p> Interpolation is clamped to the domain of the input data. The spline is 
 * guaranteed to pass through each control point exactly. Optionally
 * the spline can be constrained to be monotonic.
 */
public class SplineInterpolator implements Interpolator {

	private final double[] mX;
	private final double[] mY;
	private final double[] mM;

	private SplineInterpolator(double[] x, double[] y, double[] m) {
		mX = x;
		mY = y;
		mM = m;
	}

	/**
	 * 	Creates a monotonic cubic spline from a given set of control points.
	 * @param x The X component of the control points in increasing order
	 * @param y The Y component of the control points in increasing or decreasing order
	 * @see #createCubicSpline(double[], double[], boolean)
	 */
	public static SplineInterpolator createMonotoneCubicSpline(double[] x, double[] y) {
		return createCubicSpline(x, y, true);
	}
	
	/**
	 * 	Creates a cubic spline from a given set of control points.
	 * @param x The X component of the control points in increasing order
	 * @param y The Y component of the control points
	 * @see #createCubicSpline(double[], double[], boolean)
	 */
	public static SplineInterpolator createCubicSpline(double[] x, double[] y) {
		return createCubicSpline(x, y, false);
	}
			
	
	/**
	 * Creates a (possibily monotonic) cubic spline from a given set of control points.
	 * 
	 * The spline is guaranteed to pass through each control point exactly. Moreover, assuming the control points are
	 * monotonic (Y is non-decreasing or non-increasing) then the interpolated values will also be monotonic.
	 * Interpolation is clamped to the domain of the input data.
	 * 
	 * This function uses finite differences for non monotonic and the 
	 * Fritsch-Carlson method for monotonic for computing the spline parameters.
	 * http://en.wikipedia.org/wiki/Monotone_cubic_interpolation
	 * 
	 * @param x The X component of the control points in increasing order
	 * @param y The Y component of the control points
	 * @param monotonic Constrain spline to be monotonic (y must be increasing or decreasing order)
	 * @throws IllegalArgumentException
	 *             if the X or Y arrays are null, have different lengths or have fewer than 2 values.
	 */
	public static SplineInterpolator createCubicSpline(double[] x, double[] y, boolean monotonic) {
		Interpolator.checkInputs(x, y);
		final int n = x.length;
		double[] d = new double[n - 1]; // could optimize this out
		double[] m = new double[n];

		// Compute slopes of secant lines between successive points.
		for (int i = 0; i < n - 1; i++) {
			
			if (!Double.isFinite(x[i]) || !Double.isFinite(y[i])) {
				throw new IllegalArgumentException("Non-finite values in Spline Interpolator");
			}
			
			double h = x[i + 1] - x[i];
			if (h <= 0.0) {
				throw new IllegalArgumentException("The control points must all "
						+ "have strictly increasing X values.");
			}
			d[i] = (y[i + 1] - y[i]) / h;
		}

		// Initialize the tangents as the average of the secants.
		m[0] = d[0];
		for (int i = 1; i < n - 1; i++) {
			m[i] = (d[i - 1] + d[i]) * 0.5;
		}
		m[n - 1] = d[n - 2];

		if (monotonic) {
			// Update the tangents to preserve monotonicity.
			for (int i = 0; i < n - 1; i++) {
				if (d[i] == 0.0) { // successive Y values are equal
					m[i] = 0.0;
					m[i + 1] = 0.0;
				} else {
					double a = m[i] / d[i];
					double b = m[i + 1] / d[i];
					double h = (double) FastMath.hypot(a, b);
					// Math hypot gives sqrt hence 3 and not 9 as in wikipedia:
					if (h > 3.0) {
						double t = 3.0 / h;
						m[i] = t * a * d[i];
						m[i + 1] = t * b * d[i];
					}
				}
			}
		}
		return new SplineInterpolator(x, y, m);
	}

	/**
	 * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of the spline.
	 * 
	 * @param x The X value.
	 * @return The interpolated Y = f(X) value.
	 */
	public double differential(double x) {
		// Handle the boundary cases.
		final int n = mX.length;
		if (Double.isNaN(x)) {
			return x;
		}
		if (x <= mX[0]) {
			return 0; //mM[0];
		}
		if (x >= mX[n - 1]) {
			return 0; //mM[n - 1];
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
	 * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of 
	 * the input data.
	 * 
	 * @param x The X value.
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
	
	public double inverse(double y) {
	    // Handle boundaries
	    if (y <= mY[0]) return mX[0];
	    if (y >= mY[mY.length - 1]) return mX[mX.length - 1];

	    // Initial guess: interpolate in (y, x) space using the control points
	    double init = initialGuess(y);
	    double x = init;

	    final int maxIter = 8;          // Low limit for speed
	    final double tol = 1e-6;        // Reduced accuracy tolerance
	    final double eps = 1e-12;

	    for (int i = 0; i < maxIter; i++) {
	        double fx = interpolate(x);
	        double dfx = differential(x);

	        double delta = (fx - y);
	        if (FastMath.abs(delta) < tol) return x;

	        if (FastMath.abs(dfx) < eps) {
	            // Derivative near zero — avoid division by zero
	            break;
	        }

	        double step = delta / dfx;
	        double xNew = x - step;

	        // If step is huge or NaN, fall back to secant guess
	        if (Double.isNaN(xNew) || FastMath.abs(step) > 1e6) {
	            break;
	        }

	        // Clamp to domain
	        if (xNew <= mX[0]) {
	            x = mX[0] + 1e-8;
	        } else if (xNew >= mX[mX.length - 1]) {
	            x = mX[mX.length - 1] - 1e-8;
	        } else {
	            x = xNew;
	        }
	    }

	    // If Newton fails, fall back to closest of initial and last estimate
	    return (FastMath.abs(init - y) > FastMath.abs(x - y)) ? x : init;
	}
	
	private double initialGuess(double y) {
	    // Binary search to find interval [mY[i], mY[i+1]] containing y
	    int i = 0;
	    int j = mY.length - 1;

	    if (y <= mY[0]) return mX[0];
	    if (y >= mY[j]) return mX[j];

	    while (i < j - 1) {
	        int mid = (i + j) / 2;
	        if (y < mY[mid]) {
	            j = mid;
	        } else {
	            i = mid;
	        }
	    }

	    // Linear interpolation in y → x
	    double t = (y - mY[i]) / (mY[i + 1] - mY[i]);
	    return mX[i] + t * (mX[i + 1] - mX[i]);
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
	
	
	public SplineInterpolator generateInverse() {
		// return createMonotoneCubicSpline(mY,mX);
		
		int intervals = (mY.length) - (mY.length) % 2; // ensure even
		double yMin = mY[0];
		double yMax = mY[mY.length - 1];
		double[] y = new double[intervals+1];
		double[] x = new double[intervals+1];
		x[0] = mX[0];
		x[intervals] = mX[mX.length-1];
		y[0] = mY[0];
		y[intervals] = mY[mY.length-1];
		double step = (yMax-yMin)/(intervals); 
		for (int i=1; i<intervals; i++) {
			y[i] = yMin + i*step;
			x[i] = inverse(y[i]);
		}
		return createMonotoneCubicSpline(y, x);
	}
	
//	This is an alternative approach that ought to be better but I think I need to 
//  Look at the limits more closely
//	static double[] QUANTILE_KNOTS = new double[] {
//			1.000000e-05, 3.162352e-05, 1.000000e-04, 1.581166e-04, 2.500000e-04, 
//			1.000000e-03, 1.119725e-03, 2.237782e-03, 5.000000e-03, 5.040693e-03,
//			2.500000e-02, 3.543421e-02, 5.000000e-02, 7.103939e-02, 1.000000e-01,
//			1.613905e-01, 2.500000e-01, 3.660254e-01, 5.000000e-01, 6.339746e-01,
//			7.500000e-01, 8.386095e-01, 9.000000e-01, 9.289606e-01, 9.500000e-01, 
//			9.645658e-01, 9.750000e-01, 9.949593e-01, 9.950000e-01, 9.977622e-01,
//			9.988803e-01, 9.990000e-01, 9.997500e-01, 9.998419e-01, 9.999000e-01, 
//			9.999684e-01, 9.999900e-01	
//		};
//		
//		public SplineInterpolator generateInverse() {
//			// return createMonotoneCubicSpline(mY,mX);
//			
//			int intervals = QUANTILE_KNOTS.length; // ensure even
//			double yMin = Math.min(mY[0],QUANTILE_KNOTS[0]-0.1);
//			double yMax = Math.max(mY[mY.length - 1],QUANTILE_KNOTS[0]+0.1);
//			double[] y = new double[intervals+2];
//			double[] x = new double[intervals+2];
//			x[0] = mX[0];
//			x[intervals+1] = mX[mX.length-1];
//			y[0] = yMin;
//			y[intervals+1] = yMax;
//			for (int i=1; i<=intervals; i++) {
//				y[i] = QUANTILE_KNOTS[i-1];
//				x[i] = inverse(QUANTILE_KNOTS[i-1]);
//			}
//			return createMonotoneCubicSpline(y, x);
//		}
}
