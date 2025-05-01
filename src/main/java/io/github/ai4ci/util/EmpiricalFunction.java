package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalFunction.class)
@JsonDeserialize(as = ImmutableEmpiricalFunction.class)
public interface EmpiricalFunction extends Serializable {
 
	public static interface Builder {
		public ImmutableEmpiricalFunction.Builder addData(double[] data);
		public default ImmutableEmpiricalFunction.Builder putDataPoint(double... data) {
			return this.addData(data);
		};
	}
	
	public static enum Link {
		NONE, LOG, LOGIT
	}
	
	List<double[]> getData();
	@Value.Default default Link getLink() {return Link.NONE;}
	
	private Map<Double,Double> getDataPoints() {
		Map<Double,Double> tmp = new HashMap<>();
		getData().stream().forEach(c -> tmp.put(c[0], c[1]));
		return tmp;
	}
	
	@JsonIgnore
	@Value.Derived default double getMin() {
		return getDataPoints().keySet().stream().mapToDouble(d -> d).min()
				.orElse(invLinkFn().applyAsDouble(0D));
	}
	
	private DoubleUnaryOperator linkFn() {
		if (getLink().equals(Link.LOG)) return d -> Math.log(d);
		if (getLink().equals(Link.LOGIT)) return d -> Conversions.logit(d);
		return d -> d;
	}
	
	private DoubleUnaryOperator invLinkFn() {
		if (getLink().equals(Link.LOG)) return d -> Math.exp(d);
		if (getLink().equals(Link.LOGIT)) return d -> Conversions.expit(d);
		return d -> d;
	}
	
	@JsonIgnore
	@Value.Derived default double getMax() {
		return getDataPoints().keySet().stream().mapToDouble(d->d)
			.max()
			.orElse(invLinkFn().applyAsDouble(0D));
	}
	
	@JsonIgnore
	@Value.Derived 
	default SplineInterpolator getInterpolator() {
		double[] x = getDataPoints().keySet().stream().mapToDouble(d->d).toArray();
		Arrays.sort(x);
		double[] y = Arrays.stream(x).map(x1 -> getDataPoints().get(x1))
				.map(linkFn())
				.toArray();
		return SplineInterpolator.createMonotoneCubicSpline(x, y);
	}
	
	default double interpolate(double x) {
		if (x<getMin()) return getDataPoints().get(getMin());
		if (x>getMax()) return getDataPoints().get(getMax());
		return invLinkFn().applyAsDouble(getInterpolator().interpolate(x));
	}
}
