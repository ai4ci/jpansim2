package io.github.ai4ci.functions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Interface for simple mathematical functions that map a single input to a
 * single output.
 *
 * <p>
 * Provides basic mathematical operations and supports JSON type deduction for
 * different function implementations including empirical, fixed-value, and
 * mathematical functions.
 */
@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes(
	{ @Type(ImmutableEmpiricalFunction.class),
			@Type(ImmutableFixedValueFunction.class),
			@Type(ImmutableMathematicalFunction.class) }
)
public interface SimpleFunction {

	/** Increment used for numerical differentiation */
	double DX = 0.00001;

	/**
	 * Computes the numerical derivative of the function at the given point.
	 *
	 * @param x the point at which to compute the derivative
	 * @return the numerical derivative using central difference method
	 */
	default double differential(double x) {
		double dy = (this.value(x + DX) - this.value(x - DX)) / (2 * DX);
		return dy;
	}

	/**
	 * Evaluates the function at the given input value.
	 *
	 * @param x the input value
	 * @return the function value at x
	 */
	double value(double x);
}