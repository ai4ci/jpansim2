package io.github.ai4ci;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ValidationMethod;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
// import com.j256.simplecsv.common.CsvColumn;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Typing;

import io.github.ai4ci.config.Scale;
import io.github.ai4ci.flow.output.Export;

/**
 * Central annotation for configuring Immutables value types with consistent
 * styling and Jackson serialization support across the codebase.
 *
 * <p>
 * This annotation and its nested styles provide standardized configurations for
 * Immutables-generated value types, ensuring consistency in:
 * <ul>
 * <li>Accessor method naming patterns</li>
 * <li>Builder initialization patterns</li>
 * <li>Jackson serialization/deserialization behavior</li>
 * <li>Hash code, equals, and toString implementations</li>
 * <li>Pluralization handling for collection properties</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>
 * {@code @Data.Style}
 * public interface Person {
 *     String getName();
 *     int getAge();
 * }
 * </pre>
 *
 * <p>
 * Each nested annotation provides specialized styling for different use cases
 * while maintaining overall consistency across the codebase.
 *
 * @see Value.Style
 * @see JsonSerialize
 * @see JsonDeserialize
 */
public @interface Data {

	/**
	 * Standard immutable value type style with full Immutables feature support.
	 *
	 * <p>
	 * This style configures immutable types with:
	 * <ul>
	 * <li>Standard getter patterns: {@code is*} for booleans, {@code get*} for
	 * others</li>
	 * <li>Builder initialization with {@code set*} methods</li>
	 * <li>Deep immutables detection for nested immutable types</li>
	 * <li>{@code with*} methods for creating modified copies</li>
	 * <li>Automatic toBuilder() implementation</li>
	 * <li>Custom hashCode, equals, and toString underrides</li>
	 * <li>Pluralization support with custom dictionary</li>
	 * <li>Export and Import annotation pass-through</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Target Usage:</b> General-purpose immutable data transfer objects and
	 * domain models requiring full Immutables feature set.
	 *
	 * @see Export
	 * @see Import
	 */
	@Target({ ElementType.PACKAGE, ElementType.TYPE })
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	@SuppressWarnings("immutables")
	@Value.Style(get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
			init = "set*", // Builder initialization methods will have 'set' prefix
			deepImmutablesDetection = true, with = "with*", toBuilder = "toBuilder", strictBuilder = false, underrideHashCode = "hash", underrideEquals = "equality", underrideToString = "print", strictModifiable = false, passAnnotations = {
					Export.class, Import.class }, isSet = "initialised*", depluralize = true, // enable feature
			depluralizeDictionary = { "person:people" } // specifying dictionary of exceptions
	)
	public static @interface Style {
	}

	/**
	 * Mutable value type style for modifiable data structures.
	 *
	 * <p>
	 * This style configures types that support mutation with:
	 * <ul>
	 * <li>Standard getter patterns: {@code is*} for booleans, {@code get*} for
	 * others</li>
	 * <li>Builder initialization with {@code set*} methods</li>
	 * <li>Constructor creation with {@code new} instead of builder</li>
	 * <li>Deep immutables detection for nested immutable types</li>
	 * <li>{@code with*} methods for creating modified copies</li>
	 * <li>Automatic toBuilder() implementation</li>
	 * <li>Custom hashCode, equals, and toString underrides</li>
	 * <li>Pluralization support with custom dictionary</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Target Usage:</b> Mutable data structures and configuration objects where
	 * immutability is not required.
	 */
	@Target({ ElementType.PACKAGE, ElementType.TYPE })
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	@Value.Style(get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
			init = "set*", // Builder initialization methods will have 'set' prefix
			create = "new", deepImmutablesDetection = true, with = "with*", toBuilder = "toBuilder", strictBuilder = false, strictModifiable = false, underrideHashCode = "hash", underrideEquals = "equality", underrideToString = "print", passAnnotations = {
			// CsvColumn.class
			}, isSet = "initialised*", depluralize = true, // enable feature
			depluralizeDictionary = { "person:people" } // specifying dictionary of exceptions
	)
	public static @interface Mutable {
	}

	/**
	 * Partial value type style for configuration modifications and patches.
	 *
	 * <p>
	 * This style configures partial types used for:
	 * <ul>
	 * <li>Configuration modifications and overrides</li>
	 * <li>Partial object updates and patches</li>
	 * <li>Demographic adjustment scaling operations</li>
	 * </ul>
	 *
	 * <p>
	 * Key characteristics:
	 * <ul>
	 * <li>Standard getter patterns</li>
	 * <li>Builder initialization with {@code set*} methods</li>
	 * <li>No deep immutables detection (performance optimization)</li>
	 * <li>DemographicAdjustment.Scale annotation pass-through</li>
	 * <li>Type naming pattern: {@code _Partial*} for abstract, {@code Partial*} for
	 * immutable</li>
	 * <li>Custom hashCode, equals, and toString underrides</li>
	 * <li>Pluralization support with custom dictionary</li>
	 * <li>Validation disabled for partial objects</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Target Usage:</b> Configuration patches, demographic adjustments, and
	 * partial object updates.
	 *
	 * @see Scale
	 */
	@Target({ ElementType.PACKAGE, ElementType.TYPE })
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	@Value.Style(get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
			init = "set*", // Builder initialization methods will have 'set' prefix
			deepImmutablesDetection = false, passAnnotations = {
					Scale.class }, typeAbstract = "_Partial*", typeImmutable = "Partial*", strictModifiable = false, depluralize = true, // enable
																																			// feature
			underrideHashCode = "hash", underrideEquals = "equality", underrideToString = "print", depluralizeDictionary = {
					"person:people" }, // specifying dictionary of exceptions
			validationMethod = ValidationMethod.NONE)
	@SuppressWarnings("immutables")
	public static @interface Partial {
	}

	/**
	 * Repository-style configuration for data access objects and persistence
	 * entities.
	 *
	 * <p>
	 * This style optimizes types for repository and data access patterns with:
	 * <ul>
	 * <li>Standard getter patterns</li>
	 * <li>Builder initialization with {@code set*} methods</li>
	 * <li>{@code with*} methods for creating modified copies</li>
	 * <li>Automatic toBuilder() implementation</li>
	 * <li>Custom hashCode, equals, and toString underrides</li>
	 * <li>Export and Import annotation pass-through</li>
	 * <li>Pluralization support with custom dictionary</li>
	 * <li>Initialization state tracking with {@code initialised*} methods</li>
	 * </ul>
	 *
	 * <p>
	 * <b>Target Usage:</b> Data access objects, repository entities, and
	 * persistence-layer models requiring export/import capabilities.
	 *
	 * @see Export
	 * @see Import
	 */
	@Target({ ElementType.PACKAGE, ElementType.TYPE })
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	@Value.Style(get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
			init = "set*", // Builder initialization methods will have 'set' prefix
			deepImmutablesDetection = false, with = "with*", toBuilder = "toBuilder", strictBuilder = false, underrideHashCode = "hash", underrideEquals = "equality", underrideToString = "print", strictModifiable = false, passAnnotations = {
					Export.class, Import.class }, isSet = "initialised*", depluralize = true, // enable feature
			depluralizeDictionary = { "person:people" } // specifying dictionary of exceptions
	)
	public static @interface Repository {
	}

}