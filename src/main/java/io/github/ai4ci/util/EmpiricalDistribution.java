package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.util.Coordinates.DuplicateResolution;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution.Builder;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalDistribution.class)
@JsonDeserialize(as = ImmutableEmpiricalDistribution.class)
/**
 * An empirical probability distribution with cumulative, density and sample
 * methods. It can be constructed from x, F(x) pairs, or from a set of samples
 * of x. If constructed from pairs it needs realistic minimum and maximum values.
 * Additionally a link function can be specified that maps the domain to plus
 * and minus infinity. Spline fitting is done on a link-logit transform of the
 * input data. A quantile function is present but in the majority of cases
 * a  
 */
public interface EmpiricalDistribution extends Abstraction.Distribution, Serializable {

	double getMinimum();
	double getMaximum();
	
	double[] getX();
	double[] getCumulativeProbability();
	
	@Value.Default default LinkFunction getLink() {return LinkFunction.NONE;}
	
	// A monotonic cubic spline function connecting link(X) and logit(F(X))
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
	
	// A monotonic cubic spline function connecting logit(F(X)) and link(X)
	@JsonIgnore
	@Value.Lazy default SplineInterpolator getLinkLogitQuantile() {
		return  getLogitLinkCDF().generateInverse();
	}
	
	@JsonIgnore
	public default double sample() {
		Sampler rng = Sampler.getSampler();
		return sample(rng);
	}
	
	@JsonIgnore
	default double sample(Sampler rng) {
		return getLink().invFn(
				getLinkLogitQuantile().interpolate(
					LinkFunction.LOGIT.fn(rng.nextDouble())
				));
	}
	
	@JsonIgnore
	@Value.Lazy
	default double getCentral() {
		RombergIntegrator tmp = new RombergIntegrator(0.001, 0.001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
		return tmp.integrate(100000, x -> x * this.getDensity(x), 
				getMinimum(), 
				getMaximum());
	};
	
	@JsonIgnore
	default double getCumulative(double x) {
		return Conversions.expit(
			getLogitLinkCDF().interpolate(
				this.getLink().fn(x)
			)
		);
	};
	
	@JsonIgnore
	default double getQuantile(double p) {
		return 
			getLink().invFn(	
				getLinkLogitQuantile().interpolate(
					LinkFunction.LOGIT.fn(p)
				)
			);
	}
	
	@JsonIgnore
	default double getMedian() {
		return getQuantile(0.5);
	}
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
	 * If this function represents an odds ratio then baselining it against
	 * the associated distribution means that the result is also a probability 
	 * distribution. This is a question of scaling this function so that the 
	 * product of odds * density integrates to one.
	 * @param odds The associated odds function
	 * @return
	 */
	default EmpiricalFunction baselineOdds(EmpiricalFunction odds) {
		RombergIntegrator tmp = new RombergIntegrator(0.0001, 0.0001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
		double total = tmp.integrate(100000, 
			x -> odds.value(x) * this.getDensity(x),
			this.getMinimum(),
			this.getMaximum()
//			Math.max(this.getMinimum(), odds.getMinimum()), 
//			Math.min(this.getMaximum(), odds.getMaximum())
		);
		
		return ImmutableEmpiricalFunction.builder()
				.setLink(getLink())
				.setX(odds.getX())
				.setY(Arrays.stream(odds.getY()).map(y -> y/total).toArray())
				.build();
	}
	
	static int KNOTS = 50;
	
	public static ImmutableEmpiricalDistribution fromData(double... data) {
		return fromData(LinkFunction.NONE, data);
	}
	
	public static ImmutableEmpiricalDistribution fromData(LinkFunction link, double... data) {
		
		Arrays.sort(data);
		Builder out = ImmutableEmpiricalDistribution.builder();
		
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
