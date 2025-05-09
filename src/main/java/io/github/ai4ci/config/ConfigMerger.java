package io.github.ai4ci.config;

import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.ImmutableDelayDistribution;

@Mapper(
		builder = @Builder(buildMethod = "build"),
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
		
)
public abstract class ConfigMerger {
	
	public static ConfigMerger INSTANCE = Mappers.getMapper( ConfigMerger.class );
	 
	public ImmutableExecutionConfiguration mergeConfiguration(
			ExecutionConfiguration target,
			Abstraction.Modification<ExecutionConfiguration> modification
	) {
		PartialExecutionConfiguration source = (PartialExecutionConfiguration) modification; 
		return updateConfiguration(
				ImmutableExecutionConfiguration.builder().from(target),
				source)
				.setAvailableTests(target.getAvailableTests().combine(source.getAvailableTests()))
				.build()
				;
	};
	
	
	public SetupConfiguration mergeConfiguration(
			SetupConfiguration config,
			Abstraction.Modification<? extends SetupConfiguration> modification
	) {
		if (config instanceof WattsStrogatzConfiguration)
			return mergeConfiguration((WattsStrogatzConfiguration) config, 
					(PartialWattsStrogatzConfiguration) modification);
		
		if (config instanceof AgeStratifiedNetworkConfiguration)
			return mergeConfiguration((AgeStratifiedNetworkConfiguration) config, 
					(PartialAgeStratifiedNetworkConfiguration) modification);
		
		else throw new RuntimeException("invalid combination.");
	}
	
	public ImmutableWattsStrogatzConfiguration mergeConfiguration(
			WattsStrogatzConfiguration target,
			PartialWattsStrogatzConfiguration source
	) {
		return updateConfiguration(
				ImmutableWattsStrogatzConfiguration.builder().from(target),
				source).build();
	};
	
	public ImmutableAgeStratifiedNetworkConfiguration mergeConfiguration(
			AgeStratifiedNetworkConfiguration target,
			PartialAgeStratifiedNetworkConfiguration source
	) {
		return updateConfiguration(
				ImmutableAgeStratifiedNetworkConfiguration.builder().from(target),
				source).build();
	};
	
	@Mapping(target = "availableTests", ignore = true)
	abstract protected ImmutableExecutionConfiguration.Builder updateConfiguration(
			@MappingTarget ImmutableExecutionConfiguration.Builder target,
			PartialExecutionConfiguration source); 
	
	abstract protected ImmutableWattsStrogatzConfiguration.Builder updateConfiguration(
			@MappingTarget ImmutableWattsStrogatzConfiguration.Builder target,
			PartialWattsStrogatzConfiguration source);
	
	abstract protected ImmutableAgeStratifiedNetworkConfiguration.Builder updateConfiguration(
			@MappingTarget ImmutableAgeStratifiedNetworkConfiguration.Builder target,
			PartialAgeStratifiedNetworkConfiguration source);
	
	abstract protected ImmutableStochasticModel mapper(StochasticModel source);
	abstract protected ImmutablePhenomenologicalModel mapper(PhenomenologicalModel source);
	
	protected InHostConfiguration mapper(InHostConfiguration source) {
		if (source instanceof PhenomenologicalModel) return mapper((PhenomenologicalModel) source);
		if (source instanceof StochasticModel) return mapper((StochasticModel) source);
		throw new RuntimeException("Unknown type: "+source.getClass());
	};
	
	protected SetupConfiguration mapper(SetupConfiguration source) {
		if (source instanceof WattsStrogatzConfiguration) return mapper((WattsStrogatzConfiguration) source);
		if (source instanceof AgeStratifiedNetworkConfiguration) return mapper((AgeStratifiedNetworkConfiguration) source);
		throw new RuntimeException("Unknown type: "+source.getClass());
	};
	
	abstract protected ImmutableDelayDistribution mapper(DelayDistribution source);
}
