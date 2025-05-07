package io.github.ai4ci.config;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;

@Value.Immutable
@JsonSerialize(as = ImmutableFixedValueFunction.class)
@JsonDeserialize(as = ImmutableFixedValueFunction.class)
public interface FixedValueFunction extends Serializable, Abstraction.Interpolator {

	double getValue();
	default double interpolate(double x) {
		return getValue();
	}
	
	public static FixedValueFunction ofOne() {
		return ImmutableFixedValueFunction.builder().setValue(1).build();
	}
}
