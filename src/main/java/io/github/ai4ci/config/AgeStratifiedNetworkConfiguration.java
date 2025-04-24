package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution;

@Value.Immutable(copy=false)
//@JsonSerialize(as = ImmutableAgeStratifiedNetworkConfiguration.class)
//@JsonDeserialize(as = ImmutableAgeStratifiedNetworkConfiguration.class)
public interface AgeStratifiedNetworkConfiguration extends SetupConfiguration {

	@Partial @Value.Immutable
	@JsonSerialize(as = PartialAgeStratifiedNetworkConfiguration.class)
	@JsonDeserialize(as = PartialAgeStratifiedNetworkConfiguration.class)
	public interface _PartialAgeStratifiedNetworkConfiguration extends AgeStratifiedNetworkConfiguration, Abstraction.Modification {}
	
	public static AgeStratifiedNetworkConfiguration DEFAULT = 
			ImmutableAgeStratifiedNetworkConfiguration.builder()
				.setInitialImports(5)
				.setName("age-stratified")
				.setNetworkSize(10000)
				.setAgeDistribution(
						ImmutableEmpiricalDistribution.builder()
							.setMinimum(0)
							.setMaximum(120)
							.putCumulative(18, 0.1)
							.putCumulative(45, 0.5)
							.putCumulative(65, 0.75)
							.putCumulative(85, 0.9)	
							.build()
				)
				.build();
	
	EmpiricalDistribution getAgeDistribution();
	
}
