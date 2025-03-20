package io.github.ai4ci.config;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.util.Data.Partial;



@Value.Immutable
public interface SetupConfiguration extends Abstraction.Named, Serializable {

	@Partial @Value.Modifiable 
	public interface _PartialSetupConfiguration extends SetupConfiguration {}
	
	SetupConfiguration DEFAULT = ImmutableSetupConfiguration.builder()
			.setName("default-setup")
			.setNetworkSize(10000)
			.setNetworkConnectedness(40)
			.setNetworkRandomness(0.25)
			.setInitialImports(5)
			.build();
	
	//
	Integer getNetworkSize();
	Integer getNetworkConnectedness();
	Double getNetworkRandomness();
	Integer getInitialImports();
	
	 
	// Other parameters could reflect the set up of agents.
	// e.g. age distribution
	
	// initial mobility
	// initial 
}