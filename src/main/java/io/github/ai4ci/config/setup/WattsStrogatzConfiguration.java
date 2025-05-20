package io.github.ai4ci.config.setup;

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
	
	ImmutableWattsStrogatzConfiguration DEFAULT = ImmutableWattsStrogatzConfiguration.builder()
			.setName("watts-strogatz")
			.setNetworkSize(128*128)
			.setNetworkConnectedness(100)
			.setNetworkRandomness(0.15)
			.setInitialImports(30)
			.build();
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialWattsStrogatzConfiguration.class)
	@JsonDeserialize(as = PartialWattsStrogatzConfiguration.class)
	public interface _PartialWattsStrogatzConfiguration extends WattsStrogatzConfiguration, Abstraction.Modification<WattsStrogatzConfiguration> {
		default _PartialWattsStrogatzConfiguration self() {return this;}
	}
	
	/**
	 * A measure of the randomness of the small world social network. This is 
	 * a range from 0 to 1 where 0 is ordered and 1 is totally random.
	 */
	Double getNetworkRandomness();
	Integer getNetworkConnectedness();
}
