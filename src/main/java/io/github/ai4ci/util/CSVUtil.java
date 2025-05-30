package io.github.ai4ci.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CSVUtil {
	
	// TODO: Use of reflection for creating CSV files involves performance hit. 
	
	public static String csvFrom(Object... objects) {
		StringBuilder s = new StringBuilder();
		boolean sep = false;
		for (Object o: objects) {
			s.append((sep ? "," : "") + valueOf(o));
			sep = true;
		}
		return s.toString();
	}
	
	private static String valueOf(Object object) {
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
	
	public static <X> String headers(Class<X> clazz) {
		return Arrays.stream(clazz.getMethods())
			.filter(c -> c.isAnnotationPresent(JsonProperty.class))
			.map(c -> c.getAnnotation(JsonProperty.class).value())
			.collect(Collectors.joining(","));
	}
	
	public static <X> String row(X value) {
		return Arrays.stream(value.getClass().getMethods())
			.filter(c -> c.isAnnotationPresent(JsonProperty.class))
			.map(m -> {
				try {
					return valueOf(m.invoke(value));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			})
			.collect(Collectors.joining(","));
	}
	
//	private static String uncapitalize(String s) {
//		if (s.startsWith("is")) s = s.substring(2);
//		if (s.startsWith("get")) s = s.substring(3);
//        return s.substring(0, 1).toLowerCase() + s.substring(1);
//    }
}
