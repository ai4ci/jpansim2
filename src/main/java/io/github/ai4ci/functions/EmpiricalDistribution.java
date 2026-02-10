package io.github.ai4ci.functions;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.functions.Coordinates.DuplicateResolution;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalDistribution.class)
@JsonDeserialize(as = ImmutableEmpiricalDistribution.class)
/**
 * Empirical probability distribution constructed from sample data or
 * from x / F(x) pairs.
 *
 * <p>Main purpose: provide cumulative, density, quantile and sampling
 * functionality for empirically observed variables. The implementation
 * fits a monotonic spline on a transformed scale and exposes convenient
 * accessors for mean, median, density and random sampling.
 *
 * <p>Downstream uses: consumed by in-host configuration and calibration
 * routines (for example {@link io.github.ai4ci.config.inhost.InHostConfiguration}
 * and {@link io.github.ai4ci.abm.Calibration}) and by initialisers that need
 * empirical sampling behaviour.
 *
 * @author Rob Challen
 */
public interface EmpiricalDistribution extends Distribution, Serializable {

    /**
     * Minimum support value for the distribution.
     *
     * @return the minimum domain value for which the distribution is defined
     */
    double getMinimum();

    /**
     * Maximum support value for the distribution.
     *
     * @return the maximum domain value for which the distribution is defined
     */
    double getMaximum();
    
    /**
     * The x coordinates used to construct the empirical cumulative.
     *
     * @return an array of x values (in ascending order) used for the CDF
     */
    double[] getX();

    /**
     * The cumulative probabilities corresponding to {@link #getX()}.
     *
     * @return an array of cumulative probabilities in the unit interval
     */
    double[] getCumulativeProbability();
    
    /**
     * The link function applied to the domain when fitting the spline.
     *
     * @return the link function used for fitting and inversion
     */
    @Value.Default default LinkFunction getLink() {return LinkFunction.NONE;}

    /**
     * Minimum support used internally by derived methods. This mirrors
     * {@link #getMinimum()} and is provided for clarity in templates.
     *
     * @return the lower support bound
     */
    @Value.Derived @JsonIgnore default double getMinSupport() {
        return getMinimum();
    }

    /**
     * Maximum support used internally by derived methods. This mirrors
     * {@link #getMaximum()} and is provided for clarity in templates.
     *
     * @return the upper support bound
     */
    @Value.Derived @JsonIgnore default double getMaxSupport() {
        return getMaximum();
    }
    
    
    /**
	 * The spline is fitted on the transformed scale to ensure monotonicity and
	 * to allow for flexible support. The link function transforms the x values
	 * to a scale suitable for fitting, while the logit transformation of the
	 * cumulative probabilities ensures that the fitted function can capture
	 * probabilities in the unit interval without boundary issues.
	 *
	 * @return a spline interpolator representing logit(F(X)) as a function of link(X)
	 */
    @JsonIgnore
    @Value.Derived default SplineInterpolator getLogitLinkCDF() {
        Coordinates tmp = ImmutableCoordinates.builder()
            .setX(getX())
            .setY(getCumulativeProbability())
            .setXLink(getLink())
            .setYLink(LinkFunction.LOGIT)
            .setIncreasing(true)
            .setResolveDuplicates(DuplicateResolution.MAX)
            .setXMin(getMinimum())
            .setXMax(getMaximum())
            .build();
        return SplineInterpolator.createMonotoneCubicSpline(
            tmp.getHx(),
            tmp.getHy()
        );
    }
    
    
    /**
	 * The quantile function is the inverse of the CDF. By fitting the spline
	 * on the logit-transformed cumulative probabilities, we can easily derive
	 * the quantile function by inverting the spline and applying the inverse
	 * link function.
	 *
	 * @return a spline interpolator representing the quantile function
	 */
    @JsonIgnore
    @Value.Lazy default SplineInterpolator getLinkLogitQuantile() {
        return  getLogitLinkCDF().generateInverse();
    }
    
    /**
     * Sample a random value from the empirical distribution using the
     * default sampler.
     *
     * @return a random draw from the distribution
     */
    @JsonIgnore
    public default double sample() {
        Sampler rng = Sampler.getSampler();
        return sample(rng);
    }
    
    /**
     * Sample a random value using the supplied {@link Sampler} instance.
     *
     * @param rng the random number generator to use
     * @return a random draw from the distribution
     */
    @JsonIgnore
    default double sample(Sampler rng) {
        return getLink().invFn(
                getLinkLogitQuantile().interpolate(
                    LinkFunction.LOGIT.fn(rng.nextDouble())
                ));
    }
    
