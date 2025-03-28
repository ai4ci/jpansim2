package io.github.ai4ci.flow;

import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonBaseline;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.ModelBuild;
import io.github.ai4ci.abm.ModelOperation;
import io.github.ai4ci.abm.ModifiableOutbreak;
import io.github.ai4ci.abm.ModifiablePerson;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Updater;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.output.StateExporter;
import io.github.ai4ci.util.Sampler;
import io.reactivex.rxjava3.core.Flowable;

public class ExperimentBuilder {

	static Logger log = LoggerFactory.getLogger(ExperimentBuilder.class);
	
	public static void runExperiments(ExperimentConfiguration config, String urnBase, Updater updater, StateExporter exporter, int toStep, StateExporter finalState ) {
		
		Flowable
			.fromIterable(config.getSetup())
			.map(cfg -> {
				ExperimentBuilder builder = new ExperimentBuilder();
				builder.setupOutbreak(cfg, urnBase);
				return builder;
			})
			.flatMapStream(builder -> {
				return config.getExecution().stream().map(exCfg -> {
					ExperimentBuilder builder2 = builder.copy();
					builder2.baselineModel(exCfg);
					builder2.initialiseStatus(exCfg);
					return builder2.build();
				});	
			})
			.map( outbreak -> {
				IntStream.range(0, toStep).forEach(i -> {
					updater.update(outbreak);
					exporter.export(outbreak);
				});
				finalState.export(outbreak);
				return outbreak;
			})
			.blockingSubscribe();
			;
			
		
	}
	
	public static Outbreak buildExperiment(
			SetupConfiguration setupConfig,
			ExecutionConfiguration execConfig,
			String urnBase) {
		return buildExperiment(
			setupConfig,
			execConfig,
			ModelBuild.OutbreakSetupFn.DEFAULT.fn(),
			ModelBuild.OutbreakBaselinerFn.DEFAULT.fn(),
			ModelBuild.PersonBaselinerFn.DEFAULT.fn(),
			ModelBuild.OutbreakStateInitialiserFn.DEFAULT.fn(),
			ModelBuild.PersonStateInitialiserFn.DEFAULT.fn(),
			urnBase
		);	
	}
	
	public static Outbreak buildExperiment(
			SetupConfiguration setupConfig,
			ExecutionConfiguration execConfig,
			ModelOperation.OutbreakSetup setupFn,
			ModelOperation.OutbreakBaseliner outbreakBaselineFn,
			ModelOperation.PersonBaseliner personBaselineFn,
			ModelOperation.OutbreakInitialiser outbreakInitFn,
			ModelOperation.PersonInitialiser personInitFn,
			String urnBase
	) {
		
		ExperimentBuilder experiment = new ExperimentBuilder(
			setupFn, outbreakBaselineFn, personBaselineFn, outbreakInitFn,
			personInitFn
		);
		experiment.setupOutbreak(setupConfig, urnBase);
		experiment.baselineModel(execConfig);
		experiment.initialiseStatus(execConfig);
		return experiment.build();
	}
	
	ExperimentBuilder() {
		this(
				ModelBuild.OutbreakSetupFn.DEFAULT.fn(),
				ModelBuild.OutbreakBaselinerFn.DEFAULT.fn(),
				ModelBuild.PersonBaselinerFn.DEFAULT.fn(),
				ModelBuild.OutbreakStateInitialiserFn.DEFAULT.fn(),
				ModelBuild.PersonStateInitialiserFn.DEFAULT.fn()
		);
	}
	
	ExperimentBuilder(
		ModelOperation.OutbreakSetup setupFn,
		ModelOperation.OutbreakBaseliner outbreakBaselineFn,
		ModelOperation.PersonBaseliner personBaselineFn,
		ModelOperation.OutbreakInitialiser outbreakInitFn,
		ModelOperation.PersonInitialiser personInitFn
	) {
		this.setupFn = setupFn;
		this.outbreakBaselineFn =  outbreakBaselineFn;
		// TODO will this ever be part of the configuration?
		// Some sort of ENUM in the config file in which case these
		// would have to become optional...
		this.personBaselineFn = personBaselineFn;
		this.outbreakInitFn = outbreakInitFn;
		this.personInitFn = personInitFn;
		this.outbreak = ModifiableOutbreak.createOutbreakStub();
	}
	
