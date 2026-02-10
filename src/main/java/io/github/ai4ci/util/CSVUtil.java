package io.github.ai4ci.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.ai4ci.flow.output.OutputWriter.Writeable;

/**
 * Utility class for converting objects to CSV format.
 * <p>
 * This class provides functionality to transform objects annotated with {@link JsonProperty}
 * into CSV rows with appropriate escaping for CSV special characters. The transformation
 * follows the standard CSV format specification where fields may be quoted if they contain
 * commas, quotes, or newlines.
 * <p>
 * Each property value is sanitised according to the following rules:
 * <ul>
 *   <li>Null values are represented as "NA"</li>
 *   <li>Strings containing commas, quotes, or newlines are quoted</li>
 *   <li>Quotes within strings are escaped by doubling them</li>
 * </ul>
 *
 * @param <X> the type of objects to be serialised, must implement Writeable interface
 * @author Rob Challen
 * @see com.fasterxml.jackson.annotation.JsonProperty
 * @see io.github.ai4ci.flow.output.OutputWriter.Writeable
 */
public class CSVUtil<X extends Writeable> {
	
	// TODO: Use of reflection for creating CSV files involves performance hit.
	// See @Data immutables annotation and commented out code here
	// Datatype<X> dt;
	Class<X> type;
	Method[] methods;
	
	/**
	 * Constructs a new CSVUtil instance for the specified class.
	 * <p>
	 * The constructor initialises the utility by scanning all methods annotated
	 * with {@link JsonProperty} in the provided class. These methods are used
	 * to extract property values during CSV serialisation.
	 *
	 * @param clazz the class type parameterised by X that implements Writeable
	 * @throws IllegalArgumentException if clazz is null
	 */
	public CSVUtil(Class<X> clazz) {
		this.type = clazz;
//		this.dt = ReflectionUtils.datatype(clazz);
		this.methods = Arrays.stream(clazz.getMethods())
			.filter(c -> c.isAnnotationPresent(JsonProperty.class))
			.toArray(i -> new Method[i]);
	}
	
	/**
	 * Generates a CSV-formatted string from an array of objects.
	 * <p>
	 * This static method converts individual values to CSV format and concatenates
	 * them with comma separators. 
	 *
	 * @param objects the objects to convert to CSV format
	 * @return a CSV-formatted string of the input objects
	 */
	public static String csvFrom(Object... objects) {
		StringBuilder s = new StringBuilder();
		boolean sep = false;
		for (Object o: objects) {
			s.append((sep ? "," : "") + csvString(o));
			sep = true;
		}
		return s.toString();
	}
	
	/**
	 * Sanitises an individual object value for CSV format.
	 * <p>
	 * This method applies the following rules to convert an object to a CSV-compatible string:
	 * <ul>
	 *  <li>Null values are represented as "NA"</li>
	 *  <li>Strings containing commas, quotes, or newlines are enclosed in double quotes</li>
	 *  <li>Double quotes within strings are escaped by doubling them</li>
	 *  <li>Non-string objects are converted to their string representation</li>
	 * </ul>
	 *  
	 * @param object the object to sanitise for CSV format
	 * @return the sanitised CSV value as a string
	 */
	private static String csvString(Object object) {
		if (object == null) return "NA";
		if (object instanceof String) {
			String value = (String) object;
			if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
	            value = value.replace("\"", "\"\"");
	            return "\"" + value + "\"";
	        }
	        return value;
		};
		return object.toString();
	}
	
	/**
	 * Generates the CSV header row containing property names.
	 * <p>
	 * The header row is composed of the {@link JsonProperty} annotation values
	 * extracted from all annotated methods in the class. 
	 * 
	 * @return the CSV header row as a comma-separated string
	 */
	public String headers() {
		return Arrays.stream(methods)
			.map(c -> c.getAnnotation(JsonProperty.class).value())
			.collect(Collectors.joining(","));
	}
	
	/**
	 * Generates a CSV row from the specified object instance.
	 * <p>
	 * This method extracts property values from the object and converts them
	 * to CSV format. 
	 *
	 * @param value the object instance to serialise as a CSV row
	 * @return the object's data formatted as a CSV row
	 * @throws RuntimeException if property access fails during reflection
	 */
	public String row(X value) {
		return fieldsStream(value)
			.map(o -> csvString(o))
			.collect(Collectors.joining(","));
	}
	
//	private static String uncapitalize(String s) {
//		if (s.startsWith("is")) s = s.substring(2);
//		if (s.startsWith("get")) s = s.substring(3);
//        return s.substring(0, 1).toLowerCase() + s.substring(1);
//    }
	
	/**
	 * Creates a stream of field values from the specified object.
	 * <p>
	 * This method provides access to the property values as a stream,
	 * allowing for further stream processing operations. The stream is
	 * constructed from the array of values obtained by invoking annotated
	 * methods on the object instance.
	 *
	 * @param tmp the object instance to extract field values from
	 * @return a stream containing property values in declared order
	 */
	public Stream<Object> fieldsStream(X tmp) {
		return Arrays.stream(valuesOf(tmp));
	}

	/**
	 * Extracts property values from an object instance via reflection.
	 * <p>
	 * This method invokes all methods annotated with {@link JsonProperty}
	 * on the specified object to obtain the corresponding property values.
	 * The values are returned in the same order as the methods array.
	 * 
	 *
	 * @param single the object instance to extract values from
	 * @return an array of property values in declaration order
	 * @throws RuntimeException if reflection invocation fails
	 */
	public Object[] valuesOf(X single) {
//		if (dt != null) {
//			// process using mirror construct.
//			return dt.features().stream().map(f -> dt.get(f, single))
//					.toArray();
//		}
		Object[] out = new Object[methods.length];
		for (int i=0; i<methods.length; i++) {
			try {
				out[i] = methods[i].invoke(single);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		return out;
	}
}
