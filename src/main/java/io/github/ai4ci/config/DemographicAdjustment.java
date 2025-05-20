package io.github.ai4ci.config;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.mechanics.Abstraction.SimpleFunction;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.EmpiricalFunction.Link;
import io.github.ai4ci.util.FixedValueFunction;
import io.github.ai4ci.util.ImmutableEmpiricalFunction;

public interface DemographicAdjustment {
	
	@Value.Immutable @Partial
	@JsonSerialize(as = PartialDemographicAdjustment.class)
	@JsonDeserialize(as = PartialDemographicAdjustment.class)
	public interface _PartialDemographicAdjustment extends 
		DemographicAdjustment.Execution<SimpleFunction, SimpleFunction>,
		DemographicAdjustment.Phenomenological<SimpleFunction, SimpleFunction>,
		DemographicAdjustment.Markov<SimpleFunction, SimpleFunction>,
		Serializable
	{}

	public static PartialDemographicAdjustment EMPTY = 
			PartialDemographicAdjustment.builder().build();
	
	public static PartialDemographicAdjustment AGE_DEFAULT =
			PartialDemographicAdjustment.builder()
			.setContactProbability(
					ImmutableEmpiricalFunction.builder()
						.setX(0,5,15,25,45,75)
						.setY(2,1,1.5,1.25,0.8,1.2)
						.setLink(Link.LOG)
						.build()	
				)
			.setAppUseProbability(FixedValueFunction.ofOne())
			.setComplianceProbability(FixedValueFunction.ofOne())
			.setMaximumSocialContactReduction(FixedValueFunction.ofOne())
			.setIncubationPeriod(FixedValueFunction.ofOne())
			.setPeakToRecoveryDelay(
					ImmutableEmpiricalFunction.builder()
					.setX(0,5,15,25,45,65)
					.setY(1,0.5,0.5,0.75,1,2)
					.setLink(Link.LOG)
					.build()
			)
			.setImmuneWaningHalfLife(
					ImmutableEmpiricalFunction.builder()
					.setX(0,5,15,25,45,65,85)
					.setY(1,2,2,1.5,1,0.5,0.25)
					.setLink(Link.LOG)
					.build()
			)
			.build();
			
	
	@Inherited
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Scale {
		ScaleType value() default ScaleType.ODDS;
	}
	
	public static enum ScaleType {
		ODDS, FACTOR, POWER; 
		
		public static double scale(double base, double factor, ScaleType scale) {
			switch (scale) {
				case FACTOR:
					return base * factor;
				case ODDS:
					return Conversions.scaleProbabilityByOR(base, factor);
				case POWER:
					return Math.pow(base, factor);
				}
			throw new RuntimeException();
		}
	}
	
	public static interface Execution<DIST,NUMERIC> {
		@Scale(ScaleType.ODDS) DIST getContactProbability();
		@Scale(ScaleType.ODDS) NUMERIC getAsymptomaticFraction();
		@Scale(ScaleType.ODDS) NUMERIC getCaseHospitalisationRate();
		@Scale(ScaleType.ODDS) NUMERIC getCaseFatalityRate();
		@Scale(ScaleType.ODDS) DIST getComplianceProbability();
		@Scale(ScaleType.ODDS) DIST getAppUseProbability();
		@Scale(ScaleType.ODDS) DIST getMaximumSocialContactReduction();
	} 
	
	public static interface Phenomenological<DIST,NUMERIC> {
		@Scale(ScaleType.FACTOR) DIST getIncubationPeriod();
		@Scale(ScaleType.FACTOR) DIST getPeakToRecoveryDelay();
		@Scale(ScaleType.FACTOR) DIST getImmuneWaningHalfLife();
	}
	
	public static interface Markov<DIST,NUMERIC> {
		@Scale(ScaleType.FACTOR) DIST getIncubationPeriod();
		@Scale(ScaleType.FACTOR) DIST getImmuneWaningHalfLife();
		@Scale(ScaleType.FACTOR) DIST getInfectiousDuration();
		@Scale(ScaleType.FACTOR) DIST getSymptomDuration();
		
	}
	
}
