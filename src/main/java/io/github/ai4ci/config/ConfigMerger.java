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

@Mapper(
		builder = @Builder(buildMethod = "build"),
		nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
		collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
		nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
		nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
		
)
public abstract class ConfigMerger {
	 
	public static ConfigMerger INSTANCE = Mappers.getMapper( ConfigMerger.class );
	
	public ImmutableExecutionConfiguration.Builder mergeConfiguration(
			ExecutionConfiguration target,
			PartialExecutionConfiguration source
	) {
		return updateConfiguration(
				ImmutableExecutionConfiguration.builder().from(target),
				source)
				.setAvailableTests(target.getAvailableTests().combine(source.getAvailableTests()))
				;
	};
	
	public ImmutableSetupConfiguration.Builder mergeConfiguration(
			SetupConfiguration target,
			PartialSetupConfiguration source
	) {
		return updateConfiguration(
				ImmutableSetupConfiguration.builder().from(target),
				source);
	};
	
	@Mapping(target = "availableTests", ignore = true)
	abstract protected ImmutableExecutionConfiguration.Builder updateConfiguration(
			@MappingTarget ImmutableExecutionConfiguration.Builder target,
			PartialExecutionConfiguration source); 
	
	abstract protected ImmutableSetupConfiguration.Builder updateConfiguration(
			@MappingTarget ImmutableSetupConfiguration.Builder target,
			PartialSetupConfiguration source);
	
	
}