    /**
     * Compute the mean of the distribution using numerical integration.
     *
     * @return the expected value (mean) of the distribution
     */
    @JsonIgnore
    @Value.Lazy
    default double getMean() {
        RombergIntegrator tmp = new RombergIntegrator(0.001, 0.001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
        return tmp.integrate(100000, x -> x * this.getDensity(x), 
                getMinimum(), 
                getMaximum());
    };
    
    /**
     * Return the cumulative distribution value at the supplied x.
     *
     * @param x the point at which to evaluate the CDF
     * @return the cumulative probability F(x) in the unit interval
     */
    @JsonIgnore
    default double getCumulative(double x) {
        return Conversions.expit(
            getLogitLinkCDF().interpolate(
                this.getLink().fn(x)
            )
        );
    };
    
    /**
     * Return the p-th quantile of the distribution.
     *
     * @param p a probability in [0,1]
     * @return the quantile corresponding to p
     */
    @JsonIgnore
    default double getQuantile(double p) {
        return 
            getLink().invFn(    
                getLinkLogitQuantile().interpolate(
                    LinkFunction.LOGIT.fn(p)
                )
            );
    }
    
    /**
     * Convenience accessor for the median.
     *
     * @return the 0.5 quantile (median)
     */
    @JsonIgnore
    default double getMedian() {
        return getQuantile(0.5);
    }
    
    /**
	 * Compute the density at the supplied x using the derivative of the
	 * fitted spline and the link function.
	 *
	 * @param x the point at which to evaluate the density
	 * @return the density f(x) at the point x
	 */
    @JsonIgnore
    default double getDensity(double x) {
        
        if (this.getMinimum() >= x) return 0;
        if (this.getMaximum() <= x) return 0;
        if (!getLink().inSupport(x)) return 0;
        
        // density is differential of the function:
        // expit(spline(link(x)))
        // = expit'(spline(link(x))) * spline'(link(x)) * link'(x))
        
        return 
                // expit'(spline(link(x))
                LinkFunction.LOGIT.derivInvFn(
                    getLogitLinkCDF().interpolate(getLink().fn(x))
                ) * 
                // spline'(link(x))
                getLogitLinkCDF().differential(
                    getLink().fn(x)
                ) * 
                // link'(x)
                getLink().derivFn(x);
    };
    
    /**
     * Baseline an odds-like function to produce a probability distribution by
     * scaling so that the product odds * density integrates to one.
     *
     * @param odds the odds-like empirical function to baseline against this density
     * @return a probability distribution function derived from the supplied odds
     */
    default EmpiricalFunction baselineOdds(EmpiricalFunction odds) {
        RombergIntegrator tmp = new RombergIntegrator(0.0001, 0.0001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
        double total = tmp.integrate(100000, 
            x -> odds.value(x) * this.getDensity(x),
            this.getMinimum(),
            this.getMaximum()
//          Math.max(this.getMinimum(), odds.getMinimum()), 
//          Math.min(this.getMaximum(), odds.getMaximum())
        );
        
        return ImmutableEmpiricalFunction.builder()
                .setLink(getLink())
                .setX(odds.getX())
                .setY(Arrays.stream(odds.getY()).map(y -> y/total).toArray())
                .build();
    }
    
    /**
     * Number of knots to use when fitting the spline. If the number of data
     * points exceeds this value, the data will be downsampled to this number of points
     * to ensure a smooth fit and to avoid overfitting. If the number of data points is
     * less than or equal to this value, the data will be used as is without downsampling.
     * 
     */
    static int KNOTS = 50;
    
    /**
     * Convenience factory that builds an empirical distribution from raw sample
     * data using a default link function.
     *
     * @param data samples used to estimate the distribution
     * @return an immutable empirical distribution fitted to the samples
     */
    public static ImmutableEmpiricalDistribution fromData(double... data) {
        return fromData(LinkFunction.NONE, data);
    }
    
    /**
     * Construct an empirical distribution from sample data using the supplied
     * link function for transformation prior to fitting.
     *
     * @param link the link function applied to the sample values prior to fitting
     * @param data the raw samples used to build the empirical distribution
     * @return an immutable empirical distribution fitted to the samples
     */
    public static ImmutableEmpiricalDistribution fromData(LinkFunction link, double... data) {
        
        Arrays.sort(data);
        ImmutableEmpiricalDistribution.Builder out = ImmutableEmpiricalDistribution.builder();
        
        double meanH = Arrays.stream(data).map(link::fn).average().getAsDouble();
        double varH = Arrays.stream(data).map(link::fn).map(d -> Math.pow(d-meanH, 2) ).average().getAsDouble();
        
        double min = link.invFn(meanH-5*Math.sqrt(varH));
        double max = link.invFn(meanH+5*Math.sqrt(varH));
        
        out.setMinimum(min);
        out.setMaximum(max);
        
        if (data.length > KNOTS) {
            
            float step = ((float) data.length) / KNOTS;
            double[] x = new double[KNOTS-1];
            double[] y = new double[KNOTS-1];
            for (int i=1; i<KNOTS; i++) {
                double ix = i*step;
                int i0 = (int) Math.floor(ix);
                int i1 = (int) Math.ceil(ix);
                if (i0==i1) {
                    x[i-1] = data[i0];
                } else {
                    x[i-1] = data[i0] * (i1-ix) + data[i1] * (ix-i0);
                }
                y[i-1] = ((double) i) / (KNOTS);
            }
            out.setX(x);
            out.setCumulativeProbability(y);
        } else {
            // use the data as is
            double[] y = new double[data.length];
            for (int i=0; i<data.length; i++) y[i] = (i+1.0) / (data.length + 1.0);
            out.setX(data);
            out.setCumulativeProbability(y);
        }
        out.setLink(link);
        return out.build();
        
    }
}