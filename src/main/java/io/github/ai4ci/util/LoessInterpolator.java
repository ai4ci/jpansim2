package io.github.ai4ci.util;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Interpolator;

import java.util.Arrays;

import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

@Value.Immutable
public interface LoessInterpolator extends Abstraction.Interpolator {

	double[] getX();
	double[] getY();
	
	@Value.Derived
	default double getMinSupport() {
		return Arrays.stream(getX()).min().getAsDouble();
	}
	
	@Value.Derived
	default double getMaxSupport() {
		return Arrays.stream(getX()).max().getAsDouble();
	}
	
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
	
	public static LoessInterpolator createLoessInterpolator(double[] x, double[] y) {
		return ImmutableLoessInterpolator.builder().setX(x).setY(y).build();
	}	
	
	@Override
	default double interpolate(double x) {
		x = Abstraction.squish(getMinSupport(), x, getMaxSupport());
		return getLoess().value(x);
	}
	
	@Override
	default double differential(double x) {
		x = Abstraction.squish(getMinSupport(), x, getMaxSupport());
		return getLoess().derivative().value(x);
	}

}
