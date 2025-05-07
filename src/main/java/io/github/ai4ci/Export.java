package io.github.ai4ci;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Export {

	public static enum Stage {
		BASELINE, START, UPDATE, FINISH
	}

	String value();
	Stage stage();
	int size();
	
}
