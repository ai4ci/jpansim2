package io.github.ai4ci.abm;
import static io.github.ai4ci.abm.ModelOperation.baselineOutbreak;
import static io.github.ai4ci.abm.ModelOperation.baselinePerson;
import static io.github.ai4ci.abm.ModelOperation.initialiseOutbreak;
import static io.github.ai4ci.abm.ModelOperation.initialisePerson;
import static io.github.ai4ci.abm.ModelOperation.setupOutbreak;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.ModelOperation.OutbreakBaseliner;
import io.github.ai4ci.abm.ModelOperation.OutbreakInitialiser;
import io.github.ai4ci.abm.ModelOperation.OutbreakSetup;
import io.github.ai4ci.abm.ModelOperation.PersonBaseliner;
import io.github.ai4ci.abm.ModelOperation.PersonInitialiser;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
public class ModelBuild {

	static Logger log = LoggerFactory.getLogger(ModelBuild.class);
	
	public static enum OutbreakSetupFn {
		
		@SuppressWarnings("unchecked")
		DEFAULT(
			setupOutbreak((outbreak, setupConfig, sampler) -> {
				outbreak.setSetupConfiguration(setupConfig);
				outbreak.setSocialNetwork(
						new SimpleWeightedGraph<Person, Person.Relationship>(
								(Supplier<Person> & Serializable) () -> Person.createPersonStub(outbreak), 
								(Supplier<Person.Relationship> & Serializable) () -> new Person.Relationship()
						)
					);
				outbreak.setInfections(
						new DirectedAcyclicGraph<PersonHistory, PersonHistory.Infection>(
								PersonHistory.Infection.class
								));
			
				log.debug("Initialising network config");
				
				if (!(outbreak.initialisedSocialNetwork() && outbreak.initialisedInfections())) throw new RuntimeException("Already configured");
				WattsStrogatzGraphGenerator<Person, Person.Relationship> gen = 
						new WattsStrogatzGraphGenerator<Person, Person.Relationship>(
								setupConfig.getNetworkSize(),
								Math.min(
									setupConfig.getNetworkConnectedness(),
									setupConfig.getNetworkSize()
								) / 2 * 2,
								setupConfig.getNetworkRandomness()
						);
				SimpleWeightedGraph<Person, Person.Relationship> socialNetwork = outbreak.getSocialNetwork();
				gen.generateGraph(socialNetwork);
				log.debug("contact graph {} edges, {} average degree ", 
						socialNetwork.iterables().edgeCount(),
						Calibration.getConnectedness(outbreak)
						);
				socialNetwork.edgeSet().forEach(r -> socialNetwork.setEdgeWeight(r, sampler.uniform()));
			})
		);
		
		OutbreakSetup fn;
		OutbreakSetupFn(ModelOperation.OutbreakSetup fn) {this.fn=fn;}
		public OutbreakSetup fn() {return fn;}
	}
	
	public static enum PersonBaselinerFn {
		
		DEFAULT(
			baselinePerson((builder, person, rng) -> {
				ExecutionConfiguration configuration = person.getOutbreak().getExecutionConfiguration();
				person.getStateMachine().init(configuration.getDefaultBehaviourModel());
				
				// TODO: in the future this will have to be reconfigured to fit with 
				// different types of person e.g. age groups etc.
				// Possibly all of this can be some kind of function of the person demographics?
				
				builder
						
						// Behaviour modification
//						.setLowRiskMobilityIncreaseTrigger(trigger * 1/Math.sqrt(configuration.getRiskTriggerRatio()))
//						.setHighRiskMobilityDecreaseTrigger(trigger * Math.sqrt(configuration.getRiskTriggerRatio()))
//						.setHighRiskMobilityModifier(configuration.getHighRiskMobilityModifier())
						
						//Host immunity setup 
						.setImmuneTargetRatio(configuration.getImmuneTargetRatio().sample(rng))
						.setImmuneActivationRate(configuration.getImmuneActivationRate().sample(rng))
						.setImmuneWaningRate(configuration.getImmuneWaningRate().sample(rng))
						.setTargetCellCount(configuration.getTargetCellCount())
						.setTargetRecoveryRate(configuration.getTargetRecoveryRate().sample(rng))
						.setInfectionCarrierProbability(configuration.getInfectionCarrierProbability().sample(rng))
						
						// Host behaviour setup
						 
						.setMobilityBaseline(	configuration.getContactProbability().sample(rng) )
						.setTransmissibilityBaseline(	configuration.getConditionalTransmissionProbability().sample(rng) )
						.setComplianceBaseline( configuration.getComplianceProbability().sample(rng))
						.setAppUseProbability( configuration.getAppUseProbability().sample(rng))
						.setDefaultBehaviourState( configuration.getDefaultBehaviourModel() )
						
						.setSymptomSensitivity( configuration.getSymptomSensitivity().sample(rng))
						.setSymptomSpecificity( configuration.getSymptomSpecificity().sample(rng))
						.setSelfIsolationDepth( configuration.getMaximumSocialContactReduction().sample(rng) )
						
						;
			})
		);
		
