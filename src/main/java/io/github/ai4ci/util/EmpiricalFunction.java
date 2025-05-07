package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalFunction.class)
@JsonDeserialize(as = ImmutableEmpiricalFunction.class)
public interface EmpiricalFunction extends Serializable, Abstraction.Interpolator {
 
	public static enum Link {
		NONE (d->d, d->d), 
		LOG (d -> Math.log(d), d -> Math.exp(d)), 
		LOGIT (d -> Conversions.logit(d), d -> Conversions.expit(d));
		
		public DoubleUnaryOperator fn; public DoubleUnaryOperator invFn;
		Link( DoubleUnaryOperator fn, DoubleUnaryOperator invFn) {
			this.fn = fn;
			this.invFn = invFn;
		}
		
	}
	
	double[] getX();
	double[] getY();
	@Value.Default default Link getLink() {return Link.NONE;}
	
	private Map<Double,Double> getDataPoints() {
		Map<Double,Double> tmp = new HashMap<>();
		if (getX().length != getY().length) throw new RuntimeException("Mismatched inputs"); 
		for (int i = 0; i < getX().length; i++) {
			tmp.put(getX()[i], getY()[i]);
		}
		return tmp;
	}
	
	@JsonIgnore
	@Value.Derived default double getMin() {
		return getDataPoints().keySet().stream().mapToDouble(d -> d).min()
				.orElse(getLink().invFn.applyAsDouble(0D));
	}
	
	
	
	@JsonIgnore
	@Value.Derived default double getMax() {
		return getDataPoints().keySet().stream().mapToDouble(d->d)
			.max()
			.orElse(getLink().invFn.applyAsDouble(0D));
	}
	
	@JsonIgnore
	@Value.Derived 
	default SplineInterpolator getInterpolator() {
		double[] x = getDataPoints().keySet().stream().mapToDouble(d->d).toArray();
		Arrays.sort(x);
		double[] y = Arrays.stream(x).map(x1 -> getDataPoints().get(x1))
				.map(getLink().fn)
				.toArray();
		return SplineInterpolator.createMonotoneCubicSpline(x, y);
	}
	
	
	default double interpolate(double x) {
		if (x<getMin()) return getDataPoints().get(getMin());
		if (x>getMax()) return getDataPoints().get(getMax());
		return getLink().invFn.applyAsDouble(getInterpolator().interpolate(x));
	}
	
	default EmpiricalFunction normalise(EmpiricalDistribution input) {
		RombergIntegrator tmp = new RombergIntegrator(0.0001, 0.0001, RombergIntegrator.DEFAULT_MIN_ITERATIONS_COUNT  ,RombergIntegrator.ROMBERG_MAX_ITERATIONS_COUNT);
		double total = tmp.integrate(100000, 
				x -> this.interpolate(x) * input.getDensity(x), 
				input.getMinimum(), input.getMaximum());
		
		return ImmutableEmpiricalFunction.builder()
				.setLink(getLink())
				.setX(getX())
				.setY(Arrays.stream(getY()).map(y -> y/total).toArray())
				.build();
	}
}
