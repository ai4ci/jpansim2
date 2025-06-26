package io.github.ai4ci.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.builders.DefaultModelBuilder;
import io.github.ai4ci.abm.mechanics.AbstractModelBuilder;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Cloner;
import io.github.ai4ci.util.Sampler;

public class ExecutionBuilder {

	static Logger log = LoggerFactory.getLogger(ExecutionBuilder.class);
	
	public static Outbreak buildExperiment(
			SetupConfiguration setupConfig,
			ExecutionConfiguration execConfig,
			String urnBase
	) {
		
		ExecutionBuilder experiment = new ExecutionBuilder(
				setupConfig
		);
		experiment.setupOutbreak(urnBase);
		experiment.baselineModel(execConfig);
		experiment.initialiseStatus(execConfig);
		return experiment.build();
	}
	
	public static double freeMem() {
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
		// allocatedMemory = allocatedMemory > maxMem ? maxMem : allocatedMemory;  
		return ((double) runtime.maxMemory() - allocatedMemory)/(1024*1024*1024);
	}
	
	ExecutionBuilder(
			SetupConfiguration setupConfig
	) {
		this.setupConfig = setupConfig;
		this.modelBuilder = new DefaultModelBuilder();
		this.outbreak = Outbreak.createOutbreakStub();
	}
	
	SetupConfiguration setupConfig;
	ModifiableOutbreak outbreak;
	AbstractModelBuilder modelBuilder;
	
	public ExecutionBuilder copy(long estSize) {
		ExecutionBuilder tmp = new ExecutionBuilder(
				this.setupConfig
				);
		if (estSize < 0) {
			tmp.outbreak = Cloner.copy(outbreak);
		} else {
			tmp.outbreak = Cloner.copy(outbreak, estSize);
		}
		return tmp;
	}
	
	void setupOutbreak(String urnBase) {
		outbreak.setUrn(
				(urnBase != null ? urnBase+":" : "")
				+setupConfig.getName()+":"+setupConfig.getReplicate());
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		modelBuilder.doSetupOutbreak(outbreak, setupConfig, sampler);
	}
	
	void baselineModel(ExecutionConfiguration execConfig) {
		outbreak.setUrn(
			outbreak.getUrn()+":"+execConfig.getName()+":"+execConfig.getReplicate()	
		);
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		outbreak.setExecutionConfiguration(execConfig);
		
		outbreak.getPeople().parallelStream().forEach(
				p -> {
					Sampler sampler2 = Sampler.getSampler(outbreak.getUrn());
					if (p instanceof ModifiablePerson) {
						ModifiablePerson m = (ModifiablePerson) p;
						
						ImmutablePersonBaseline.Builder builder = 
								m.initialisedBaseline() ?
								ImmutablePersonBaseline.builder().from(m.getBaseline()) :
								ImmutablePersonBaseline.builder();
						
						modelBuilder.doBaselinePerson(builder, p, sampler2);
						m.setBaseline(builder.build());
				}});
		
		// Calibrate R0 to a baseline transmission probability
		ImmutableOutbreakBaseline.Builder builder = 
				outbreak.initialisedBaseline() ?
				ImmutableOutbreakBaseline.builder().from(outbreak.getBaseline()) :
				ImmutableOutbreakBaseline.builder();
		
		modelBuilder.doBaselineOutbreak(builder, outbreak, sampler);
		outbreak.setBaseline(builder.build());
		
	}
	
	
	void initialiseStatus(ExecutionConfiguration execConfig) {
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		
		ImmutableOutbreakState.Builder builder = ImmutableOutbreakState.builder();
		if (outbreak.initialisedCurrentState()) builder.from(outbreak.getCurrentState());
			
		builder
			.setEntity(outbreak)
			.setTime(0);
		
		modelBuilder.doInitialiseOutbreak(builder, outbreak, sampler);
		outbreak.setCurrentState(builder.build());
		
		outbreak.getPeople()
			.parallelStream()
			.forEach(p -> {
				if (p instanceof ModifiablePerson) {
					ModifiablePerson m = (ModifiablePerson) p;
					
					Sampler sampler2 = Sampler.getSampler(outbreak.getUrn());
				
					ImmutablePersonState.Builder builder2 = ImmutablePersonState.builder();
					if (m.initialisedCurrentState()) builder2.from(m.getCurrentState());
						
					builder2
						.setEntity(p)
						.setTime(0);
				
					
					m.setCurrentState(
							modelBuilder.doInitialisePerson(builder2, p, sampler2)
					);
				
				} else {
					throw new RuntimeException("Not modifiable person");
				}
				
			});
		
	}

	public Outbreak build() {
		if (!outbreak.isInitialized()) 
			throw new RuntimeException("Not initialised");
		Outbreak tmp = outbreak;
		this.outbreak = null;
		return tmp;
	}
	
}
