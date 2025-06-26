package io.github.ai4ci.config;

import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.behaviour.SmartAgentLFTTesting;
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.behaviour.Symptomatic;
import io.github.ai4ci.abm.behaviour.Test;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.abm.policy.ReactiveLockdown;
import io.github.ai4ci.abm.policy.Trigger.Value;
import io.github.ai4ci.config.ExperimentFacet.SetupFacet;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.config.setup.AgeStratifiedDemography;
import io.github.ai4ci.config.setup.BarabasiAlbertConfiguration;
import io.github.ai4ci.config.setup.ErdosReyniConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.util.SimpleDistribution;

public enum ExampleConfig {

	DEFAULT ("default",
			ExperimentConfiguration.DEFAULT.withExecutionConfig(
					ExecutionConfiguration.DEFAULT
					.withDefaultPolicyModelName( NoControl.class.getSimpleName())
					.withDefaultBehaviourModelName( NonCompliant.class.getSimpleName())
					.withImportationProbability(0D)
					.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
				)
			),
	
	BEHAVIOUR ("behaviour-comparison",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("behaviour-comparison")
						)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName( NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName( NonCompliant.class.getSimpleName())
						.withImportationProbability(0D)
						// .setInHostConfiguration(StochasticModel.DEFAULT)
						)
				.withFacet(
						"behaviour", 
						PartialExecutionConfiguration.builder()
						.setName("ignore")
						.setDefaultBehaviourModelName(Test.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("smart-agent")
						.setDefaultBehaviourModelName(SmartAgentTesting.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("smart-agent-lft")
						.setDefaultBehaviourModelName(SmartAgentLFTTesting.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("reactive-test")
						.setDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(Symptomatic.class.getSimpleName())
						.build()
						)
				),

		R0 ("test-R0",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("test-R0")
						)
				.withSetupConfig(
						SetupConfiguration.DEFAULT
				)
				.withSetupReplications(1)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(Test.class.getSimpleName())
						.withImportationProbability(0D) //.001D)
//						.withContactProbability( SimpleDistribution.unimodalBeta(0.1, 0.1) )
//						// .withInHostConfiguration(StochasticModel.DEFAULT)
						)
				.withExecutionReplications(1)
				.withFacet(
						"R",
						PartialExecutionConfiguration.builder()
						.setName("1.0")
						.setR0(1D)
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("2.0")
						.setR0(2D)
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("3.0")
						.setR0(3D)
						.build()
						)
				),

