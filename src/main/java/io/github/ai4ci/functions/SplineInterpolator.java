package io.github.ai4ci.functions;

import org.apache.commons.math3.util.FastMath;

/**
 * Performs spline interpolation given a set of control points. This class is
 * here because it needs to be serializable which the default apache version is
 * not. Oh and also I needed one that did monotonic interpolation for CDFs.
 *
 * <p>
 * Interpolation is clamped to the domain of the input data. The spline is
 * guaranteed to pass through each control point exactly. Optionally the spline
 * can be constrained to be monotonic.
 */
public class SplineInterpolator implements Interpolator {

	/**
	 * Creates a cubic spline from a given set of control points.
	 *
	 * @param x The X component of the control points in increasing order
	 * @param y The Y component of the control points
	 * @see #createCubicSpline(double[], double[], boolean)
	 * @return A cubic spline interpolator that passes through the given control
	 *         points
	 */
	public static SplineInterpolator createCubicSpline(double[] x, double[] y) {
		return createCubicSpline(x, y, false);
	}

	/**
	 * Creates a (possibily monotonic) cubic spline from a given set of control
	 * points.
	 *
	 * The spline is guaranteed to pass through each control point exactly.
	 * Moreover, assuming the control points are monotonic (Y is non-decreasing
	 * or non-increasing) then the interpolated values will also be monotonic.
	 * Interpolation is clamped to the domain of the input data.
	 *
	 * This function uses finite differences for non monotonic and the
	 * Fritsch-Carlson method for monotonic for computing the spline parameters.
	 * http://en.wikipedia.org/wiki/Monotone_cubic_interpolation
	 *
	 * @param x         The X component of the control points in increasing order
	 * @param y         The Y component of the control points
	 * @param monotonic Constrain spline to be monotonic (y must be increasing or
	 *                  decreasing order)
	 * @throws IllegalArgumentException if the X or Y arrays are null, have
	 *                                  different lengths or have fewer than 2
	 *                                  values.
	 * @return A cubic spline interpolator that passes through the given control
	 */
	public static SplineInterpolator createCubicSpline(
			double[] x, double[] y, boolean monotonic
	) {
		Interpolator.checkInputs(x, y);
		final var n = x.length;
		var d = new double[n - 1]; // could optimize this out
		var m = new double[n];

		// Compute slopes of secant lines between successive points.
		for (var i = 0; i < n - 1; i++) {

			if (!Double.isFinite(x[i]) || !Double.isFinite(y[i])) {
				throw new IllegalArgumentException(
						"Non-finite values in Spline Interpolator"
				);
			}

			var h = x[i + 1] - x[i];
			if (h <= 0.0) {
				throw new IllegalArgumentException(
						"The control points must all "
								+ "have strictly increasing X values."
				);
			}
			d[i] = (y[i + 1] - y[i]) / h;
		}

		// Initialize the tangents as the average of the secants.
		m[0] = d[0];
		for (var i = 1; i < n - 1; i++) {
			m[i] = (d[i - 1] + d[i]) * 0.5;
		}
		m[n - 1] = d[n - 2];

		if (monotonic) {
			// Update the tangents to preserve monotonicity.
			for (var i = 0; i < n - 1; i++) {
				if (d[i] == 0.0) { // successive Y values are equal
					m[i] = 0.0;
					m[i + 1] = 0.0;
				} else {
					var a = m[i] / d[i];
					var b = m[i + 1] / d[i];
					var h = FastMath.hypot(a, b);
					// Math hypot gives sqrt hence 3 and not 9 as in wikipedia:
					if (h > 3.0) {
						var t = 3.0 / h;
						m[i] = t * a * d[i];
						m[i + 1] = t * b * d[i];
					}
				}
			}
		}
		return new SplineInterpolator(x, y, m);
	}

