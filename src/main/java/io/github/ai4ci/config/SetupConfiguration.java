package io.github.ai4ci.config;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.OptBoolean;

import io.github.ai4ci.abm.mechanics.Abstraction;



// @Value.Immutable
// @JsonSerialize(as = ImmutableSetupConfiguration.class)
// @JsonDeserialize(as = ImmutableSetupConfiguration.class)
// @JsonTypeInfo(use = Id.SIMPLE_NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes( {@Type(ImmutableWattsStrogatzConfiguration.class), @Type(ImmutableAgeStratifiedNetworkConfiguration.class) } )
public interface SetupConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable {

	// public static Logger log = LoggerFactory.getLogger(SetupConfiguration.class);
	
	// @Partial @Value.Immutable
	// @JsonSerialize(as = PartialSetupConfiguration.class)
	// @JsonDeserialize(as = PartialSetupConfiguration.class)
	// public interface _PartialSetupConfiguration extends SetupConfiguration, Abstraction.Modification {}
	
	Integer getNetworkSize();
	Integer getInitialImports();
	
	
	// Other parameters could reflect the set up of agents.
	// e.g. age distribution
	// initial mobility
	// initial
	
	
}