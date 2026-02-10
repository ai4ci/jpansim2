package io.github.ai4ci;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a data item as being importable from a csv file via the
 * {@link io.github.ai4ci.util.Repository}.
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Import {

	/**
	 * The name of the csv file to import from.
	 *
	 * @return the name of the csv file to import from
	 */
	String value();

	/**
	 * Marks a field as the unique identifier for the data item, used for indexing
	 * and lookup during import.
	 */
	@Inherited
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Id {
	}
}
