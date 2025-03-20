package io.github.ai4ci.flow;

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
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.util.Sampler;

public class ExperimentBuilder {

	static Logger log = LoggerFactory.getLogger(ExperimentBuilder.class);
	
	public static void runExperiments(ExperimentConfiguration config) {
		
//		Flowable
//			.just(config)
//			.flatMap(cfg -> {
//				String urn = "";
//				
//			});
		
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
		experiment.setupOutbreak(setupConfig, urnBase, 0);
		experiment.baselineModel(execConfig, 0);
		experiment.initialiseStatus(execConfig);
		return experiment.build();
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
		
	private void setupOutbreak(SetupConfiguration setupConfig, String urnBase, int replica) {
		outbreak.setUrn(urnBase+":"+setupConfig.getName()+":"+replica);
		Sampler sampler = Sampler.getSampler(outbreak.getUrn());
		setupFn.getSelector().test(setupConfig);
		setupFn.getConsumer().accept(outbreak, setupConfig, sampler);
	}
	
	private void baselineModel(ExecutionConfiguration execConfig, int replica) {
		outbreak.setUrn(
			outbreak.getUrn()+":"+execConfig.getName()+":"+replica	
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
	
//	private PersonBaseline baselineFrom(ExecutionConfiguration configuration, Sampler rng) {
//		double trigger = rng.logitNormal(configuration.getRiskTriggerMedian(), configuration.getRiskTriggerScale());
//		return ImmutablePersonBaseline.builder()
//				.setSocialMobility(
//					(double) rng.logitNormal( 
//							configuration.getMedianContactProbability(),
//							configuration.getScaleContactProbability()
//					)
//				)
//				.setLowRiskMobilityIncreaseTrigger(trigger * 1/Math.sqrt(configuration.getRiskTriggerRatio()))
//				.setHighRiskMobilityDecreaseTrigger(trigger * Math.sqrt(configuration.getRiskTriggerRatio()))
//				.setHighRiskMobilityModifier(configuration.getHighRiskMobilityModifier())
//				.build();
//	}
	
//	private OutbreakBaseline outbreakBaselineFrom(ExecutionConfiguration configuration, Sampler rng) {
//		SimpleWeightedGraph<Person, Person.Relationship> contacts = outbreak.getSocialNetwork();
//		long peopleCount = contacts.iterables().vertexCount();
//		double meanContactWeight = StreamSupport.stream(
//							contacts.iterables().edges().spliterator(),false
//						).mapToDouble(
//							c -> c.getConnectednessQuantile()*
//								configuration.getMedianContactProbability()*
//								contacts.getEdgeSource(c).getBaseline().getSocialMobility()*
//								contacts.getEdgeTarget(c).getBaseline().getSocialMobility()
//						).sum() / peopleCount;
//		double pTransmission = configuration.getRO()/meanContactWeight;
//		
//		if (pTransmission > 1) {
//			log.warn("Cannot satisfy R0 with current network structure.");
//		}
//		DelayDistribution tmp = configuration.getGenerationTime();
//		
//		return ImmutableOutbreakBaseline
//				.builder()
//				.setPInfectionGivenInfectiousContact(tmp.conditionedOn(pTransmission))
//				.build();
//	}
	
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
	
//	private PersonState personStatusFrom(Person person, ExecutionConfiguration execConfig, Sampler sampler) {
//		// TODO Auto-generated method stub
//		return ImmutablePersonState.builder()
//				.setEntity(person)
//				.setState(InfectionState.SUSCEPTIBLE)
//				.setTime(0)
//				.build();
//	}
//	
//	
//	private OutbreakState outbreakStatusFrom(Outbreak outbreak, ExecutionConfiguration execConfig, Sampler sampler) {
//		// TODO Auto-generated method stub
//		return ImmutableOutbreakState.builder()
//				.setEntity(outbreak)
//				.setTime(0)
//				.build();
//	}

	public Outbreak build() {
		if (!outbreak.isInitialized()) 
			throw new RuntimeException("Not initialised");
		return outbreak;
	}
	
}
