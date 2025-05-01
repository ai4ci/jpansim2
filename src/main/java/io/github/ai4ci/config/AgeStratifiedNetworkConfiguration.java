package io.github.ai4ci.config;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.util.EmpiricalDistribution;
import io.github.ai4ci.util.EmpiricalFunction;
import io.github.ai4ci.util.EmpiricalFunction.Link;
import io.github.ai4ci.util.ImmutableEmpiricalDistribution;
import io.github.ai4ci.util.ImmutableEmpiricalFunction;

@Value.Immutable
@JsonSerialize(as = ImmutableAgeStratifiedNetworkConfiguration.class)
@JsonDeserialize(as = ImmutableAgeStratifiedNetworkConfiguration.class)
public interface AgeStratifiedNetworkConfiguration extends SetupConfiguration {
 
	@Partial @Value.Immutable
	@JsonSerialize(as = PartialAgeStratifiedNetworkConfiguration.class)
	@JsonDeserialize(as = PartialAgeStratifiedNetworkConfiguration.class)
	public interface _PartialAgeStratifiedNetworkConfiguration extends AgeStratifiedNetworkConfiguration, Abstraction.Modification<AgeStratifiedNetworkConfiguration> {}
	
	public interface Builder extends SetupConfiguration.Builder {}
	
	public static ImmutableAgeStratifiedNetworkConfiguration DEFAULT = 
			ImmutableAgeStratifiedNetworkConfiguration.builder()
				.setInitialImports(5)
				.setName("age-stratified")
				.setNetworkSize(10000)
				.setNetworkRandomness(0.1)
				.setNetworkConnectedness(100)
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
				.setOddsContactFromAgeDifference(
						ImmutableEmpiricalFunction.builder()
						.putDataPoint(0, 2)
						.putDataPoint(10, 0.5)
						.putDataPoint(25, 1.5)
						.putDataPoint(40, 0.5)
						.putDataPoint(60, 1.0)
						.putDataPoint(70, 0.5)
						.setLink(Link.LOG)
						.build()
				)
				.setOddsMobilityFromAge(
						ImmutableEmpiricalFunction.builder()
						.putDataPoint(0, 2)
						.putDataPoint(5, 1)
						.putDataPoint(15, 1.5)
						.putDataPoint(25, 1.25)
						.putDataPoint(45, 0.8)
						.putDataPoint(75, 1.2)
						.setLink(Link.LOG)
						.build()
				)
				.build();
	
	double getNetworkRandomness();
	int getNetworkConnectedness();
	EmpiricalDistribution getAgeDistribution();
	EmpiricalFunction getOddsContactFromAgeDifference();
	EmpiricalFunction getOddsMobilityFromAge();
}
