package io.github.ai4ci.config;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.util.Data.Partial;



@Value.Immutable
@JsonSerialize(as = ImmutableSetupConfiguration.class)
@JsonDeserialize(as = ImmutableSetupConfiguration.class)
public interface SetupConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable {

	@Partial @Value.Immutable
	@JsonSerialize(as = PartialSetupConfiguration.class)
	@JsonDeserialize(as = PartialSetupConfiguration.class)
	public interface _PartialSetupConfiguration extends SetupConfiguration, Abstraction.Modification {}
	
	SetupConfiguration DEFAULT = ImmutableSetupConfiguration.builder()
			.setName("setup")
			.setNetworkSize(10000)
			.setNetworkConnectedness(100)
			.setNetworkRandomness(0.15)
			.setInitialImports(5)
			.build();
	
	//
	
	Integer getNetworkSize();
	Integer getNetworkConnectedness();
	
	/**
	 * A measure of the randomness of the small world social network. This is 
	 * a range from 0 to 1 where 0 is ordered and 1 is totally random.
	 */
	Double getNetworkRandomness();
	
	
	Integer getInitialImports();
	
	 
	// Other parameters could reflect the set up of agents.
	// e.g. age distribution
	
	// initial mobility
	// initial 
}