package io.github.ai4ci.util;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;

@Value.Immutable
@JsonSerialize(as = ImmutableFixedValueFunction.class)
@JsonDeserialize(as = ImmutableFixedValueFunction.class)
public interface FixedValueFunction extends Serializable, Abstraction.SimpleFunction {

	double getValue();
	default double value(double x) {
		return getValue();
	}
	
	public static FixedValueFunction ofOne() {
		return ImmutableFixedValueFunction.builder().setValue(1).build();
	}
	
	public static FixedValueFunction of(double value) {
		return ImmutableFixedValueFunction.builder().setValue(value).build();
	}
}
