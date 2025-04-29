package io.github.ai4ci.flow;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.builders.AbstractModelBuilder;
import io.github.ai4ci.abm.builders.BuilderFactory;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.util.Sampler;

public class ExecutionBuilder {

	static Logger log = LoggerFactory.getLogger(ExecutionBuilder.class);
	
	
//	public static Outbreak buildExperiment(
//			SetupConfiguration setupConfig,
//			ExecutionConfiguration execConfig,
//			String urnBase) {
//		return buildExperiment(
//			setupConfig,
//			execConfig,
////			ModelBuild.getSetupForConfiguration(setupConfig),
////			ModelBuild.OutbreakBaselinerFn.DEFAULT.fn(),
////			ModelBuild.PersonBaselinerFn.DEFAULT.fn(),
////			ModelBuild.OutbreakStateInitialiserFn.DEFAULT.fn(),
////			ModelBuild.PersonStateInitialiserFn.DEFAULT.fn(),
//			urnBase
//		);	
//	}
	
	public static Outbreak buildExperiment(
			SetupConfiguration setupConfig,
			ExecutionConfiguration execConfig,
//			ModelOperation.OutbreakSetup setupFn,
//			ModelOperation.OutbreakBaseliner outbreakBaselineFn,
//			ModelOperation.PersonBaseliner personBaselineFn,
//			ModelOperation.OutbreakInitialiser outbreakInitFn,
//			ModelOperation.PersonInitialiser personInitFn,
			String urnBase
	) {
		
		ExecutionBuilder experiment = new ExecutionBuilder(
				setupConfig
//			setupFn, outbreakBaselineFn, personBaselineFn, outbreakInitFn,
//			personInitFn
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
	
//	ExperimentBuilder() {
//		this(
//				ModelBuild.OutbreakSetupFn.DEFAULT.fn(),
//				ModelBuild.OutbreakBaselinerFn.DEFAULT.fn(),
//				ModelBuild.PersonBaselinerFn.DEFAULT.fn(),
//				ModelBuild.OutbreakStateInitialiserFn.DEFAULT.fn(),
//				ModelBuild.PersonStateInitialiserFn.DEFAULT.fn()
//		);
//	}
	
	ExecutionBuilder(
			SetupConfiguration setupConfig
//		ModelOperation.OutbreakSetup setupFn,
//		ModelOperation.OutbreakBaseliner outbreakBaselineFn,
//		ModelOperation.PersonBaseliner personBaselineFn,
//		ModelOperation.OutbreakInitialiser outbreakInitFn,
//		ModelOperation.PersonInitialiser personInitFn
	) {
//		this.setupFn = setupFn;
//		this.outbreakBaselineFn =  outbreakBaselineFn;
//		// TODO will this ever be part of the configuration?
//		// Some sort of ENUM in the config file in which case these
//		// would have to become optional...
//		this.personBaselineFn = personBaselineFn;
//		this.outbreakInitFn = outbreakInitFn;
//		this.personInitFn = personInitFn;
		this.setupConfig = setupConfig;
		this.modelBuilder = BuilderFactory.builderFrom(setupConfig);
		this.outbreak = ModifiableOutbreak.createOutbreakStub();
	}
	
	SetupConfiguration setupConfig;
	ModifiableOutbreak outbreak;
	AbstractModelBuilder modelBuilder;
//	ModelOperation.OutbreakSetup setupFn;
//	ModelOperation.PersonBaseliner personBaselineFn;
//	ModelOperation.OutbreakBaseliner outbreakBaselineFn;
//	ModelOperation.PersonInitialiser personInitFn;
//	ModelOperation.OutbreakInitialiser outbreakInitFn;
	
	public ExecutionBuilder copy() {
		ExecutionBuilder tmp = new ExecutionBuilder(
				this.setupConfig
//				this.setupFn,
//				this.outbreakBaselineFn,
//				this.personBaselineFn,
//				this.outbreakInitFn,
//				this.personInitFn
		);
		tmp.outbreak = SerializationUtils.clone(outbreak);
		return tmp;
	}
	
	void setupOutbreak(String urnBase) {
		outbreak.setUrn(
				(urnBase != null ? urnBase+":" : "")
				+setupConfig.getName()+":"+setupConfig.getReplicate());
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		modelBuilder.doSetupOutbreak(outbreak, setupConfig, sampler);
//		setupFn.getSelector().test(setupConfig);
//		setupFn.getConsumer().accept(outbreak, setupConfig, sampler);
	}
	
	void baselineModel(ExecutionConfiguration execConfig) {
		outbreak.setUrn(
			outbreak.getUrn()+":"+execConfig.getName()+":"+execConfig.getReplicate()	
		);
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		outbreak.setExecutionConfiguration(execConfig);
		
//		if (!outbreakBaselineFn.getSelector().test(outbreak))
//			throw new RuntimeException("Not correctly configured");
		
		outbreak.getPeople().parallelStream().forEach(
				p -> {
//					if (!personBaselineFn.getSelector().test(p)) throw new RuntimeException("Not correctly configured");
					Sampler sampler2 = Sampler.getSampler(outbreak.getUrn());
					if (p instanceof ModifiablePerson) {
						ModifiablePerson m = (ModifiablePerson) p;
						
						ImmutablePersonBaseline.Builder builder = 
								m.initialisedBaseline() ?
								ImmutablePersonBaseline.builder().from(m.getBaseline()) :
								ImmutablePersonBaseline.builder();
						
						// personBaselineFn.getConsumer().accept(builder, p, sampler2);
						modelBuilder.doBaselinePerson(builder, p, sampler2);
						m.setBaseline(builder.build());
				}});
		
		// Calibrate R0 to a baseline transmission probability
		ImmutableOutbreakBaseline.Builder builder = 
				outbreak.initialisedBaseline() ?
				ImmutableOutbreakBaseline.builder().from(outbreak.getBaseline()) :
				ImmutableOutbreakBaseline.builder();
		
		// outbreakBaselineFn.getConsumer().accept(builder, outbreak, sampler);
		modelBuilder.doBaselineOutbreak(builder, outbreak, sampler);
		outbreak.setBaseline(builder.build());
		
	}
	
	
	void initialiseStatus(ExecutionConfiguration execConfig) {
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		// if (!outbreakInitFn.getSelector().test(outbreak)) throw new RuntimeException("Not baselined");
		
		ImmutableOutbreakState.Builder builder = ImmutableOutbreakState.builder();
		if (outbreak.initialisedCurrentState()) builder.from(outbreak.getCurrentState());
			
		builder
			.setEntity(outbreak)
			.setTime(0);
		
		//outbreakInitFn.getConsumer().accept(builder, outbreak, sampler);
		modelBuilder.doInitialiseOutbreak(builder, outbreak, sampler);
		outbreak.setCurrentState(builder.build());
		
		outbreak.getPeople()
			.parallelStream()
			.forEach(p -> {
				if (p instanceof ModifiablePerson) {
					ModifiablePerson m = (ModifiablePerson) p;
					
					Sampler sampler2 = Sampler.getSampler(outbreak.getUrn());
					//if (!personInitFn.getSelector().test(m)) throw new RuntimeException("Person not baselined");
				
					ImmutablePersonState.Builder builder2 = ImmutablePersonState.builder();
					if (m.initialisedCurrentState()) builder2.from(m.getCurrentState());
						
					builder2
						.setEntity(p)
						.setTime(0);
				
					// personInitFn.getConsumer().accept(builder2, p, sampler2);
					modelBuilder.doInitialisePerson(builder2, p, sampler2);
					m.setCurrentState(builder2.build());
				
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
