package io.github.ai4ci.util;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.duckdb.DuckDBAppender;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DuckDBUtil {

	public static void append(DuckDBAppender appender, Object value) {
		try {
			if (value instanceof Boolean) {
				appender.append(((Boolean) value).booleanValue());
			} else if (value instanceof Byte) {
				appender.append(((Byte) value).byteValue());
			} else if (value instanceof Short) {
				appender.append(((Short) value).shortValue());
			} else if (value instanceof Integer) {
				appender.append(((Integer) value).intValue());
			} else if (value instanceof Long) {
				appender.append(((Long) value).longValue());
			} else if (value instanceof Float) {
				appender.append(((Float) value).floatValue());
			} else if (value instanceof Double) {
				appender.append(((Double) value).doubleValue());
			} else if (value instanceof String) {
				appender.append((String) value);
			} else if (value instanceof BigDecimal) {
				appender.appendBigDecimal((BigDecimal) value);
			} else if (value instanceof LocalDateTime) {
				appender.appendLocalDateTime((LocalDateTime) value);
			} else {
				throw new IllegalArgumentException("Unsupported type for DuckDB Appender");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static <X> String createSql(Class<X> type, String tableName) {
		return "CREATE TABLE "+tableName+" ("+
				Arrays.stream(type.getMethods())
		.filter(c -> c.isAnnotationPresent(JsonProperty.class))
		.map(m ->
		m.getAnnotation(JsonProperty.class).value() +" "+
		mapToDuckDBType(m.getReturnType())
				).collect(Collectors.joining(","))+
		")"
		;
	}

	public static String mapToDuckDBType(Class<?> clazz) {
		if (clazz == null) {
			throw new IllegalArgumentException("Class cannot be null");
		}

		if (clazz.isPrimitive()) {
			if (clazz == boolean.class) return "BOOLEAN";
			if (clazz == byte.class) return "TINYINT";
			if (clazz == short.class) return "SMALLINT";
			if (clazz == int.class) return "INTEGER";
			if (clazz == long.class) return "BIGINT";
			if (clazz == float.class) return "FLOAT";
			if (clazz == double.class) return "DOUBLE";
		} else {
			if (clazz == Boolean.class) return "BOOLEAN";
			if (clazz == Byte.class) return "TINYINT";
			if (clazz == Short.class) return "SMALLINT";
			if (clazz == Integer.class) return "INTEGER";
			if (clazz == Long.class) return "BIGINT";
			if (clazz == Float.class) return "FLOAT";
			if (clazz == Double.class) return "DOUBLE";
			if (clazz == BigDecimal.class) return "DECIMAL";
			if (clazz == LocalDateTime.class) return "TIMESTAMP";
			if (clazz == String.class) return "VARCHAR";
		}

		throw new IllegalArgumentException("Unsupported type for DuckDB Appender: " + clazz.getName());
	}
}
