package io.github.ai4ci.util;

import java.util.function.DoubleUnaryOperator;

import io.github.ai4ci.abm.mechanics.Abstraction;

public enum LinkFunction {
	
	NONE ( 
			d->d , 
			d->d, 
			d -> 1.0, // derivative of identity is 1 (doh)
			d -> 1.0,
			Math.pow(10,8)
		), 
	LOG (
			d -> Math.log(d), 
			d -> Math.exp(d), 
			d-> 1.0/d, 
			d -> Math.exp(d),
			Math.log(10)*8 
		), 
	LOGIT (
			d -> Conversions.logit(d), 
			d -> Conversions.expit(d), 
			d -> 1.0/d + 1.0/(1.0-d),
			d -> Conversions.expit(d) * (1-Conversions.expit(d)),
			Conversions.logit(1-Math.pow(10, -8))
		);
	
	public DoubleUnaryOperator fn; 
	public DoubleUnaryOperator invFn;
	public DoubleUnaryOperator derivFn;
	public DoubleUnaryOperator derivInvFn;
	public double lim;
	
	LinkFunction( DoubleUnaryOperator fn, DoubleUnaryOperator invFn, DoubleUnaryOperator derivFn,DoubleUnaryOperator derivInvFn, double lim) {
		this.fn = fn;
		this.invFn = invFn;
		this.derivFn = derivFn;
		this.derivInvFn = derivInvFn;
		this.lim = lim;
	}
	
//	double minHX(double minX) {
//		// hard limits are +/- infinity on the log scale.
//		// we have to bound these though. we will be using 10^-8 and 1-10^8 on 
//		// the logit scale (+/- 20) for Y rather than infinity therefore lets use +/-10^8
//		// on the link scale as hard limits. Outside of this range we will clip
//		// values in the interpolator.
//		return Math.max(-lim, fn(minX) );
//	}
//	
//	double maxHX(double maxX) {
//		return Math.min(lim, fn(maxX) );
//	}
	
	public double getMinSupport() {
		return invFn(-lim);
	}
	
	public double getMaxSupport() {
		return invFn(lim);
	}
	
	public double fn(double x) {
		double hx = fn.applyAsDouble(x);
		return Abstraction.squish(-lim, hx, lim);
	}
	public double invFn(double y) {
		y = Abstraction.squish(-lim, y, lim);
		return invFn.applyAsDouble(y);
	}

	public double derivFn(double x) {
		return derivFn.applyAsDouble(x);
	}
	
	public double derivInvFn(double y) {
		y = Abstraction.squish(-lim, y, lim);
		return derivInvFn.applyAsDouble(y);
	}

	public boolean inSupport(double x) {
		return fn(x) > -lim && fn(x) < lim;
	}
	
	
}