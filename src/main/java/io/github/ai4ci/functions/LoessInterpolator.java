package io.github.ai4ci.functions;

import java.util.Arrays;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.immutables.value.Value;

/**
 * Local regression (LOESS) interpolator for one-dimensional data.
 *
 * <p>Main purpose: provide a convenient, immutable wrapper around Apache
 * Commons Math's LOESS implementation that fulfils the {@link Interpolator}
 * contract. The implementation fits a smooth piecewise polynomial to the
 * supplied (x,y) data and exposes interpolation and derivative evaluation.
 *
 * <p>Key features:
 * <ul>
 *   <li>Immutable configuration via Immutables builder;</li>
 *   <li>Automatic bandwidth selection based on sample size;</li>
 *   <li>Provides both interpolated value and first derivative evaluation.</li>
 * </ul>
 *
 * <p>Downstream uses: consumed wherever a smooth local regression is required
 * for numeric curves, for example in smoothing empirical profiles before
 * fitting or calibration routines (see {@link EmpiricalDistribution}).
 *
 * @author Rob Challen
 */
@Value.Immutable
public interface LoessInterpolator extends Interpolator {

    /**
     * The independent coordinate values used to fit the LOESS model.
     *
     * @return the x-coordinates (must be strictly increasing and match {@link #getY()})
     */
    double[] getX();

    /**
     * The dependent coordinate values used to fit the LOESS model.
     *
     * @return the y-coordinates corresponding to {@link #getX()}
     */
    double[] getY();

    /**
     * The minimum x value in the support of the interpolator.
     *
     * @return the minimum x value (domain lower bound)
     */
    @Value.Derived
    default double getMinSupport() {
        return Arrays.stream(getX()).min().getAsDouble();
    }

    /**
     * The maximum x value in the support of the interpolator.
     *
     * @return the maximum x value (domain upper bound)
     */
    @Value.Derived
    default double getMaxSupport() {
        return Arrays.stream(getX()).max().getAsDouble();
    }

    /**
     * Returns an evaluated polynomial spline function produced by the
     * LOESS algorithm.
     *
     * <p>Clients may use the returned {@link PolynomialSplineFunction} for
     * advanced evaluation, for example to obtain higher order derivatives.
     *
     * @return the fitted {@link PolynomialSplineFunction}
     */
    @Value.Lazy
    default PolynomialSplineFunction getLoess() {
        int n = getX().length;
        double bw = 4.0/n;
        org.apache.commons.math3.analysis.interpolation.LoessInterpolator tmp
         = new org.apache.commons.math3.analysis.interpolation.LoessInterpolator(
             bw,
             org.apache.commons.math3.analysis.interpolation.LoessInterpolator.DEFAULT_ROBUSTNESS_ITERS
        );
        return tmp.interpolate(getX(),getY());
    }

    /**
     * Construct a LOESS interpolator from the supplied data arrays.
     *
     * @param x the x-coordinates (must be strictly increasing)
     * @param y the y-coordinates (same length as {@code x})
     * @return an immutable {@link LoessInterpolator} instance
     * @see Interpolator#checkInputs(double[], double[])
     */
    public static LoessInterpolator createLoessInterpolator(double[] x, double[] y) {
        Interpolator.checkInputs(x, y);
        return ImmutableLoessInterpolator.builder().setX(x).setY(y).build();
    }   
    
    /**
     * Interpolate the fitted LOESS spline at the supplied point.
     *
     * <p>Values outside the fitted support are clamped to the support
     * boundaries to avoid extrapolation artefacts.
     *
     * @param x the point at which to evaluate the interpolant
     * @return the interpolated y value
     */
    @Override
    default double interpolate(double x) {
        x = Interpolator.squish(getMinSupport(), x, getMaxSupport());
        return getLoess().value(x);
    }
    
    /**
     * Compute the first derivative of the fitted LOESS spline at the point
     * supplied. Values outside the fitted support are clamped to the support
     * boundaries before evaluation.
     *
     * @param x the point at which to evaluate the derivative
     * @return the first derivative of the interpolant at {@code x}
     */
    @Override
    default double differential(double x) {
        x = Interpolator.squish(getMinSupport(), x, getMaxSupport());
        return getLoess().derivative().value(x);
    }

}