	/**
	 * Creates a monotonic cubic spline from a given set of control points.
	 *
	 * @param x The X component of the control points in increasing order
	 * @param y The Y component of the control points in increasing or decreasing
	 *          order
	 * @see #createCubicSpline(double[], double[], boolean)
	 * @return A monotonic cubic spline interpolator that passes through the
	 *         given control points
	 */
	public static SplineInterpolator createMonotoneCubicSpline(
			double[] x, double[] y
	) {
		return createCubicSpline(x, y, true);
	}

	private final double[] mX;

	private final double[] mY;

	private final double[] mM;

	private SplineInterpolator(double[] x, double[] y, double[] m) {
		this.mX = x;
		this.mY = y;
		this.mM = m;
	}

	/**
	 * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of
	 * the spline.
	 *
	 * @param x The X value.
	 * @return The interpolated Y = f(X) value.
	 */
	@Override
	public double differential(double x) {
		// Handle the boundary cases.
		final var n = this.mX.length;
		if (Double.isNaN(x)) { return x; }
		if ((x <= this.mX[0]) || (x >= this.mX[n - 1])) {
			return 0; // mM[n - 1];
		}

		// Find the index 'i' of the last point with smaller X.
		// We know this will be within the spline due to the boundary tests.
		var i = 0;
		while (x >= this.mX[i + 1]) {
			i += 1;
			if (x == this.mX[i]) { return this.mM[i]; }
		}

		var p0 = this.mY[i];
		var m0 = this.mM[i];
		var p1 = this.mY[i + 1];
		var m1 = this.mM[i + 1];

		// Perform cubic Hermite spline interpolation.
		var scale = this.mX[i + 1] - this.mX[i];
		var t = (x - this.mX[i]) / scale;
		var t2 = t * t;
		var h00 = 6 * t2 - 6 * t;
		var h10 = 3 * t2 - 4 * t + 1;
		var h01 = -6 * t2 + 6 * t;
		var h11 = 3 * t2 - 2 * t;

		// https://math.stackexchange.com/questions/2444650/cubic-hermite-spline-derivative
		// and
		// https://github.com/thibauts/cubic-hermite-spline/blob/master/index.js
		// Correct scaling:
		// https://github.com/liuyxpp/CubicHermiteSpline.jl/blob/master/src/univariate.jl

		return (h00 * p0 + h10 * m0 * scale + h01 * p1 + h11 * m1 * scale)
				/ scale;
	}

	/**
	 * Generates an inverse spline that approximates the inverse function of this
	 * spline. The generated spline will have the same number of control points
	 * as this spline, but with the X and Y components swapped. The generated
	 * spline will be monotonic if this spline is monotonic.
	 *
	 * @return A new SplineInterpolator that approximates the inverse function of
	 *         this spline.
	 */
	public SplineInterpolator generateInverse() {
		// return createMonotoneCubicSpline(mY,mX);

		var intervals = (this.mY.length) - (this.mY.length) % 2; // ensure even
		var yMin = this.mY[0];
		var yMax = this.mY[this.mY.length - 1];
		var y = new double[intervals + 1];
		var x = new double[intervals + 1];
		x[0] = this.mX[0];
		x[intervals] = this.mX[this.mX.length - 1];
		y[0] = this.mY[0];
		y[intervals] = this.mY[this.mY.length - 1];
		var step = (yMax - yMin) / (intervals);
		for (var i = 1; i < intervals; i++) {
			y[i] = yMin + i * step;
			x[i] = this.inverse(y[i]);
		}
		return createMonotoneCubicSpline(y, x);
	}

	private double initialGuess(double y) {
		// Binary search to find interval [mY[i], mY[i+1]] containing y
		var i = 0;
		var j = this.mY.length - 1;

		if (y <= this.mY[0]) { return this.mX[0]; }
		if (y >= this.mY[j]) { return this.mX[j]; }

		while (i < j - 1) {
			var mid = (i + j) / 2;
			if (y < this.mY[mid]) {
				j = mid;
			} else {
				i = mid;
			}
		}

		// Linear interpolation in y → x
		var t = (y - this.mY[i]) / (this.mY[i + 1] - this.mY[i]);
		return this.mX[i] + t * (this.mX[i + 1] - this.mX[i]);
	}

