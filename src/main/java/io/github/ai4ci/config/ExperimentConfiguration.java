package io.github.ai4ci.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


@Value.Immutable
@JsonSerialize(as = ImmutableExperimentConfiguration.class)
@JsonDeserialize(as = ImmutableExperimentConfiguration.class)
public interface ExperimentConfiguration {
 
	
	ExperimentConfiguration DEFAULT = ImmutableExperimentConfiguration.builder()
				.setSetupConfig(WattsStrogatzConfiguration.DEFAULT)
				.setExecutionConfig(ExecutionConfiguration.DEFAULT)
				.setExecutionReplications(1)
				.setSetupReplications(1)
				.build();
		
	default ImmutableExperimentConfiguration adjustSetup(Consumer<ImmutableWattsStrogatzConfiguration.Builder> tweaks) {
		ImmutableWattsStrogatzConfiguration.Builder tmp = ImmutableWattsStrogatzConfiguration.builder().from(this.getSetupConfig());
		tweaks.accept(tmp);
		
		return ImmutableExperimentConfiguration.builder()
			.from(this)
			.setSetupConfig(
				tmp.build()
			).build();
	}
		
	default ImmutableExperimentConfiguration adjustExecution(Consumer<ImmutableExecutionConfiguration.Builder> tweaks) {
		ImmutableExecutionConfiguration.Builder tmp = ImmutableExecutionConfiguration.builder().from(this.getExecutionConfig());
		tweaks.accept(tmp);
		
		return ImmutableExperimentConfiguration.builder()
			.from(this)
			.setExecutionConfig(
				tmp.build()
			).build();
	}
		
	ExperimentConfiguration AGE_STRATIFIED = ImmutableExperimentConfiguration.builder()
				.setSetupConfig(AgeStratifiedNetworkConfiguration.DEFAULT)
				.setExecutionConfig(ExecutionConfiguration.DEFAULT)
				.setExecutionReplications(1)
				.setSetupReplications(1)
				.build();
		
	
	
	
	
	SetupConfiguration getSetupConfig();
	ExecutionConfiguration getExecutionConfig();
	List<ExperimentFacet> getFacets();
	int getSetupReplications();
	int getExecutionReplications();
	
	
	
	
	
	@JsonIgnore
	default List<SetupConfiguration> getSetup() {
		
		SetupConfiguration base = getSetupConfig();
		List<SetupConfiguration> out = new ArrayList<>();
		out.add(base);
		for (ExperimentFacet facet: getFacets()) {
			//if (facet instanceof ExperimentFacet.SetupModification s) {
				
			String facetName = facet.getName();
			List<SetupConfiguration> tmp = new ArrayList<>();
			
			boolean changes = false;
			for (String name: facet.getModifications().keySet()) {
				
				if (facet.getModifications().get(name) instanceof SetupConfiguration) {
//					PartialWattsStrogatzConfiguration modifier = (PartialWattsStrogatzConfiguration) facet.getModifications().get(name);
					
					for (SetupConfiguration b: out) {
						
						
						
						SetupConfiguration modified = (SetupConfiguration) ConfigMerger.INSTANCE
								.mergeConfiguration(
										b, facet.getModifications().get(name)
								)
								.withName(
									(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
										+facetName+":"+name);
						
						tmp.add(modified);
						
//						if (b instanceof ImmutableWattsStrogatzConfiguration) {
//							ImmutableWattsStrogatzConfiguration.Builder modified = ConfigMerger.INSTANCE
//								.mergeConfiguration(
//									(WattsStrogatzConfiguration) b, modifier
//								).setName(
//									(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
//										+facetName+":"+name);
//							tmp.add(modified.build());
//						}
						
						changes = true;
					}
				}
				
//				if (facet.getModifications().get(name) instanceof PartialAgeStratifiedNetworkConfiguration) {
//					
//					PartialAgeStratifiedNetworkConfiguration modifier = (PartialAgeStratifiedNetworkConfiguration) facet.getModifications().get(name);
//					
//					for (SetupConfiguration b: out) {
//						if (b instanceof ImmutableAgeStratifiedNetworkConfiguration) {
//							ImmutableAgeStratifiedNetworkConfiguration.Builder modified = ConfigMerger.INSTANCE
//								.mergeConfiguration(
//									(AgeStratifiedNetworkConfiguration) b, modifier
//								).setName(
//									(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
//										+facetName+":"+name);
//							tmp.add(modified.build());
//						}
//						
//						changes = true;
//					}
//					
//				}
//			}
			
			if (changes) out = tmp;
			}
		}
		
		List<SetupConfiguration> tmp = new ArrayList<>();
		for (int i =0; i<this.getSetupReplications(); i++) {
			for (SetupConfiguration b: out) {
				tmp.add((SetupConfiguration) b.withReplicate(i));
//				if (b instanceof ImmutableWattsStrogatzConfiguration) {
//					tmp.add(
//						((ImmutableWattsStrogatzConfiguration) b).withReplicate(i));
//				}
//				
//				if (b instanceof ImmutableAgeStratifiedNetworkConfiguration) {
//					tmp.add(
//						((ImmutableAgeStratifiedNetworkConfiguration) b).withReplicate(i));
//				}
			}
		}
		
		return tmp;
		
	}
	
	@JsonIgnore
	default List<ExecutionConfiguration> getExecution() {
		
		ExecutionConfiguration base = getExecutionConfig();
		List<ExecutionConfiguration> out = new ArrayList<>();
		out.add(base);
		for (ExperimentFacet facet: getFacets()) {
			//if (facet instanceof ExperimentFacet.SetupModification s) {
				
			String facetName = facet.getName();
			List<ExecutionConfiguration> tmp = new ArrayList<>();
			
			for (String name: facet.getModifications().keySet()) {
				
				if (facet.getModifications().get(name) instanceof PartialExecutionConfiguration) {
					PartialExecutionConfiguration modifier = (PartialExecutionConfiguration) facet.getModifications().get(name);
					
					out.forEach(b -> {
						ImmutableExecutionConfiguration modified = ConfigMerger.INSTANCE
							.mergeConfiguration(
								b, modifier
							)
							.withName(
								(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
									+facetName+":"+name);
						tmp.add(modified);
					});
				}
			}
			
			out = tmp;
			//}
		}
		
		List<ExecutionConfiguration> tmp = new ArrayList<>();
		for (int i =0; i<this.getExecutionReplications(); i++) {
			for (ExecutionConfiguration b: out) {
				tmp.add((ExecutionConfiguration) b.withReplicate(i));
			}
		}
		
		return tmp;
		
	}
	
	default void writeToYaml(Path directory) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.setSerializationInclusion(Include.NON_NULL);
		Path file = directory.resolve("config.yml");
		om.writeValue(file.toFile(), this);
	}
	
	static ExperimentConfiguration readFromYaml(Path file) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.setSerializationInclusion(Include.NON_NULL);
		ExperimentConfiguration rt = om.readerFor(ExperimentConfiguration.class).readValue(file.toFile());
		return rt;
	}
	
}
