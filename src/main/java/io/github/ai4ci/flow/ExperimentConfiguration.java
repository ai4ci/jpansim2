package io.github.ai4ci.flow;

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

import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.ConfigMerger;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ImmutableExecutionConfiguration;
import io.github.ai4ci.config.ImmutableWattsStrogatzConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialWattsStrogatzConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;


@Value.Immutable
@JsonSerialize(as = ImmutableBasicExperimentConfiguration.class)
@JsonDeserialize(as = ImmutableBasicExperimentConfiguration.class)
public interface ExperimentConfiguration<SETUP extends SetupConfiguration> {

	
	public interface BasicExperimentConfiguration extends ExperimentConfiguration<WattsStrogatzConfiguration> {
		
		BasicExperimentConfiguration DEFAULT = ImmutableBasicExperimentConfiguration.builder()
				.setSetupConfig(WattsStrogatzConfiguration.DEFAULT)
				.setExecutionConfig(ExecutionConfiguration.DEFAULT)
				.setExecutionReplications(1)
				.setSetupReplications(1)
				.build();
		
		default ImmutableBasicExperimentConfiguration adjustSetup(Consumer<ImmutableWattsStrogatzConfiguration.Builder> tweaks) {
			ImmutableWattsStrogatzConfiguration.Builder tmp = ImmutableWattsStrogatzConfiguration.builder().from(this.getSetupConfig());
			tweaks.accept(tmp);
			
			return ImmutableBasicExperimentConfiguration.builder()
				.from(this)
				.setSetupConfig(
					tmp.build()
				).build();
		}
		
		default ImmutableBasicExperimentConfiguration adjustExecution(Consumer<ImmutableExecutionConfiguration.Builder> tweaks) {
			ImmutableExecutionConfiguration.Builder tmp = ImmutableExecutionConfiguration.builder().from(this.getExecutionConfig());
			tweaks.accept(tmp);
			
			return ImmutableBasicExperimentConfiguration.builder()
				.from(this)
				.setExecutionConfig(
					tmp.build()
				).build();
		}
		
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableAgeStratifiedExperimentConfiguration.class)
	@JsonDeserialize(as = ImmutableAgeStratifiedExperimentConfiguration.class)
	public interface AgeStratifiedExperimentConfiguration extends ExperimentConfiguration<AgeStratifiedNetworkConfiguration> {
		
		AgeStratifiedExperimentConfiguration DEFAULT = ImmutableAgeStratifiedExperimentConfiguration.builder()
				.setSetupConfig(AgeStratifiedNetworkConfiguration.DEFAULT)
				.setExecutionConfig(ExecutionConfiguration.DEFAULT)
				.setExecutionReplications(1)
				.setSetupReplications(1)
				.build();
		
	}
	
	
	
	SETUP getSetupConfig();
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
				
				if (facet.getModifications().get(name) instanceof PartialWattsStrogatzConfiguration) {
					PartialWattsStrogatzConfiguration modifier = (PartialWattsStrogatzConfiguration) facet.getModifications().get(name);
					
					for (SetupConfiguration b: out) {
						ImmutableWattsStrogatzConfiguration.Builder modified = ConfigMerger.INSTANCE
							.mergeConfiguration(
								(WattsStrogatzConfiguration) b, modifier
							).setName(
								(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
									+facetName+":"+name);
						tmp.add(modified.build());
						changes = true;
					}
				}
			}
			
			if (changes) out = tmp;
			//}
		}
		
		List<SetupConfiguration> tmp = new ArrayList<>();
		for (int i =0; i<this.getSetupReplications(); i++) {
			for (SetupConfiguration b: out) {
				tmp.add(
					ImmutableWattsStrogatzConfiguration.builder().from(b)
						.setReplicate(i)
						.build()
					);
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
						ImmutableExecutionConfiguration.Builder modified = ConfigMerger.INSTANCE
							.mergeConfiguration(
								b, modifier
							).setName(
								(!b.getName().equals(base.getName()) ? b.getName()+":" : "")
									+facetName+":"+name);
						tmp.add(modified.build());
					});
				}
			}
			
			out = tmp;
			//}
		}
		
		List<ExecutionConfiguration> tmp = new ArrayList<>();
		for (int i =0; i<this.getExecutionReplications(); i++) {
			for (ExecutionConfiguration b: out) {
				tmp.add(
					ImmutableExecutionConfiguration.builder().from(b)
						.setReplicate(i)
						.build()
					);
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
	
	static <X extends ExperimentConfiguration<?>> X readFromYaml(Path file, Class<X> type) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.setSerializationInclusion(Include.NON_NULL);
		X rt = om.readerFor(type).readValue(file.toFile());
		return rt;
	}
	
}
