package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalFunction.class)
@JsonDeserialize(as = ImmutableEmpiricalFunction.class)
public interface EmpiricalFunction extends Serializable {
 
	Map<Double,Double> getDataPoints();
	
	@JsonIgnore
	@Value.Derived default double getMin() {
		return getDataPoints().keySet().stream().mapToDouble(d -> d).min().orElse(0);
	}
	
	@JsonIgnore
	@Value.Derived default double getMax() {
		return getDataPoints().keySet().stream().mapToDouble(d -> d).max().orElse(0);
	}
	
	@JsonIgnore
	@Value.Derived 
	default SplineInterpolator getInterpolator() {
		double[] x = getDataPoints().keySet().stream().mapToDouble(d->d).toArray();
		double[] y = Arrays.stream(x).map(x1 -> getDataPoints().get(x1)).toArray();
		return SplineInterpolator.createMonotoneCubicSpline(x, y);
	}
	
	default double interpolate(double x) {
		if (x<getMin()) return getDataPoints().get(getMin());
		if (x>getMax()) return getDataPoints().get(getMax());
		return getInterpolator().interpolate(x);
	}
}
