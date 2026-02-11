package io.github.ai4ci.util;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.duckdb.DuckDBAppender;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Utility class for DuckDB database operations and type mapping.
 * <p>
 * This class provides functionality for appending Java objects to DuckDB tables
 * and generating SQL CREATE TABLE statements based on Java class metadata. The
 * class handles type mapping between Java types and DuckDB SQL types,
 * supporting primitive types, wrapper classes, and common Java types.
 *
 * @author Rob Challen
 * @see org.duckdb.DuckDBAppender
 * @see com.fasterxml.jackson.annotation.JsonProperty
 */

public class DuckDBUtil {

	/**
	 * Appends a Java object to a DuckDB table using type-specific appender
	 * methods.
	 * <p>
	 * This method performs type-based dispatch to invoke the appropriate
	 * DuckDBAppender method for the given object type. Supported types include
	 * primitive types, their wrapper classes, BigDecimal, LocalDateTime, and
	 * String. If the object type is not supported, an IllegalArgumentException
	 * is thrown.
	 *
	 * @param appender the DuckDBAppender instance to append data to
	 * @param value    the object to append, must be of a supported type
	 * @throws IllegalArgumentException if the object type is not supported
	 * @throws RuntimeException         if a SQLException occurs during appending
	 */
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
			} else
				throw new IllegalArgumentException(
						"Unsupported type for DuckDB Appender"
				);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates a CREATE TABLE SQL statement for a given Java class.
	 * <p>
	 * This method constructs SQL DDL statements dynamically by analysing methods
	 * annotated with {@link JsonProperty} in the specified class. The generated
	 * SQL creates a table with columns corresponding to each annotated property.
	 *
	 * @param <X>       the class type parameter
	 * @param type      the Java class to generate SQL for
	 * @param tableName the name of the table to create
	 * @return a complete CREATE TABLE SQL statement
	 */
	public static <X> String createSql(Class<X> type, String tableName) {
		return "CREATE TABLE " + tableName + " ("
				+ Arrays.stream(type.getMethods())
						.filter(c -> c.isAnnotationPresent(JsonProperty.class))
						.map(
								m -> m.getAnnotation(JsonProperty.class).value() + " "
										+ mapToDuckDBType(m.getReturnType())
						).collect(Collectors.joining(","))
				+ ")";
	}

	/**
	 * Maps Java class types to corresponding DuckDB SQL types.
	 * <p>
	 * This function implements the type mapping covering both primitive types
	 * and their corresponding wrapper classes, plus additional Java types
	 * commonly used in database applications.
	 * <p>
	 * The mapping logic handles primitive types first, then falls back to
	 * wrapper classes and common Java types. Unsupported types result in an
	 * IllegalArgumentException being thrown.
	 *
	 * @param clazz the Java class to map to a DuckDB type
	 * @return the corresponding DuckDB SQL type as a string
	 * @throws IllegalArgumentException if clazz is null or unsupported
	 */
	public static String mapToDuckDBType(Class<?> clazz) {
		if (clazz == null)
			throw new IllegalArgumentException("Class cannot be null");

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

		throw new IllegalArgumentException(
				"Unsupported type for DuckDB Appender: " + clazz.getName()
		);
	}
}