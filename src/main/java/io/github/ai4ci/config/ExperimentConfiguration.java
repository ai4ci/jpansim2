package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import io.github.ai4ci.config.ExperimentFacet.ExecutionFacet;
import io.github.ai4ci.config.ExperimentFacet.SetupFacet;


@Value.Immutable
@JsonSerialize(as = ImmutableExperimentConfiguration.class)
@JsonDeserialize(as = ImmutableExperimentConfiguration.class)
public interface ExperimentConfiguration {
 
	
	ImmutableExperimentConfiguration DEFAULT = ImmutableExperimentConfiguration.builder()
				.setSetupConfig(
					List.of(
						ImmutableWattsStrogatzFacet.builder().setDefault(WattsStrogatzConfiguration.DEFAULT).build(),
						ImmutableAgeStratifiedNetworkFacet.builder().setDefault(AgeStratifiedNetworkConfiguration.DEFAULT).build()
					)
				)
				.setExecutionConfig(
						ImmutableExecutionFacet.builder().setDefault(ExecutionConfiguration.DEFAULT).build()
				)
				.setExecutionReplications(1)
				.setSetupReplications(1)
				.build();
		
//	default ImmutableExperimentConfiguration adjustSetup(Consumer<ImmutableWattsStrogatzConfiguration.Builder> tweaks) {
//		ImmutableWattsStrogatzConfiguration.Builder tmp = ImmutableWattsStrogatzConfiguration.builder().from(this.getSetupConfig());
//		tweaks.accept(tmp);
//		
//		return ImmutableExperimentConfiguration.builder()
//			.from(this)
//			.setSetupConfig(
//				tmp.build()
//			).build();
//	}
//		
//	default ImmutableExperimentConfiguration adjustExecution(Consumer<ImmutableExecutionConfiguration.Builder> tweaks) {
//		ImmutableExecutionConfiguration.Builder tmp = ImmutableExecutionConfiguration.builder().from(this.getExecutionConfig());
//		tweaks.accept(tmp);
//		
//		return ImmutableExperimentConfiguration.builder()
//			.from(this)
//			.setExecutionConfig(
//				tmp.build()
//			).build();
//	}
		
//	ExperimentConfiguration AGE_STRATIFIED = ImmutableExperimentConfiguration.builder()
//				.setSetupConfig(AgeStratifiedNetworkConfiguration.DEFAULT)
//				.setExecutionConfig(ExecutionConfiguration.DEFAULT)
//				.setExecutionReplications(1)
//				.setSetupReplications(1)
//				.build();
		
	
	
	
	
	List<SetupFacet<?>> getSetupConfig();
	ExecutionFacet getExecutionConfig();
	int getSetupReplications();
	int getExecutionReplications();
	
	
	
	
	
	@JsonIgnore
	default List<SetupConfiguration> getSetup() {
		
		List<SetupConfiguration> tmp = new ArrayList<>();
		List<SetupFacet<?>> setupCfg = getSetupConfig();
		
		for (SetupFacet<?> facet: setupCfg) {
			
			SetupConfiguration base = facet.getDefault();
			for (String name: facet.getModifications().keySet()) {
				
				SetupConfiguration modified = (SetupConfiguration) ConfigMerger.INSTANCE
					.mergeConfiguration(
						base, facet.getModifications().get(name)
					)
					.withName(
						facet.getName()+":"+name
					);
						
				tmp.add(modified);
			}
				
			if (tmp.isEmpty()) tmp.add(base);
		}
		
		List<SetupConfiguration> tmp2 = new ArrayList<>();
		for (int i =0; i<this.getSetupReplications(); i++) {
			for (SetupConfiguration b: tmp) {
				tmp2.add((SetupConfiguration) b.withReplicate(i));
			}
		}
		
		return tmp2;
		
	}
	
	@JsonIgnore
	default List<ExecutionConfiguration> getExecution() {
		
		ExecutionFacet facet = getExecutionConfig();
		ExecutionConfiguration base = facet.getDefault();
		
		List<ExecutionConfiguration> tmp = new ArrayList<>();
			
		for (String name: facet.getModifications().keySet()) {
				
			ImmutableExecutionConfiguration modified = ConfigMerger.INSTANCE
				.mergeConfiguration(
						base, facet.getModifications().get(name)
				)
				.withName(
					facet.getName()+":"+name
				);
			
			tmp.add(modified);
		}
		
		if (tmp.isEmpty()) tmp.add(base);
		
		List<ExecutionConfiguration> tmp2 = new ArrayList<>();
		for (int i =0; i<this.getExecutionReplications(); i++) {
			for (ExecutionConfiguration b: tmp) {
				tmp2.add((ExecutionConfiguration) b.withReplicate(i));
			}
		}
		
		return tmp2;
		
	}
	
	default void writeConfig(Path directory) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		Path file = directory.resolve("config.json");
		om.writeValue(file.toFile(), this);
	}
	
	static ExperimentConfiguration readConfig(Path file) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		ExperimentConfiguration rt = om.readerFor(ExperimentConfiguration.class).readValue(file.toFile());
		return rt;
	}
	
	default ImmutableExperimentConfiguration withSetupConfig(SetupConfiguration config) {
		return ImmutableExperimentConfiguration.builder().from(this)
			.setSetupConfig(List.of(SetupFacet.subtype(config)))
			.build();
	}
	
	default ImmutableExperimentConfiguration withExecutionConfig(ExecutionConfiguration config) {
		return ImmutableExperimentConfiguration.builder().from(this)
				.setExecutionConfig(
					ImmutableExecutionFacet.builder().setDefault(config).build()
				)
				.build();
	}
	
	default ImmutableExperimentConfiguration withFacet(String name, PartialExecutionConfiguration config) {
		return ImmutableExperimentConfiguration.builder().from(this)
				.setExecutionConfig(
					ImmutableExecutionFacet.builder()
						.from(getExecutionConfig())
						.putModification(name, config)
						.build()
				)
				.build();
	}
	
}
