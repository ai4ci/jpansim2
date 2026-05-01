package io.github.ai4ci.example;

import io.github.ai4ci.abm.behaviour.FixedBehaviour;
import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.behaviour.SmartAgentLFTTesting;
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.behaviour.Symptomatic;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.abm.policy.ReactiveLockdown;
import io.github.ai4ci.abm.policy.Trigger.Value;
import io.github.ai4ci.config.BatchConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.Exporters;
import io.github.ai4ci.config.ImmutableExperimentConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.SetupFacet;
import io.github.ai4ci.config.execution.DemographicAdjustment;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.config.setup.AgeStratifiedDemography;
import io.github.ai4ci.config.setup.BarabasiAlbertConfiguration;
import io.github.ai4ci.config.setup.ErdosReyniConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.config.setup.UnstratifiedDemography;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.functions.SimpleDistribution;

/**
 * Example experiments.
 *
 * <p>
 * This enum captures a set of example experiments that can be run via the
 * {@link io.github.ai4ci.example.RunExampleSim}. Each experiment is defined by
 * an {@link ImmutableExperimentConfiguration} that specifies the batch, setup
 * and execution configuration, and any facets. These configurations are used by
 * the runner to execute the experiment and generate results.
 *
 * <p>
 * Downstream uses include example code and tests that rely on these
 * configurations as compact baselines. The default experiment is used in
 * several places as a simple baseline.
 *
 * @author Rob Challen
 */
public enum Experiment {

	/**
	 * A simple default experiment.
	 *
	 * <p>
	 * This experiment provides a compact configuration that can be used as a
	 * simple baseline in examples and tests. It uses the default batch, setup
	 * and execution configurations, and does not include any facets. Downstream
	 * callers that rely on this simple default include example code and tests.
	 */
	DEFAULT(
			"default",
			ExperimentConfiguration.DEFAULT.withExecutionConfig(
				ExecutionConfiguration.DEFAULT
					.withDefaultPolicyModelName(NoControl.class.getSimpleName())
					.withDefaultBehaviourModelName(
						NonCompliant.class.getSimpleName()
					)
					.withImportationProbability(0D)
					.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
			)
	),

	/**
	 * An experiment comparing behaviour models.
	 */
	BEHAVIOUR(
			"behaviour-comparison",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withSimulationDuration(200)
						.withUrnBase("behaviour-comparison")
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(
							NonCompliant.class.getSimpleName()
						)
						.withImportationProbability(0D)// .setInHostConfiguration(StochasticModel.DEFAULT)
				)
				.withFacet(
					"behaviour",
					PartialExecutionConfiguration.builder()
						.setName("ignore")
						.setDefaultBehaviourModelName(
							FixedBehaviour.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(
							Symptomatic.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("reactive-test")
						.setDefaultBehaviourModelName(
							ReactiveTestAndIsolate.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("smart-agent")
						.setDefaultBehaviourModelName(
							SmartAgentTesting.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("smart-agent-lft")
						.setDefaultBehaviourModelName(
							SmartAgentLFTTesting.class.getSimpleName()
						)
						.build()
				)
				.withExecutionReplications(5)
				.withSetupReplications(5)
	),

	/**
	 * An experiment comparing different R0 values.
	 */
	R0(
			"test-R0",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withSimulationDuration(200)
						.withUrnBase("test-R0")
				)
				.withSetupConfig(SetupConfiguration.DEFAULT)
				.withSetupReplications(1)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(
							FixedBehaviour.class.getSimpleName()
						)
						.withImportationProbability(0D)// .001D)
					//						.withContactProbability( SimpleDistribution.unimodalBeta(0.1, 0.1) )
					//						// .withInHostConfiguration(StochasticModel.DEFAULT)
				)
				.withExecutionReplications(1)
				.withFacet(
					"R",
					PartialExecutionConfiguration.builder()
						.setName("1.0")
						.setR0(SimpleDistribution.point(1D))
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("2.0")
						.setR0(SimpleDistribution.point(2D))
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("3.0")
						.setR0(SimpleDistribution.point(3D))
						.build()
				)
				.withExecutionReplications(5)
				.withSetupReplications(5)
	),

	/**
	 * An experiment comparing age‑stratified and non‑stratified demographics.
	 */
	AGE_STRAT(
			"age-stratification",
			ExperimentConfiguration.DEFAULT.withDescription(
				"An uncontrolled outbreak with no controls, and fixed behaviour, in an age stratified population"
			)
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withSimulationDuration(200)
						.withUrnBase("age-stratification")
						.withExporters(Exporters.values())
						.withDescription("Exports all available data")
				)
				.withSetupConfig(
					SetupConfiguration.DEFAULT
						.withDemographics(AgeStratifiedDemography.DEFAULT)
						.withDescription()
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT.withDescription(
						"A configuration with no behaviour or policy control.",
						"Symptoms are completely sensitive and specific for disease."
					)
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(
							FixedBehaviour.class.getSimpleName()
						)
						.withImportationProbability(0D)
						.withDemographicAdjustment(DemographicAdjustment.AGE_DEFAULT)
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
				)
				.withSetupReplications(1)
				.withExecutionReplications(1)
	),

