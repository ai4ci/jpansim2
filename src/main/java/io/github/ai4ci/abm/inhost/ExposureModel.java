package io.github.ai4ci.abm.inhost;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
public interface ExposureModel extends Serializable {
	
	@Value.Immutable
	public static interface BiPhasicLogistic extends Serializable {
		
		double getGrowthRate();
		double getGrowthTime();
		double getDecayRate();
		double getDecayTime();
		
		@Value.Derived default double getUnit() {
			return f(0, getGrowthRate(), getGrowthTime());
		}
		
		public static double rate(double x1,double y1,double x2,double y2) {
			double tmp = (log((1-y2)/y2) - log((1-y1)/y1))/(x1 - x2);
			if (tmp < 0) {
				throw new RuntimeException();
			}
			return tmp;
		}
		
		public static double time(double x1,double y1,double x2,double y2) {
			double r = rate(x1,y1,x2,y2);
			return 1/r * log((1-y1)/y1) + x1;
		}
		
		private static double f(double x, double r, double s) {
			return 1/(1+exp(-r*(x-s)));
		}
		
		public static double invF(double y, double r, double s) {
			return s - log((1-y)/y) / r;
		}
		
		default double adjusted(double x, double growthOffset, double decayOffset) {
			return f(x, this.getGrowthRate(), this.getGrowthTime()-growthOffset) * 
				(1-f(x, this.getDecayRate(), this.getDecayTime()-decayOffset));
		}
		
		public static BiPhasicLogistic calibrateViralLoad(
				double onsetTime, double peakDelay, double postPeakDuration, 
				double thresholdLevel, double peakLevel
			) {
			peakLevel = sqrt(peakLevel);
			return ImmutableBiPhasicLogistic.builder()
				.setGrowthRate(BiPhasicLogistic.rate(onsetTime,thresholdLevel, onsetTime+peakDelay, peakLevel))
				.setGrowthTime(BiPhasicLogistic.time(onsetTime,thresholdLevel, onsetTime+peakDelay, peakLevel))
				.setDecayRate(BiPhasicLogistic.rate( onsetTime+peakDelay, 1-peakLevel, onsetTime+peakDelay+postPeakDuration, 1-thresholdLevel))
				.setDecayTime(BiPhasicLogistic.time( onsetTime+peakDelay, 1-peakLevel, onsetTime+peakDelay+postPeakDuration, 1-thresholdLevel))
				.build();
		}
		
		public static BiPhasicLogistic calibrateImmuneActivity(
				double peakTime, double peakLevel, double halfLife
			) {
			double halfPeakLevel =peakLevel/2;
			peakLevel = sqrt(peakLevel);
			return ImmutableBiPhasicLogistic.builder()
				.setGrowthRate(BiPhasicLogistic.rate(0,0.01, peakTime, peakLevel))
				.setGrowthTime(BiPhasicLogistic.time(0,0.01, peakTime, peakLevel))
				.setDecayRate(BiPhasicLogistic.rate(peakTime, 1-peakLevel, peakTime+halfLife, 1-halfPeakLevel))
				.setDecayTime(BiPhasicLogistic.time(peakTime, 1-peakLevel, peakTime+halfLife, 1-halfPeakLevel))
				.build();
		}
		
		default double unadjusted(int x) {
			return f(x, this.getGrowthRate(), this.getGrowthTime()) * 
				(1-f(x, this.getDecayRate(), this.getDecayTime()));
		}
		
	}

	double MIN_IMMUNE_ACTIVITY = 0.001D;
		
	public int getExposureTime();
	public double getNormalisedExposure();
	public double getImmuneActivityAtExposure();
	double getGrowthOffset();
	double getDecayOffset();
	
	default double getExposureViralLoad(int time, BiPhasicLogistic model) {
		if (time <= this.getExposureTime()) return 0;
		return model.adjusted(
				time-getExposureTime(), 
				getGrowthOffset(), getDecayOffset());
	}
	
	default double getExposureImmuneActivity(int time, BiPhasicLogistic model) {
		if (time <= this.getExposureTime()) return 0;
		return model.unadjusted(time-getExposureTime()); 
	}
	
	default boolean isIrrelevant(int time, BiPhasicLogistic model) {
		return this.getExposureTime() + 
				BiPhasicLogistic.invF(0.99,model.getDecayRate(),model.getDecayTime()) < time;
	};

	public static ExposureModel createExposure(double exposure, double immuneActivity, BiPhasicLogistic viralLoad, int time) {
		
		double dose = exposure * viralLoad.getUnit();
		
		// Shift the growth curve backwards such that the growth curve starts at the dose
		// at time zero. For exposures <1 this will be a shift forward and a 
		// negative number
		double growthOffset = BiPhasicLogistic.invF(dose, 
				viralLoad.getGrowthRate(), 
				viralLoad.getGrowthTime());
		
		// Shift the decay curve such that the decay component is the same as
		// the current immune activity. Never shift it forwards
		double decayOffset = Math.max(0,BiPhasicLogistic.invF(
				immuneActivity, 
				viralLoad.getDecayRate(), 
				viralLoad.getDecayTime()));
		
		return ImmutableExposureModel.builder()
			.setDecayOffset(decayOffset)
			.setExposureTime(time)
			.setGrowthOffset(growthOffset)
			.setImmuneActivityAtExposure(immuneActivity)
			.setNormalisedExposure(exposure)
			.build();
	}
	
}