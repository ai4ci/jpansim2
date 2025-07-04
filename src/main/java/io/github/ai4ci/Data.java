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

import io.github.ai4ci.config.DemographicAdjustment;

public @interface Data {

	@Target({ElementType.PACKAGE, ElementType.TYPE})
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	// @InjectAnnotation(type = CsvColumn.class, target = InjectAnnotation.Where.FIELD)
	// @Csv
	@Value.Style(
	    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
	    init = "set*", // Builder initialization methods will have 'set' prefix
	    deepImmutablesDetection = true,
	    with="with*",
	    toBuilder = "toBuilder",
	    strictBuilder = false,
	    underrideHashCode = "hash",
	    underrideEquals = "equality",
	    underrideToString = "print",
	    strictModifiable = false,
	    passAnnotations = {
	    		// CsvColumn.class
	    		Export.class
	    },
	    isSet = "initialised*",
	    depluralize = true, // enable feature
	    depluralizeDictionary = {"person:people"} // specifying dictionary of exceptions
	    // builder = "new" //, // construct builder using 'new' instead of factory method
	    // allows MapStruct to map immutable instance to builder.
	    //defaults = @Value.Immutable(copy = false) // Disable copy methods by default
	    ) 
	public static @interface Style {}

	@Target({ElementType.PACKAGE, ElementType.TYPE})
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	// @InjectAnnotation(type = CsvColumn.class, target = InjectAnnotation.Where.FIELD)
	// @Csv
	@Value.Style(
	    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
	    init = "set*", // Builder initialization methods will have 'set' prefix
	    create = "new",
	    deepImmutablesDetection = true,
	    with="with*",
	    toBuilder = "toBuilder",
	    strictBuilder = false,
	    strictModifiable = false,
		underrideHashCode = "hash",
	    underrideEquals = "equality",
	    underrideToString = "print",
	    passAnnotations = {
	    		// CsvColumn.class
	    },
	    isSet = "initialised*",
	    depluralize = true, // enable feature
	    depluralizeDictionary = {"person:people"} // specifying dictionary of exceptions
	    // builder = "new" //, // construct builder using 'new' instead of factory method
	    // allows MapStruct to map immutable instance to builder.
	    //defaults = @Value.Immutable(copy = false) // Disable copy methods by default
	    ) 
	public static @interface Mutable {}
	
	@Target({ElementType.PACKAGE, ElementType.TYPE})
	@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
	@JsonSerialize(typing = Typing.DYNAMIC)
	@JsonDeserialize
	// @InjectAnnotation(type = CsvColumn.class, target = InjectAnnotation.Where.FIELD)
	// @Csv
	@Value.Style(
	    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
	    init = "set*", // Builder initialization methods will have 'set' prefix
	    deepImmutablesDetection = false,
	    passAnnotations = {
	    		DemographicAdjustment.Scale.class
	    },
	    typeAbstract = "_Partial*",
	    typeImmutable = "Partial*",
	    strictModifiable = false,
	    depluralize = true, // enable feature
		underrideHashCode = "hash",
	    underrideEquals = "equality",
	    underrideToString = "print",
	    depluralizeDictionary = {"person:people"}, // specifying dictionary of exceptions
	    validationMethod = ValidationMethod.NONE
	    // builder = "new" //, // construct builder using 'new' instead of factory method
	    // allows MapStruct to map immutable instance to builder.
	    
	    )
	@SuppressWarnings("immutables")
	public static @interface Partial {}

	
//	@InjectAnnotation(type = CsvColumn.class, target = InjectAnnotation.Where.FIELD)
//	@Inherited
//	public static @interface Csv {
//	}

}