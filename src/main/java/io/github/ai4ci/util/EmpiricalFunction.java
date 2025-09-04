package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.util.Coordinates.DuplicateResolution;

@Value.Immutable
@JsonSerialize(as = ImmutableEmpiricalFunction.class)
@JsonDeserialize(as = ImmutableEmpiricalFunction.class)
public interface EmpiricalFunction extends Serializable, Abstraction.SimpleFunction {
 
	double[] getX();
	double[] getY();
	@Value.Default default LinkFunction getLink() {return LinkFunction.NONE;}
	
	@Value.Default default double getMinimum() {
		return Arrays.stream(getX()).min().getAsDouble();
	}
	
	@Value.Default default double getMaximum() {
		return Arrays.stream(getX()).max().getAsDouble();
	}
	
	@JsonIgnore
	@Value.Derived 
	default Abstraction.Interpolator getInterpolator() {

		Coordinates tmp = ImmutableCoordinates.builder()
			.setX(getX())
			.setY(getY())
			.setXMin(getMinimum())
			.setXMax(getMaximum())
			.setXLink(LinkFunction.NONE)
			.setYLink(getLink())
			.setIncreasing(false)
			.setResolveDuplicates(DuplicateResolution.MEAN)
			.build();
	
//		if (getX().length < 20) {
		
		return SplineInterpolator.createCubicSpline(
				tmp.getHx(), tmp.getHy()
		);
		
//		} else {
//			return LoessInterpolator.createLoessInterpolator(tmp.getHx(), tmp.getHy());
//		}
	}
	
	default double value(double x) {
		return getLink().invFn(getInterpolator().interpolate(x));
	}
	
	default double differential(double x) {
		return getLink().derivInvFn(
				getInterpolator().interpolate(x)
		) * getInterpolator().differential(x);
		
	}
	
}
