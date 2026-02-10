package io.github.ai4ci.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for factors that can be represented as enums with optional level labels.
 * Provides methods for getting the label of an enum constant and for constructing
 * an enum constant from a label. Uses memoisation to cache mappings between enum
 * names and labels for efficient lookup.
 *
 * <p>Enum constants can be annotated with {@link Level} to specify a custom label;
 * if not annotated, the enum name is used as the label by default.
 *
 * @author Rob Challen
 */
public interface Factor {
	
	/**
	 * Annotation for specifying a custom label for an enum constant.
	 *
	 * <p>When applied to an enum constant, the value of this annotation is used
	 * as the label for that constant. If not present, the enum name is used as
	 * the label by default.
	 */
	@Inherited
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Level {
		/**
		 * The custom label for the enum constant.
		 *
		 * @return the label to use for the annotated enum constant
		 */
		String value();
	}
	
	/** Memoised functions for forward and reverse lookups between enum names and labels.
	 * The forward mapping is from enum name to label, and the reverse mapping is from
	 * label to enum name. These mappings are computed once per enum class and cached
	 * for efficient retrieval.
	 */
	static final Memoised<Class<? extends Enum<?>>, Map<String,String>> forward = 
			Memoised.memoise(
					c -> {
						Map<String,String> lookup = new HashMap<>();
						for (Field e: c.getDeclaredFields()) {
							lookup.put(
									//Enum name
									e.getName(),
									//Level
									e.isAnnotationPresent(Level.class) ? 
											e.getAnnotation(Level.class).value() :
											e.getName());
						}
						return lookup;
					}
			);
	
	/**
	 * Memoised function for reverse lookup from label to enum name.
	 * This function computes a mapping from labels to enum names for a given enum class.
	 * It iterates over the declared fields of the enum class and constructs a lookup map
	 * where the key is the label (either from the Level annotation or the enum name)
	 * and the value is the corresponding enum name. This allows for efficient retrieval of
	 * enum constants based on their labels.
	 */
	static final Memoised<Class<? extends Enum<?>>, Map<String,String>> reverse = 
			Memoised.memoise(
					c -> {
						Map<String,String> lookup = new HashMap<>();
						for (Field e: c.getDeclaredFields()) {
							lookup.put( 
									//Level
									e.isAnnotationPresent(Level.class) ? 
											e.getAnnotation(Level.class).value() :
											e.getName(),
									//Enum name
									e.getName());
						}
						return lookup;
					}
			);
	
	/**
	 * Get the label for this factor.
	 *
	 * <p>If this factor is an enum constant, the label is determined by checking for
	 * the presence of the {@link Level} annotation on the corresponding field. If
	 * the annotation is present, its value is used as the label; otherwise, the
	 * enum name is used as the default label. If this factor is not an enum
	 * constant, the string representation of this factor is returned as the label.
	 *
	 * @param <T> the type of the enum constant, if applicable
	 * @return the label associated with this factor
	 */
	@SuppressWarnings("unchecked")
	default <T extends Enum<T>> String getLabel() {
		if (this.getClass().isEnum()) {
			T tmp = (T) this;
			Map<String,String> mapping = forward.apply((Class<T>) tmp.getClass());
			return mapping.getOrDefault(tmp.name(), tmp.name());
		}
		return this.toString();
	}
	
	/**
	 * Construct an enum constant of the specified type from a given label.
	 *
	 * <p>This method uses the reverse mapping to look up the enum name corresponding
	 * to the provided label. If the label is found in the mapping, the corresponding
	 * enum constant is returned; otherwise, it falls back to using the label as the
	 * enum name directly. This allows for flexible construction of enum constants
	 * based on either their labels or their names.
	 *
	 * @param <T> the type of the enum constant to construct
	 * @param type the class of the enum
	 * @param value the label to look up
	 * @return the enum constant corresponding to the given label
	 * @throws IllegalArgumentException if the enum type does not contain a constant with the specified label
	 */
	@SuppressWarnings("unchecked")
	static <T extends Enum<T>> Enum<T> fromLabel(Class<? extends Enum<?>> type, String value) {
		Map<String,String> mapping = reverse.apply(type);
		return Enum.valueOf((Class<T>) type, mapping.getOrDefault(value, value));
	}
	
	
	
}