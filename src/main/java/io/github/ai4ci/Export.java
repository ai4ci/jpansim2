package io.github.ai4ci;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.CSVWriter;
import io.github.ai4ci.flow.OutputWriter;

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Export {

	public static enum Stage {
		BASELINE, START, UPDATE, FINISH
	}
	
	public static interface Selector extends Function<Outbreak,Stream<? extends CSVWriter.Writeable>> {} 

	String value();
	Stage stage();
	int size();
	Class<? extends Selector> selector();
	
	@SuppressWarnings("rawtypes")
	Class<? extends OutputWriter> writer();
	
}
