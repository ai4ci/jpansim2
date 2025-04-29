package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.mechanics.Abstraction;

@Value.Immutable
@JsonSerialize(as = ImmutableWattsStrogatzConfiguration.class)
@JsonDeserialize(as = ImmutableWattsStrogatzConfiguration.class)
public interface WattsStrogatzConfiguration extends SetupConfiguration {

	public interface Builder extends SetupConfiguration.Builder {}
	
	WattsStrogatzConfiguration DEFAULT = ImmutableWattsStrogatzConfiguration.builder()
			.setName("setup")
			.setNetworkSize(10000)
			.setNetworkConnectedness(100)
			.setNetworkRandomness(0.15)
			.setInitialImports(5)
			.build();
	
	@Partial @Value.Immutable
	@JsonSerialize(as = PartialWattsStrogatzConfiguration.class)
	@JsonDeserialize(as = PartialWattsStrogatzConfiguration.class)
	public interface _PartialWattsStrogatzConfiguration extends WattsStrogatzConfiguration, Abstraction.Modification {}
	
	/**
	 * A measure of the randomness of the small world social network. This is 
	 * a range from 0 to 1 where 0 is ordered and 1 is totally random.
	 */
	Double getNetworkRandomness();
	Integer getNetworkConnectedness();
}
