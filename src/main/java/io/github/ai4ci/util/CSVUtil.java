package io.github.ai4ci.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.ai4ci.flow.CSVWriter.Writeable;

public class CSVUtil<X extends Writeable> {
	
	// TODO: Use of reflection for creating CSV files involves performance hit.
	
	Class<X> type;
	Method[] methods;
	
	public CSVUtil(Class<X> clazz) {
		this.type = clazz;
		this.methods = Arrays.stream(clazz.getMethods())
			.filter(c -> c.isAnnotationPresent(JsonProperty.class))
			.toArray(i -> new Method[i]);
	}
	
	public static String csvFrom(Object... objects) {
		StringBuilder s = new StringBuilder();
		boolean sep = false;
		for (Object o: objects) {
			s.append((sep ? "," : "") + csvString(o));
			sep = true;
		}
		return s.toString();
	}
	
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
	
	public String headers() {
		return Arrays.stream(methods)
			.map(c -> c.getAnnotation(JsonProperty.class).value())
			.collect(Collectors.joining(","));
	}
	
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
	
	public Stream<Object> fieldsStream(X tmp) {
		return Arrays.stream(valuesOf(tmp));
	}

	public Object[] valuesOf(X single) {
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
