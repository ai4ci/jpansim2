package io.github.ai4ci.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.config.setup.ImmutableSetupConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;

/**
 * A facet of the simulation configuration that defines the default setup and
 * any modifications to it. The default setup provides the base configuration
 * for the simulation, while modifications allow for overriding specific
 * parameters or adding additional configurations on top of the default. This
 * interface is designed to be immutable and can be easily
 * serialized/deserialized using JSON.
 *
 * <p>
 * Facets will be processed by the {@link ExperimentConfiguration} to produce a
 * final {@link SetupConfiguration} that combines the default configuration with
 * any specified modifications. This allows for flexible and modular
 * configuration of the simulation setup, enabling users to easily customize
 * their simulation runs by specifying different facets with varying defaults
 * and modifications.
 *
 * <p>
 * The resulting SetupConfiguration will initialise a single simulation which
 * will be potentially executed multiple times with different random seeds and
 * with different ExecutionConfigurations. This allows for efficient reuse of
 * the same setup across multiple runs, while still enabling variability through
 * the use of modifications and random seeds.
 *
 */
@Value.Immutable
@JsonSerialize(as = ImmutableSetupFacet.class)
@JsonDeserialize(as = ImmutableSetupFacet.class)
public interface SetupFacet {

	/**
	 * Creates an {@code ImmutableSetupFacet} with the given default
	 * configuration and modifications. The default configuration serves as the
	 * base setup, while the modifications allow for overriding specific
	 * parameters or adding additional configurations on top of the default.
	 *
	 * @param config        the default setup configuration to use as the base
	 *                      setup
	 * @param modifications optional modifications to apply on top of the default
	 *                      configuration. These can override specific parameters
	 *                      or add new configurations.
	 * @return an {@code ImmutableSetupFacet} instance containing the default
	 *         configuration and any specified modifications
	 */
	public static ImmutableSetupFacet of(
			ImmutableSetupConfiguration config,
			PartialSetupConfiguration... modifications
	) {
		return ImmutableSetupFacet.builder().setDefault(config)
				.addModifications(modifications).build();
	}

	/**
	 * Returns the default setup configuration for this facet. This configuration
	 * serves as the base setup for the simulation, providing the initial
	 * parameters and settings that will be used unless overridden by
	 * modifications.
	 *
	 * @return the default {@link SetupConfiguration} for this facet
	 */
	SetupConfiguration getDefault();

	/**
	 * Returns a list of modifications to apply on top of the default setup
	 * configuration. These modifications can override specific parameters or add
	 * new configurations to customize the simulation setup. If no modifications
	 * are specified, this method returns an empty list.
	 *
	 * @return a list of {@link PartialSetupConfiguration} instances representing
	 *         modifications to apply on top of the default configuration, or an
	 *         empty list if no modifications are specified
	 */
	@Value.Default
	default List<PartialSetupConfiguration> getModifications() {
		return Collections.emptyList();
	}
}