	/**
	 * An experiment comparing different in‑host models.
	 */
	IN_HOST(
			"in-host-test",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withExporters(Exporters.values())
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT.withDescription(
						"A default phenomenomolgical in-host model with no behaviour or policy control.",
						"Symptoms are completely sensitive and specific for disease."
					)
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withDefaultBehaviourModelName(
							FixedBehaviour.class.getSimpleName()
						)
						.withImportationProbability(0D)
						.withSymptomSensitivity(SimpleDistribution.point(1D))
						.withSymptomSpecificity(SimpleDistribution.point(1D))
				)
				.withFacet(
					"in-host-models",
					PartialExecutionConfiguration.builder()
						.setDescription(
							"Uses a markov-state model for in host viral load"
						)
						.setName("markov")
						.setInHostConfiguration(MarkovStateModel.DEFAULT)
						.build(),
					PartialExecutionConfiguration.builder()
						.setDescription(
							"Uses a phenomenological model for in host viral load"
						)
						.setName("phenomenological")
						.setInHostConfiguration(PhenomenologicalModel.DEFAULT)
						.build(),
					PartialExecutionConfiguration.builder()
						.setDescription(
							"Uses a target cell model for in host viral load"
						)
						.setName("stochastic")
						.setInHostConfiguration(StochasticModel.DEFAULT)
						.build()
				)
				.withSetupReplications(1)
				.withExecutionReplications(1)
	),

	/**
	 * An experiment comparing different lockdown triggers and isolation
	 * severities.
	 */
	LOCKDOWN(
			"lockdown-compliance",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withSimulationDuration(200)
						.withUrnBase("lockdown-compliance")
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT.withDescription(
						"A markov-state in-host, with agents who do not change their behaviour except for lockdowns."
					)
						.withInHostConfiguration(MarkovStateModel.DEFAULT)
						.withDefaultBehaviourModelName(
							FixedBehaviour.class.getSimpleName()
						)
						.withImportationProbability(0D)// .setInHostConfiguration(StochasticModel.DEFAULT)
				)
				.withFacet(
					"trigger",
					PartialExecutionConfiguration.builder()
						.setDescription("A default policy with no controls")
						.setName("none")
						.setDefaultPolicyModelName(NoControl.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setDescription(
							"Reactive lockdowns when screening tests have a 2% positivity"
						)
						.setName("2%-1%")
						.setDefaultPolicyModelName(
							ReactiveLockdown.class.getSimpleName()
						)
						.setLockdownStartTrigger(0.02)
						.setLockdownReleaseTrigger(0.01)
						.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
						.setInitialScreeningProbability(0.1)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("5%-2%")
						.setDescription(
							"Reactive lockdowns when screening tests have a 5% positivity"
						)
						.setDefaultPolicyModelName(
							ReactiveLockdown.class.getSimpleName()
						)
						.setLockdownStartTrigger(0.05)
						.setLockdownReleaseTrigger(0.02)
						.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
						.setInitialScreeningProbability(0.1)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("8%-3%")
						.setDefaultPolicyModelName(
							ReactiveLockdown.class.getSimpleName()
						)
						.setDescription(
							"Reactive lockdowns when screening tests have a 8% positivity"
						)
						.setLockdownStartTrigger(0.08)
						.setLockdownReleaseTrigger(0.03)
						.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
						.setInitialScreeningProbability(0.1)
						.build()
				)
				.withFacet("isolation", //					PartialExecutionConfiguration.builder()
				//						.setName("none")
				//						.setDescription("Lockdowns are completely ineffective")
				//						.setMaximumSocialContactReduction(
				//							SimpleDistribution.point(1.0)
				//						)
				//						.build(),
					PartialExecutionConfiguration.builder()
						.setName("mild")
						.setDescription("Lockdowns reduce contacts by 25%")
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(0.75)
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("moderate")
						.setDescription("Lockdowns reduce contacts by 50%")
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(0.5)
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("severe")
						.setDescription("Lockdowns reduce contacts by 75%")
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(0.25)
						)
						.build()
				)
				.withSetupReplications(5)
				.withExecutionReplications(5)

	),

	/**
	 * An experiment comparing different network types.
	 */
	NETWORKS(
			"network-type",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withSimulationDuration(200)
						.withUrnBase("networks")
				)
				.withSetupConfig(
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("erdos-reyni")
							.withNetwork(ErdosReyniConfiguration.DEFAULT)
							.withDemographics(UnstratifiedDemography.DEFAULT)
							.withCalibrateR0ToNetwork(false)
					),
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("watts-strogatz")
							.withNetwork(WattsStrogatzConfiguration.DEFAULT)
							.withDemographics(UnstratifiedDemography.DEFAULT)
							.withCalibrateR0ToNetwork(false)
					),
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("barabasi-albert")
							.withNetwork(BarabasiAlbertConfiguration.DEFAULT)
							.withDemographics(UnstratifiedDemography.DEFAULT)
							.withCalibrateR0ToNetwork(false)
					)
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT.withDescription(
						"A default phenomenomolgical in-host model with no behaviour or policy control."
					)
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
						.withDefaultBehaviourModelName(
							NonCompliant.class.getSimpleName()
						)
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withImportationProbability(0D)
						.withR0(SimpleDistribution.point(2.5))// .setInHostConfiguration(StochasticModel.DEFAULT)
				)
				.withFacet(
					"R",
					PartialExecutionConfiguration.builder()
						.setDescription("R0 is calibrated around 1.")
						.setName("1.0")
						.setR0(SimpleDistribution.point(1D))
						.build(),
					PartialExecutionConfiguration.builder()
						.setDescription("R0 is calibrated around 2.")
						.setName("2.0")
						.setR0(SimpleDistribution.point(2D))
						.build(),
					PartialExecutionConfiguration.builder()
						.setDescription("R0 is calibrated around 3.")
						.setName("3.0")
						.setR0(SimpleDistribution.point(3D))
						.build()
				)
				.withSetupReplications(5)
				.withExecutionReplications(5)
	),

	/**
	 * An experiment comparing different network types.
	 */
	TEST_CONCURRENCY(
			"concurrency-test",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withExporters(Exporters.values())
						.withSimulationDuration(200)
						.withUrnBase("concurrency")
				)
				.withSetupConfig(
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("barabasi-albert")
							.withNetwork(BarabasiAlbertConfiguration.DEFAULT)
					)
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT.withDescription(
						"A default phenomenomolgical in-host model with no behaviour or policy control and R0 of 2.5."
					)
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
						.withDefaultBehaviourModelName(
							NonCompliant.class.getSimpleName()
						)
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withImportationProbability(0D)
						.withR0(SimpleDistribution.point(2.5))// .setInHostConfiguration(StochasticModel.DEFAULT)
				)
	)

	;

	/**
	 * Name and configuration for the experiment.
	 *
	 * <p>
	 * These fields are used by the runner to execute the experiment and generate
	 * results. The name is used in the urn base and output paths, and the
	 * configuration specifies the batch, setup and execution configuration, and
	 * any facets.
	 */
	public String name;

	/**
	 * The experiment configuration specifying the batch, setup and execution
	 * configuration, and any facets. This is used by the runner to execute the
	 * experiment and generate results.
	 */
	public ImmutableExperimentConfiguration config;

	Experiment(String name, ImmutableExperimentConfiguration config) {
		this.name = name;
		this.config = config;
	}

}
