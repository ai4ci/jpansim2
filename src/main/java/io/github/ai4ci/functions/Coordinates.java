package io.github.ai4ci.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Value.Immutable
/**
 * A compact representation of paired coordinates with optional link
 * transformations for each axis and duplicate handling.
 *
 * <p>Main purpose: provide a small value type that accepts raw x/y arrays,
 * applies domain and range link functions, filters points to a bounded
 * support and produces cleaned coordinate arrays suitable for monotone
 * spline fitting.
 *
 * <p>Key features:
 * <ul>
 *   <li>Apply independent {@link LinkFunction} transforms to x and y data</li>
 *   <li>Filter points to a configurable support window</li>
 *   <li>Resolve duplicate transformed x-values by mean, max or min</li>
 *   <li>Produce derived arrays {@link #getHx()} and {@link #getHy()} used by
 *       spline builders and interpolators</li>
 * </ul>
 *
 * <p>Downstream uses: consumed by {@link io.github.ai4ci.functions.EmpiricalDistribution}
 * when preparing data for spline fitting, and by {@link io.github.ai4ci.functions.SplineInterpolator}
 * for constructing monotone cubic splines.
 *
 * @author Rob Challen
 */
public interface Coordinates {
    
    /**
     * The raw x coordinate array.
     *
     * @return an array of x values in the original domain order
     */
    public double[] getX();
    
    /**
     * The raw y coordinate array corresponding to {@link #getX()}.
     *
     * @return an array of y values where {@code getY()[i]} pairs with {@code getX()[i]}
     */
    public double[] getY();
    
    /**
     * The link function applied to x coordinates prior to processing.
     *
     * @return the {@link LinkFunction} for the x axis
     */
    public LinkFunction getXLink();
    
    /**
     * The link function applied to y coordinates prior to processing.
     *
     * @return the {@link LinkFunction} for the y axis
     */
    public LinkFunction getYLink();
    
    /**
     * Lower bound of the x support used when mapping points. Defaults to the
     * minimum support of the x link function.
     *
     * @return the minimum x value considered for mapping
     */
    @Value.Default default double getXMin() { return getXLink().getMinSupport(); }
    /**
     * Upper bound of the x support used when mapping points. Defaults to the
     * maximum support of the x link function.
     *
     * @return the maximum x value considered for mapping
     */
    @Value.Default default double getXMax() { return getXLink().getMaxSupport(); }
    /**
     * Lower bound of the y support used when mapping points. Defaults to the
     * minimum support of the y link function.
     *
     * @return the minimum y value considered for mapping
     */
    @Value.Default default double getYMin() { return getYLink().getMinSupport(); }
    /**
     * Upper bound of the y support used when mapping points. Defaults to the
     * maximum support of the y link function.
     *
     * @return the maximum y value considered for mapping
     */
    @Value.Default default double getYMax() { return getYLink().getMaxSupport(); }
    
    /**
     * Whether the x values are strictly increasing. If true additional
     * boundary points based on {@link #getXMin()}/{@link #getXMax()} and
     * {@link #getYMin()}/{@link #getYMax()} are inserted to guarantee
     * full domain coverage for monotone interpolators.
     *
     * @return {@code true} if x is increasing, {@code false} otherwise
     */
    public boolean isIncreasing();
    
    /**
     * Strategy used when multiple transformed x values collide after
     * applying {@link #getXLink()} (duplicate transformed keys).
     *
     * @return the duplicate resolution policy
     */
    @Value.Default default DuplicateResolution resolveDuplicates() {return DuplicateResolution.MEAN;}
    
    /**
     * Resolution strategies for duplicate transformed x keys.
     */
    public static enum DuplicateResolution {
    	/** When multiple original points map to the same transformed x value, compute the mean of their y values. */
    	MEAN, 
    	/** When multiple original points map to the same transformed x value, take the maximum of their y values. */
    	MAX, 
    	/** When multiple original points map to the same transformed x value, take the minimum of their y values. */
    	MIN};
    
//	static Map<Coordinates, SortedMap<Double,List<Double>>> CACHE = new HashMap<>();
//	
//	default SortedMap<Double,List<Double>> getMapping() {
//		return CACHE.computeIfAbsent(this, Coordinates::mapped);
//	}
    
    private SortedMap<Double,List<Double>> getMapping() {
        SortedMap<Double,List<Double>> out = new TreeMap<>();
        for (int i=0; i < getX().length; i++) {
            double x = getX()[i];
            double y = getY()[i];
            if (x <= getXMax() && x >= getXMin() && y <= getYMax() && y >= getYMin()) {
                double tx = getXLink().fn(x);
                double ty = getYLink().fn(y);
                out.putIfAbsent(
                        tx, new ArrayList<>());
                out.get(tx).add(ty);
            }
        }
        if (isIncreasing()) {
            out.putIfAbsent(
                    getXLink().fn(getXMin()), 
                    List.of(getYLink().fn(getYMin()))
                );
            out.putIfAbsent(
                    getXLink().fn(getXMax()), 
                    List.of(getYLink().fn(getYMax()))
                );
        }
        return out;
    }
    
    /**
     * Returns the transformed x-coordinates after applying the x link
     * and aggregating duplicates according to {@link #resolveDuplicates()}.
     *
     * @return an array of transformed x values suitable for spline fitting
     */
    @Value.Derived
    @JsonIgnore
    public default double[] getHx() {
        return getMapping().keySet().stream().mapToDouble(d -> d).toArray();
    }
    
    /**
     * Returns the transformed y values corresponding to {@link #getHx()}.
     * If multiple original points map to the same transformed x the values
     * are combined according to {@link #resolveDuplicates()}.
     *
     * @return an array of transformed y values aligned with {@link #getHx()}
     */
    @Value.Derived
    @JsonIgnore
    public default double[] getHy() {
        return getMapping().values().stream().mapToDouble(
            list -> { 
                if (resolveDuplicates().equals(DuplicateResolution.MEAN)) {
                    return list.stream().mapToDouble(d->d).average().getAsDouble();
                } else if (resolveDuplicates().equals(DuplicateResolution.MAX)) {
                    return list.stream().mapToDouble(d->d).max().getAsDouble();
                } else if (resolveDuplicates().equals(DuplicateResolution.MIN)) {
                    return list.stream().mapToDouble(d->d).min().getAsDouble();
                } else {
                    throw new RuntimeException();
                }
            }).toArray();
    }

}