		AGE_STRAT ("age-stratification",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("age-stratification")
						.withExporters(Exporters.values())
						)
				.withSetupConfig(
						SetupConfiguration.DEFAULT
							.withDemographics(
								AgeStratifiedDemography.DEFAULT
							)
				)
				.withSetupReplications(1)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(Test.class.getSimpleName())
						.withImportationProbability(0D)
						.withDemographicAdjustment(DemographicAdjustment.AGE_DEFAULT)
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
					)
				.withExecutionReplications(1)
				),

		IN_HOST ("in-host-test", 
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
							.withExporters(Exporters.values())
				)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(Test.class.getSimpleName())
						.withImportationProbability(0D)
						.withSymptomSensitivity(SimpleDistribution.point(1D))
						.withSymptomSpecificity(SimpleDistribution.point(1D))
				)
				.withFacet(
					"in-host-models",
					PartialExecutionConfiguration.builder()
						.setName("markov")
						.setInHostConfiguration(
							MarkovStateModel.DEFAULT
						)
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("phenomenological")
						.setInHostConfiguration(
							PhenomenologicalModel.DEFAULT
						)
						.build(),
						PartialExecutionConfiguration.builder()
						.setName("stochastic")
						.setInHostConfiguration(
							StochasticModel.DEFAULT
						)
						.build()
				)
			),
		
		LOCKDOWN ("lockdown-compliance",  
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT
						.withSimulationDuration(200)
						.withUrnBase("lockdown-compliance")
						)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withInHostConfiguration( MarkovStateModel.DEFAULT )
						.withDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
						.withImportationProbability(0D)
						// .setInHostConfiguration(StochasticModel.DEFAULT)
						)
				.withFacet("trigger", 
						PartialExecutionConfiguration.builder()
							.setName("none")
							.setDefaultPolicyModelName(NoControl.class.getSimpleName())
							.build(),
						PartialExecutionConfiguration.builder()
							.setName("5%-1%")
							.setDefaultPolicyModelName(ReactiveLockdown.class.getSimpleName())
							.setLockdownStartTrigger(0.05)
							.setLockdownReleaseTrigger(0.01)
							.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
							.setInitialScreeningProbability(0.01)
							.build(),
						PartialExecutionConfiguration.builder()
							.setName("10%-2%")
							.setDefaultPolicyModelName(ReactiveLockdown.class.getSimpleName())
							.setLockdownStartTrigger(0.1)
							.setLockdownReleaseTrigger(0.02)
							.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
							.setInitialScreeningProbability(0.01)
							.build(),
						PartialExecutionConfiguration.builder()
							.setName("15%-3%")
							.setDefaultPolicyModelName(ReactiveLockdown.class.getSimpleName())
							.setLockdownStartTrigger(0.15)
							.setLockdownReleaseTrigger(0.03)
							.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
							.setInitialScreeningProbability(0.01)
							.build()	
				)
				.withFacet(
						"isolation",
						PartialExecutionConfiguration.builder()
							.setName("none")
							.setMaximumSocialContactReduction(SimpleDistribution.point(1.0))
							.build(),
						PartialExecutionConfiguration.builder()
							.setName("mild")
							.setMaximumSocialContactReduction(SimpleDistribution.point(0.75))
							.build(),
						PartialExecutionConfiguration.builder()
							.setName("moderate")
							.setMaximumSocialContactReduction(SimpleDistribution.point(0.5))
							.build(),
						PartialExecutionConfiguration.builder()
							.setName("severe")
							.setMaximumSocialContactReduction(SimpleDistribution.point(0.25))
							.build()
				)
				
				
		),
		
		NETWORKS ("network-type",
				ExperimentConfiguration.DEFAULT
				.withBatchConfig(
						BatchConfiguration.DEFAULT.withExporters(
								Exporters.values()
						)
						.withSimulationDuration(200)
						.withUrnBase("networks")
						)
				.withSetupConfig(
						SetupFacet.of(
								SetupConfiguration.DEFAULT
								.withName("erdos-reyni")
								.withNetwork(
									ErdosReyniConfiguration.DEFAULT
								)
						),
						SetupFacet.of(
							SetupConfiguration.DEFAULT
							.withName("watts-strogatz")
							.withNetwork(
									WattsStrogatzConfiguration.DEFAULT
							)
						),
						SetupFacet.of(
								SetupConfiguration.DEFAULT
								.withName("barabasi-albert")
								.withNetwork(
									BarabasiAlbertConfiguration.DEFAULT
								)
						)
				)
				.withExecutionConfig(
						ExecutionConfiguration.DEFAULT
						.withInHostConfiguration( PhenomenologicalModel.DEFAULT )
						.withDefaultBehaviourModelName( NonCompliant.class.getSimpleName())
						.withDefaultPolicyModelName( NoControl.class.getSimpleName())
						.withImportationProbability(0D)
						.withR0(2.5)
						// .setInHostConfiguration(StochasticModel.DEFAULT)
						)
				.withFacet(
					"R",
					PartialExecutionConfiguration.builder()
						.setName("1.0")
						.setR0(1D)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("2.0")
						.setR0(2D)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("3.0")
						.setR0(3D)
						.build()
					)
		),
		
		TEST_CONCURRENCY ("concurrency-test",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withExporters(
							Exporters.values()
					)
				.withSimulationDuration(200)
				.withUrnBase("concurrency")
			)
			.withSetupConfig(
				SetupFacet.of(
					SetupConfiguration.DEFAULT
					.withName("barabasi-albert")
					.withNetwork(
						BarabasiAlbertConfiguration.DEFAULT
					)
				)
			)
			.withExecutionConfig(
				ExecutionConfiguration.DEFAULT
					.withInHostConfiguration( PhenomenologicalModel.DEFAULT )
					.withDefaultBehaviourModelName( NonCompliant.class.getSimpleName())
					.withDefaultPolicyModelName( NoControl.class.getSimpleName())
					.withImportationProbability(0D)
					.withR0(2.5)
					// .setInHostConfiguration(StochasticModel.DEFAULT)
				)
		)
		
		
	;
			
	public String name;
	public ImmutableExperimentConfiguration config;
	ExampleConfig(String name, ImmutableExperimentConfiguration config) {this.name = name;this.config=config;}
	
	

	

}