		PersonBaseliner fn;
		PersonBaselinerFn(ModelOperation.PersonBaseliner fn) {this.fn=fn;}
		public PersonBaseliner fn() {return fn;}
	}
	
	public static enum OutbreakBaselinerFn {
		
		DEFAULT(
			baselineOutbreak((builder, outbreak, rng) -> {
				ExecutionConfiguration configuration = outbreak.getExecutionConfiguration();
				
				builder.setDefaultPolicyState( configuration.getDefaultPolicyModel() );
				outbreak.getStateMachine().init(configuration.getDefaultPolicyModel());
				
				// TODO: Viral load model calibration.
				// Currently this is set on defaults but needs to look at 
				// latent period, infectious period, virion cutoff
				// and symptom delay, recovery delay, target cell cutoff.
				 
			})
		);
		
		OutbreakBaseliner fn;
		OutbreakBaselinerFn(ModelOperation.OutbreakBaseliner fn) {this.fn=fn;}
		public OutbreakBaseliner fn() {return fn;}
	}
	
	
	public static enum OutbreakStateInitialiserFn {
		
		DEFAULT(
			initialiseOutbreak((builder, outbreak, rng) -> {
				ExecutionConfiguration config = outbreak.getExecutionConfiguration();
				builder
					.setViralActivityModifier(1D)
					.setPresumedInfectiousPeriod( config.getPresumedInfectionDuration().intValue() )
					// TODO: parametrise?
					.setPresumedSymptomSensitivity(0.5) 
					.setPresumedSymptomSpecificity(0.9);
			})
		);
		
		OutbreakInitialiser fn;
		OutbreakStateInitialiserFn(ModelOperation.OutbreakInitialiser fn) {this.fn=fn;}
		public OutbreakInitialiser fn() {return fn;}
	}
	
	public static enum PersonStateInitialiserFn {
		
		DEFAULT(
			initialisePerson(
					(person) -> true, // all people
					(builder, person, rng) -> {
						ExecutionConfiguration params = person.getOutbreak().getExecutionConfiguration();
						SetupConfiguration configuration = person.getOutbreak().getSetupConfiguration();
						// PersonBaseline baseline = person.getBaseline();
						double limit = ((double) configuration.getInitialImports())/configuration.getNetworkSize();
						
						builder
							.setTransmissibilityModifier(1.0)
							.setMobilityModifier(1.0)
							.setImmuneModifier(1.0)
							.setComplianceModifier(1.0)
							
							.setContactDetectedProbability( params.getContactDetectedProbability().sample(rng) )
							.setScreeningInterval( params.getScreeningPeriod().sample(rng) )
							.setViralLoad(
									ViralLoadState.initialise(person)
							)
							.setImportationExposure(
									rng.uniform() < limit ? 2.0 : 0
							)
							.setImmunisationDose(0D);
			})
		);
		
		PersonInitialiser fn;
		PersonStateInitialiserFn(ModelOperation.PersonInitialiser fn) {this.fn=fn;}
		public PersonInitialiser fn() {return fn;}
	}
	

	
	
}
