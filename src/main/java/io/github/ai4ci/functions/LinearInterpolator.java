package io.github.ai4ci.functions;

/**
 * A linear interpolator implementation that performs piecewise linear interpolation
 * between given data points.
 * <p>
 * <b>Input Constraints:</b>
 * <ul>
 *   <li>The x array must be strictly increasing (x[i] &lt; x[i+1] for all i)</li>
 *   <li>The x and y arrays must have the same length</li>
 *   <li>Both arrays must contain at least 2 elements</li>
 *   <li>Arrays must not contain NaN or infinite values</li>
 * </ul>
 * <p>
 * For input values outside the range [x[0], x[n-1]], the interpolator returns
 * the boundary values (y[0] for x &lt;= x[0], y[n-1] for x &gt;= x[n-1]).
 *
 * @see Interpolator
 */
public class LinearInterpolator implements Interpolator {

	private final double[] x;
	private final double[] y;
	
	/**
	 * Constructs a linear interpolator with the given data points.
	 *
	 * @param x the x-coordinates of the data points (must be strictly increasing)
	 * @param y the y-coordinates of the data points
	 * @throws IllegalArgumentException if arrays have different lengths, 
	 *         x is not strictly increasing, or arrays have less than 2 elements
	 */
	private LinearInterpolator(double[] x, double[] y) {
		Interpolator.checkInputs(x, y);
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Factory method to create a linear interpolator with the given data points.
	 *
	 * @param x the x-coordinates of the data points (must be strictly increasing)
	 * @param y the y-coordinates of the data points
	 * @return a new LinearInterpolator instance
	 * @throws IllegalArgumentException if arrays have different lengths, 
	 *         x is not strictly increasing, or arrays have less than 2 elements
	 */
	public static LinearInterpolator createLinearInterpolator(double[] x, double[] y) {
		return new LinearInterpolator(x, y);
	}
	
	/**
	 * Interpolates the value at the given x-coordinate using linear interpolation.
	 *
	 * @param x the x-coordinate to interpolate at
	 * @return the interpolated y-value
	 */
	@Override
	public double interpolate(double x) {
		if (x <= this.x[0]) return this.y[0];
		if (x >= this.x[this.x.length-1]) return this.y[this.y.length-1];
		
		int i = 0;
		while (this.x[i] < x) i++;
		
		double x1 = this.x[i-1];
		double x2 = this.x[i];
		double y1 = this.y[i-1];
		double y2 = this.y[i];
		
		return y1 + (y2-y1)*(x-x1)/(x2-x1);
	}

	/**
	 * Computes the derivative (slope) of the interpolated function at the given x-coordinate.
	 * Returns 0.0 for values outside the interpolation range.
	 *
	 * @param x the x-coordinate to compute the derivative at
	 * @return the derivative (slope) of the interpolated function
	 */
	@Override
	public double differential(double x) {
		if (x <= this.x[0]) return 0.0;
		if (x >= this.x[this.x.length-1]) return 0.0;
		
		int i = 0;
		while (this.x[i] < x) i++;
		
		double x1 = this.x[i-1];
		double x2 = this.x[i];
		double y1 = this.y[i-1];
		double y2 = this.y[i];
		
		return (y2-y1)/(x2-x1);
	}

}