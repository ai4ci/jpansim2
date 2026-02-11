package io.github.ai4ci.functions;

import java.util.function.DoubleUnaryOperator;

import io.github.ai4ci.util.Conversions;

/**
 * Link functions used to transform domain and range values for interpolation
 * and statistical fitting.
 *
 * <p>
 * Main purpose: provide a small set of commonly used transforms (identity,
 * logarithm and logit) together with their inverses and derivatives. These
 * functions are used throughout the numeric pipeline to stabilise fitting and
 * interpolation (for example transforming probabilities to the logit scale
 * before fitting splines).
 *
 * <p>
 * Downstream uses: consumed by
 * {@link io.github.ai4ci.functions.EmpiricalDistribution} and spline/
 * interpolation helpers to map raw values into a transformed working scale and
 * back again.
 *
 * @author Rob Challen
 */
public enum LinkFunction {

	/**
	 * identity link: no transformation, used for unbounded domains and ranges
	 */
	NONE(
			d -> d, d -> d, d -> 1.0, // derivative of identity is 1 (doh)
			d -> 1.0, Math.pow(10, 8)
	),
	/**
	 * logarithm link: transforms positive values to the real line, used for
	 * positive domains and ranges
	 */
	LOG(
			d -> Math.log(d), d -> Math.exp(d), d -> 1.0 / d, d -> Math.exp(d),
			Math.log(10) * 8
	),
	/**
	 * logit link: transforms probabilities (0–1) to the real line, used for
	 * probability domains and ranges.
	 */
	LOGIT(
			d -> Conversions.logit(d), d -> Conversions.expit(d),
			d -> 1.0 / d + 1.0 / (1.0 - d),
			d -> Conversions.expit(d) * (1 - Conversions.expit(d)),
			Conversions.logit(1 - Math.pow(10, -8))
	);

	DoubleUnaryOperator fn;
	DoubleUnaryOperator invFn;
	DoubleUnaryOperator derivFn;
	DoubleUnaryOperator derivInvFn;
	double lim;

	private LinkFunction(
			DoubleUnaryOperator fn, DoubleUnaryOperator invFn,
			DoubleUnaryOperator derivFn, DoubleUnaryOperator derivInvFn, double lim
	) {
		this.fn = fn;
		this.invFn = invFn;
		this.derivFn = derivFn;
		this.derivInvFn = derivInvFn;
		this.lim = lim;
	}

	/**
	 * Return the derivative of the forward link evaluated at the supplied domain
	 * value.
	 *
	 * @param x the input value in the original domain
	 * @return the derivative d/dx link(x)
	 */
	public double derivFn(double x) {
		return this.derivFn.applyAsDouble(x);
	}

	/**
	 * Return the derivative of the inverse link evaluated at the supplied
	 * link-scale value. The value is clipped to safe bounds prior to
	 * differentiation.
	 *
	 * @param y a value on the link scale
	 * @return the derivative d/dy invLink(y)
	 */
	public double derivInvFn(double y) {
		y = Interpolator.squish(-this.lim, y, this.lim);
		return this.derivInvFn.applyAsDouble(y);
	}

	/**
	 * Apply the forward link transform to the supplied value. The result is
	 * clipped to a pre-defined numeric bound to avoid infinities from downstream
	 * spline operations.
	 *
	 * @param x the raw input value
	 * @return the transformed value on the link scale, clipped to safe bounds
	 */
	public double fn(double x) {
		double hx = this.fn.applyAsDouble(x);
		return Interpolator.squish(-this.lim, hx, this.lim);
	}

	/**
	 * Returns the maximum supported value for this link's domain. Values above
	 * this limit are considered outside the useful support of the function and
	 * are typically clipped by higher-level code.
	 *
	 * @return the maximum supported input value for the inverse link
	 */
	public double getMaxSupport() { return this.invFn(this.lim); }

	/**
	 * Returns the minimum supported value for this link's domain. Values below
	 * this limit are considered outside the useful support of the function and
	 * are typically clipped by higher-level code.
	 *
	 * @return the minimum supported input value for the inverse link
	 */
	public double getMinSupport() { return this.invFn(-this.lim); }

	/**
	 * Returns true if the supplied domain value lies strictly within the numeric
	 * support limits for this link.
	 *
	 * @param x the value to test
	 * @return {@code true} if the value is within support, otherwise
	 *         {@code false}
	 */
	public boolean inSupport(double x) {
		return this.fn(x) > -this.lim && this.fn(x) < this.lim;
	}

	/**
	 * Apply the inverse link (transform from link scale back to the original
	 * domain). The supplied value is clipped to safe bounds before inversion.
	 *
	 * @param y a value on the link scale
	 * @return the inverse-transformed value in the original domain
	 */
	public double invFn(double y) {
		y = Interpolator.squish(-this.lim, y, this.lim);
		return this.invFn.applyAsDouble(y);
	}

}