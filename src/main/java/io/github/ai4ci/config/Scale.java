package io.github.ai4ci.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.ai4ci.util.Conversions;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scale {
	enum ScaleType {
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

	Scale.ScaleType value() default Scale.ScaleType.ODDS;
}