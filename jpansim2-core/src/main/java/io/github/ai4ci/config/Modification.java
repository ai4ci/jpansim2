package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Interface for defining modifications to model configurations and parameters.
 *
 * <p>
 * This interface provides polymorphic support for different types of model
 * modifications through JSON type deduction, allowing for flexible
 * configuration updates during model execution and setup phases.
 *
 * @param <X> the type of object being modified
 */
@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes(
	{ @Type(PartialExecutionConfiguration.class),
			@Type(PartialStochasticModel.class),
			@Type(PartialPhenomenologicalModel.class),
			@Type(PartialMarkovStateModel.class),
			@Type(PartialSetupConfiguration.class) }
)
public interface Modification<X> {
	/**
	 * Returns the modified object instance itself.
	 *
	 * @return the modified object instance
	 */
	@Value.NonAttribute
	X self();
}