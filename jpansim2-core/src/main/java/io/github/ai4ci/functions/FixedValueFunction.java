package io.github.ai4ci.functions;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A simple function that always returns a fixed value.
 *
 * <p>
 * This interface describes a simple function that always returns the same value
 * regardless of the input. It is used as a baseline or default in cases where a
 * more complex function is not required. For example, it may be used as a
 * default contact odds function in demographic configurations or as a simple
 * parameter function in execution configurations.
 *
 * <p>
 * Downstream uses include demographic and execution configuration code such as
 * {@link io.github.ai4ci.config.setup.AgeStratifiedDemography} and
 * {@link io.github.ai4ci.config.execution.ExecutionConfiguration}.
 *
 * @author Rob Challen
 */
@Value.Immutable
@JsonSerialize(as = ImmutableFixedValueFunction.class)
@JsonDeserialize(as = ImmutableFixedValueFunction.class)
public interface FixedValueFunction extends Serializable, SimpleFunction {

	/**
	 * Creates a fixed value function that always returns the specified value.
	 *
	 * <p>
	 * This factory method allows you to create a fixed value function with any
	 * specified value. It may be used as a simple parameter function in various
	 * configurations where a constant value is sufficient.
	 *
	 * @param value the fixed value to be returned by the function
	 * @return a fixed value function that always returns the specified value
	 */
	public static FixedValueFunction of(double value) {
		return ImmutableFixedValueFunction.builder().setValue(value).build();
	}

	/**
	 * Creates a fixed value function that always returns 1.
	 *
	 * <p>
	 * This is a convenient factory method for creating a simple fixed value
	 * function with a default value of 1. It may be used as a default contact
	 * odds function or as a simple parameter function in various configurations.
	 *
	 * @return a fixed value function that always returns 1
	 */
	public static FixedValueFunction ofOne() {
		return ImmutableFixedValueFunction.builder().setValue(1).build();
	}

	/**
	 * The fixed value returned by this function.
	 *
	 * <p>
	 * This value is returned by the {@link #value(double)} method regardless of
	 * the input. It is set at construction and may be used as a simple parameter
	 * or default function in various configurations.
	 *
	 * @return the fixed value returned by this function
	 *
	 */
	double getValue();

	/**
	 * Returns the fixed value of this function.
	 *
	 * <p>
	 * This method ignores the input and always returns the value specified by
	 * {@link #getValue()}. It provides a simple implementation of the
	 * {@link SimpleFunction} interface.
	 */
	@Override
	default double value(double x) {
		return this.getValue();
	}
}