	/**
	 * Interpolates the value of Y = f(X) for given X. Clamps X to the domain of
	 * the input data.
	 *
	 * @param x The X value.
	 * @return The interpolated Y = f(X) value.
	 */
	@Override
	public double interpolate(double x) {
		// Handle the boundary cases.
		final var n = this.mX.length;
		if (Double.isNaN(x)) { return x; }
		if (x <= this.mX[0]) { return this.mY[0]; }
		if (x >= this.mX[n - 1]) { return this.mY[n - 1]; }

		// Find the index 'i' of the last point with smaller X.
		// We know this will be within the spline due to the boundary tests.
		var i = 0;
		while (x >= this.mX[i + 1]) {
			i += 1;
			if (x == this.mX[i]) { return this.mY[i]; }
		}

		// Perform cubic Hermite spline interpolation.
		var p0 = this.mY[i];
		var m0 = this.mM[i];
		var p1 = this.mY[i + 1];
		var m1 = this.mM[i + 1];

		var scale = this.mX[i + 1] - this.mX[i];
		var t = (x - this.mX[i]) / scale;

		var t2 = t * t;
		var it = 1 - t;
		var it2 = it * it;
		var tt = 2 * t;
		var h00 = (1 + tt) * it2;
		var h10 = t * it2;
		var h01 = t2 * (3 - tt);
		var h11 = t2 * (t - 1);

		return h00 * p0 + h10 * m0 * scale + h01 * p1 + h11 * m1 * scale;

//		return (p0 * (1 + 2 * t) + scale * m0 * t) * (1 - t) * (1 - t)
//				+ (p1 * (3 - 2 * t) + scale * m1 * (t - 1)) * t * t;

	}

	/**
	 * Computes the inverse of this spline at a given Y value. This method uses
	 * the Newton-Raphson method to find the X value such that interpolate(X) ≈
	 * Y. The method is robust to non-monotonic splines and handles edge cases
	 * gracefully.
	 *
	 * @param y The Y value for which to compute the inverse.
	 * @return The X value such that interpolate(X) ≈ Y.
	 */
	public double inverse(double y) {
		// Handle boundaries
		if (y <= this.mY[0]) { return this.mX[0]; }
		if (y >= this.mY[this.mY.length - 1]) {
			return this.mX[this.mX.length - 1];
		}

		// Initial guess: interpolate in (y, x) space using the control points
		var init = this.initialGuess(y);
		var x = init;

		final var maxIter = 8; // Low limit for speed
		final var tol = 1e-6; // Reduced accuracy tolerance
		final var eps = 1e-12;

		for (var i = 0; i < maxIter; i++) {
			var fx = this.interpolate(x);
			var dfx = this.differential(x);

			var delta = (fx - y);
			if (FastMath.abs(delta) < tol) { return x; }

			if (FastMath.abs(dfx) < eps) {
				// Derivative near zero — avoid division by zero
				break;
			}

			var step = delta / dfx;
			var xNew = x - step;

			// If step is huge or NaN, fall back to secant guess
			if (Double.isNaN(xNew) || FastMath.abs(step) > 1e6) { break; }

			// Clamp to domain
			if (xNew <= this.mX[0]) {
				x = this.mX[0] + 1e-8;
			} else if (xNew >= this.mX[this.mX.length - 1]) {
				x = this.mX[this.mX.length - 1] - 1e-8;
			} else {
				x = xNew;
			}
		}

		// If Newton fails, fall back to closest of initial and last estimate
		return (FastMath.abs(init - y) > FastMath.abs(x - y)) ? x : init;
	}

	@Override
	public String toString() {
		var str = new StringBuilder();
		final var n = this.mX.length;
		str.append("[");
		for (var i = 0; i < n; i++) {
			if (i != 0) { str.append(", "); }
			str.append("(").append(this.mX[i]);
			str.append(", ").append(this.mY[i]);
			str.append(": ").append(this.mM[i]).append(")");
		}
		str.append("]");
		return str.toString();
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
