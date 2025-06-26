package io.github.ai4ci.util;

import java.util.function.DoubleUnaryOperator;

public enum LinkFunction {
	NONE (d->d, d->d), 
	LOG (d -> Math.log(d), d -> Math.exp(d)), 
	LOGIT (d -> Conversions.logit(d), d -> Conversions.expit(d));
	
	public DoubleUnaryOperator fn; 
	public DoubleUnaryOperator invFn;
	LinkFunction( DoubleUnaryOperator fn, DoubleUnaryOperator invFn) {
		this.fn = fn;
		this.invFn = invFn;
	}
	
}