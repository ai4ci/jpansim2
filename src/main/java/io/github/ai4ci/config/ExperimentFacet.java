package io.github.ai4ci.config;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Modification;

public interface ExperimentFacet<X extends Abstraction.Named> extends Abstraction.Named {
	
	@Value.Default default List<Modification<X>> getModifications() { 
		return Collections.emptyList(); 
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableExecutionFacet.class)
	@JsonDeserialize(as = ImmutableExecutionFacet.class)
	public static interface ExecutionFacet extends ExperimentFacet<ExecutionConfiguration> {}
	
	@JsonTypeInfo(use = Id.SIMPLE_NAME)
	@JsonSubTypes( {
		@Type(value = ImmutableWattsStrogatzFacet.class, name = "watts-strogatz"), 
		@Type(value = ImmutableAgeStratifiedNetworkFacet.class, name = "age-stratified") 
	} )
	public static interface SetupFacet<X extends SetupConfiguration> extends ExperimentFacet<X> {
		X getDefault();
		@Value.Default default String getName() {return getDefault().getName();}

		@SuppressWarnings("unchecked")
		static <X extends SetupConfiguration> SetupFacet<X> subtype(X config) {
			if (config instanceof ImmutableWattsStrogatzConfiguration) {
				return (SetupFacet<X>) ImmutableWattsStrogatzFacet.builder().setDefault((ImmutableWattsStrogatzConfiguration) config).build();
			} else if (config instanceof ImmutableAgeStratifiedNetworkConfiguration) {
				return (SetupFacet<X>) ImmutableAgeStratifiedNetworkFacet.builder().setDefault((ImmutableAgeStratifiedNetworkConfiguration) config).build();
			} else {
				throw new RuntimeException("Unknown type");
			}
		}}
	

	@Value.Immutable
	@JsonSerialize(as = ImmutableWattsStrogatzFacet.class)
	@JsonDeserialize(as = ImmutableWattsStrogatzFacet.class)
	public static interface WattsStrogatzFacet extends SetupFacet<ImmutableWattsStrogatzConfiguration> {}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableAgeStratifiedNetworkFacet.class)
	@JsonDeserialize(as = ImmutableAgeStratifiedNetworkFacet.class)
	public static interface AgeStratifiedNetworkFacet extends SetupFacet<ImmutableAgeStratifiedNetworkConfiguration> {}
	
}
