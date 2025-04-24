package io.github.ai4ci.abm;
import static io.github.ai4ci.abm.mechanics.ModelOperation.baselineOutbreak;
import static io.github.ai4ci.abm.mechanics.ModelOperation.baselinePerson;
import static io.github.ai4ci.abm.mechanics.ModelOperation.initialiseOutbreak;
import static io.github.ai4ci.abm.mechanics.ModelOperation.initialisePerson;
import static io.github.ai4ci.abm.mechanics.ModelOperation.setupOutbreak;

import java.io.Serializable;
import java.util.function.Supplier;

import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.mechanics.ModelOperation;
import io.github.ai4ci.abm.mechanics.ModelOperation.OutbreakBaseliner;
import io.github.ai4ci.abm.mechanics.ModelOperation.OutbreakInitialiser;
import io.github.ai4ci.abm.mechanics.ModelOperation.OutbreakSetup;
import io.github.ai4ci.abm.mechanics.ModelOperation.PersonBaseliner;
import io.github.ai4ci.abm.mechanics.ModelOperation.PersonInitialiser;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.config.WattsStrogatzConfiguration;

public class ModelBuild {

	static Logger log = LoggerFactory.getLogger(ModelBuild.class);
	
	public static OutbreakSetup getSetupForConfiguration(SetupConfiguration config) {
		if (config instanceof WattsStrogatzConfiguration) return OutbreakSetupFn.DEFAULT.fn();
		throw new RuntimeException("Undefined setup configuration type");
	}
	
	public static enum OutbreakSetupFn {
		
		@SuppressWarnings("unchecked")
		DEFAULT(
			setupOutbreak((outbreak, config, sampler) -> {
				WattsStrogatzConfiguration setupConfig = (WattsStrogatzConfiguration) config; 
				outbreak.setSetupConfiguration( setupConfig );
				outbreak.setSocialNetwork(
						new SimpleWeightedGraph<Person, Person.Relationship>(
								(Supplier<Person> & Serializable) () -> Person.createPersonStub(outbreak), 
								(Supplier<Person.Relationship> & Serializable) () -> new Person.Relationship()
						)
					);
//				outbreak.setInfections(
//						NetworkBuilder.from(new DirectedAcyclicGraph<PersonHistory, PersonHistory.Infection>(
//								PersonHistory.Infection.class
//								)));
			
				log.debug("Initialising network config");
				
				if (!(outbreak.initialisedSocialNetwork()
						// && outbreak.initialisedInfections()
						)) throw new RuntimeException("Already configured");
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
				// This sets the weight of the network. This is accessible as 
				// the "connectednessQuantile" of the relationship.
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
						// Host behaviour setup
						 
						.setMobilityBaseline(	Math.sqrt( configuration.getContactProbability().sample(rng) ) )
						.setTransmissibilityModifier(	rng.gamma(1, 0.1) )
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
				
				builder
					.setDefaultPolicyState( configuration.getDefaultPolicyModel() )
					.setViralLoadTransmissibilityProbabilityFactor( 
							Calibration.inferViralLoadTransmissionProbabilityFactor(outbreak,
									configuration.getRO()
							) 
					);
				outbreak.getStateMachine().init( configuration.getDefaultPolicyModel() );
				
				
				 
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
					.setTransmissibilityModifier( 1.0 )
					.setPresumedInfectiousPeriod( config.getInitialEstimateInfectionDuration().intValue() )
					.setPresumedSymptomSensitivity( config.getInitialEstimateSymptomSensitivity() ) 
					.setPresumedSymptomSpecificity( config.getInitialEstimateSymptomSensitivity() );
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
							.setSusceptibilityModifier(1.0)
							
							.setContactDetectedProbability( params.getContactDetectedProbability().sample(rng) )
							// .setScreeningInterval( params.getScreeningPeriod().sample(rng) )
							.setInHostModel(
									params.getInHostConfiguration().initialise(person, rng)
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
