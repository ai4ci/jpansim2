package io.github.ai4ci.flow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.SystemUtils;
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

import io.github.ai4ci.abm.BehaviourModel.SmartAgentTesting;
import io.github.ai4ci.abm.PolicyModel.NoControl;
import io.github.ai4ci.config.ConfigMerger;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ImmutableExecutionConfiguration;
import io.github.ai4ci.config.ImmutableSetupConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.config.SetupConfiguration;

@Value.Immutable
public interface ExperimentConfiguration {

	ExperimentConfiguration DEFAULT = ImmutableExperimentConfiguration.builder()
			.setSetupConfig(SetupConfiguration.DEFAULT)
			.setExecutionConfig(ExecutionConfiguration.DEFAULT)
			.build();
	
	SetupConfiguration getSetupConfig();
	ExecutionConfiguration getExecutionConfig();
	List<ExperimentFacet> getFacets();
	@Value.Default default int getSetupReplications() {return 1;}
	@Value.Default default int getExecutionReplications() {return 1;}
	
	
	default ImmutableExperimentConfiguration adjustSetup(Consumer<ImmutableSetupConfiguration.Builder> tweaks) {
		ImmutableSetupConfiguration.Builder tmp = ImmutableSetupConfiguration.builder().from(this.getSetupConfig());
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
				
				if (facet.getModifications().get(name) instanceof PartialSetupConfiguration modifier) {
					
					for (SetupConfiguration b: out) {
						ImmutableSetupConfiguration.Builder modified = ConfigMerger.INSTANCE
							.mergeConfiguration(
								b, modifier
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
					ImmutableSetupConfiguration.builder().from(b)
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
				
				if (facet.getModifications().get(name) instanceof PartialExecutionConfiguration modifier) {
					
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
	
	static ExperimentConfiguration readFromYaml(Path file) throws StreamWriteException, DatabindException, IOException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.setSerializationInclusion(Include.NON_NULL);
		ExperimentConfiguration rt = om.readerFor(ExperimentConfiguration.class).readValue(file.toFile());
		return rt;
	}
	
}
