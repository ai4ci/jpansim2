package io.github.ai4ci.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Abstraction;

/**
 * Represents a facet of execution configuration in the JPanSim2 framework. This
 * interface extends {@link Abstraction.Named} and provides a mechanism to
 * specify modifications to the baseline execution configuration from
 * {@link ExperimentConfiguration#getExecutionConfig()}. It is typically used in
 * batch job configurations to customise execution parameters for different
 * scenarios or experiments.
 *
 * <p>
 * This class is used in the following contexts:
 * <ul>
 * <li>As part of batch job configurations to define execution facets.</li>
 * <li>In conjunction with {@link BatchConfiguration} to customise simulation
 * runs.</li>
 * </ul>
 *
 * @author Rob Challen
 * @see BatchConfiguration
 */
@Value.Immutable
@JsonSerialize(as = ImmutableExecutionFacet.class)
@JsonDeserialize(as = ImmutableExecutionFacet.class)
public interface ExecutionFacet extends Abstraction.Named {
	/**
	 * Gets the list of modifications to apply to the execution configuration.
	 * These modifications are used to customise the execution parameters for
	 * specific scenarios or experiments.
	 *
	 * @return A list of {@link PartialExecutionConfiguration} objects
	 *         representing the modifications. Defaults to an empty list if not
	 *         specified.
	 */
	@Value.Default
	default List<PartialExecutionConfiguration> getModifications() {
		return Collections.emptyList();
	}
}