	ModifiableOutbreak outbreak;
	ModelOperation.OutbreakSetup setupFn;
	ModelOperation.PersonBaseliner personBaselineFn;
	ModelOperation.OutbreakBaseliner outbreakBaselineFn;
	ModelOperation.PersonInitialiser personInitFn;
	ModelOperation.OutbreakInitialiser outbreakInitFn;
	
	public ExperimentBuilder copy() {
		ExperimentBuilder tmp = new ExperimentBuilder(
				this.setupFn,
				this.outbreakBaselineFn,
				this.personBaselineFn,
				this.outbreakInitFn,
				this.personInitFn
		);
		tmp.outbreak = SerializationUtils.clone(outbreak);
		return tmp;
	}
	
	private void setupOutbreak(SetupConfiguration setupConfig, String urnBase) {
		outbreak.setUrn(
				(urnBase != null ? urnBase+":" : "")
				+setupConfig.getName()+":"+setupConfig.getReplicate());
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		setupFn.getSelector().test(setupConfig);
		setupFn.getConsumer().accept(outbreak, setupConfig, sampler);
	}
	
	private void baselineModel(ExecutionConfiguration execConfig) {
		outbreak.setUrn(
			outbreak.getUrn()+":"+execConfig.getName()+":"+execConfig.getReplicate()	
		);
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		outbreak.setExecutionConfiguration(execConfig);
		
		if (!outbreakBaselineFn.getSelector().test(outbreak))
			throw new RuntimeException("Not correctly configured");
		
		outbreak.getPeople().parallelStream().forEach(
				p -> {
					if (!personBaselineFn.getSelector().test(p)) throw new RuntimeException("Not correctly configured");
					Sampler sampler2 = Sampler.getSampler(outbreak.getUrn());
					if (p instanceof ModifiablePerson m) {
						
						ImmutablePersonBaseline.Builder builder = 
								m.initialisedBaseline() ?
								ImmutablePersonBaseline.builder().from(m.getBaseline()) :
								ImmutablePersonBaseline.builder();
						
						personBaselineFn.getConsumer().accept(builder, p, sampler2);
						m.setBaseline(builder.build());
				}});
		
		// Calibrate R0 to a baseline transmission probability
		ImmutableOutbreakBaseline.Builder builder = 
				outbreak.initialisedBaseline() ?
				ImmutableOutbreakBaseline.builder().from(outbreak.getBaseline()) :
				ImmutableOutbreakBaseline.builder();
		
		outbreakBaselineFn.getConsumer().accept(builder, outbreak, sampler);
		outbreak.setBaseline(builder.build());
		
	}
	
	
	private void initialiseStatus(ExecutionConfiguration execConfig) {
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		if (!outbreakInitFn.getSelector().test(outbreak)) throw new RuntimeException("Not baselined");
		
		ImmutableOutbreakState.Builder builder = ImmutableOutbreakState.builder();
		if (outbreak.initialisedCurrentState()) builder.from(outbreak.getCurrentState());
			
		builder
			.setEntity(outbreak)
			.setTime(0);
		
		outbreakInitFn.getConsumer().accept(builder, outbreak, sampler);
		outbreak.setCurrentState(builder.build());
		
		outbreak.getPeople()
			.parallelStream()
			.forEach(p -> {
				if (p instanceof ModifiablePerson m) {
					Sampler sampler2 = Sampler.getSampler(outbreak.getUrn());
					if (!personInitFn.getSelector().test(m)) throw new RuntimeException("Person not baselined");
				
					ImmutablePersonState.Builder builder2 = ImmutablePersonState.builder();
					if (m.initialisedCurrentState()) builder2.from(m.getCurrentState());
						
					builder2
						.setEntity(p)
						.setTime(0);
				
					personInitFn.getConsumer().accept(builder2, p, sampler2);
					m.setCurrentState(builder2.build());
				
				} else {
					throw new RuntimeException("Not modifiable person");
				}
				
			});
		
	}

	public Outbreak build() {
		if (!outbreak.isInitialized()) 
			throw new RuntimeException("Not initialised");
		return outbreak;
	}
	
}
