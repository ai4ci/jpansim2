package io.github.ai4ci.example;

import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.behaviour.SmartAgentLFTTesting;
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.behaviour.Symptomatic;
import io.github.ai4ci.abm.behaviour.Test;
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
						.setDefaultBehaviourModelName(
							SmartAgentTesting.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("smart-agent-lft")
						.setDefaultBehaviourModelName(
							SmartAgentLFTTesting.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("reactive-test")
						.setDefaultBehaviourModelName(
							ReactiveTestAndIsolate.class.getSimpleName()
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("symptom-management")
						.setDefaultBehaviourModelName(
							Symptomatic.class.getSimpleName()
						)
						.build()
				)
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
						.withDefaultBehaviourModelName(Test.class.getSimpleName())
						.withImportationProbability(0D) // .001D)
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

	/**
	 * An experiment comparing age‑stratified and non‑stratified demographics.
	 */
	AGE_STRAT(
			"age-stratification",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withSimulationDuration(200)
						.withUrnBase("age-stratification")
						.withExporters(Exporters.values())
				)
				.withSetupConfig(
					SetupConfiguration.DEFAULT
						.withDemographics(AgeStratifiedDemography.DEFAULT)
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
						.setInHostConfiguration(MarkovStateModel.DEFAULT)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("phenomenological")
						.setInHostConfiguration(PhenomenologicalModel.DEFAULT)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("stochastic")
						.setInHostConfiguration(StochasticModel.DEFAULT)
						.build()
				)
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
					ExecutionConfiguration.DEFAULT
						.withInHostConfiguration(MarkovStateModel.DEFAULT)
						.withDefaultBehaviourModelName(
							ReactiveTestAndIsolate.class.getSimpleName()
						)
						.withImportationProbability(0D)
					// .setInHostConfiguration(StochasticModel.DEFAULT)
				)
				.withFacet(
					"trigger",
					PartialExecutionConfiguration.builder()
						.setName("none")
						.setDefaultPolicyModelName(NoControl.class.getSimpleName())
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("5%-1%")
						.setDefaultPolicyModelName(
							ReactiveLockdown.class.getSimpleName()
						)
						.setLockdownStartTrigger(0.05)
						.setLockdownReleaseTrigger(0.01)
						.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
						.setInitialScreeningProbability(0.01)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("10%-2%")
						.setDefaultPolicyModelName(
							ReactiveLockdown.class.getSimpleName()
						)
						.setLockdownStartTrigger(0.1)
						.setLockdownReleaseTrigger(0.02)
						.setLockdownTriggerValue(Value.SCREENING_TEST_POSITIVITY)
						.setInitialScreeningProbability(0.01)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("15%-3%")
						.setDefaultPolicyModelName(
							ReactiveLockdown.class.getSimpleName()
						)
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
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(1.0)
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("mild")
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(0.75)
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("moderate")
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(0.5)
						)
						.build(),
					PartialExecutionConfiguration.builder()
						.setName("severe")
						.setMaximumSocialContactReduction(
							SimpleDistribution.point(0.25)
						)
						.build()
				)

	),

	/**
	 * An experiment comparing different network types.
	 */
	NETWORKS(
			"network-type",
			ExperimentConfiguration.DEFAULT
				.withBatchConfig(
					BatchConfiguration.DEFAULT.withExporters(Exporters.values())
						.withSimulationDuration(200)
						.withUrnBase("networks")
				)
				.withSetupConfig(
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("erdos-reyni")
							.withNetwork(ErdosReyniConfiguration.DEFAULT)
					),
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("watts-strogatz")
							.withNetwork(WattsStrogatzConfiguration.DEFAULT)
					),
					SetupFacet.of(
						SetupConfiguration.DEFAULT.withName("barabasi-albert")
							.withNetwork(BarabasiAlbertConfiguration.DEFAULT)
					)
				)
				.withExecutionConfig(
					ExecutionConfiguration.DEFAULT
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
						.withDefaultBehaviourModelName(
							NonCompliant.class.getSimpleName()
						)
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
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
					ExecutionConfiguration.DEFAULT
						.withInHostConfiguration(PhenomenologicalModel.DEFAULT)
						.withDefaultBehaviourModelName(
							NonCompliant.class.getSimpleName()
						)
						.withDefaultPolicyModelName(NoControl.class.getSimpleName())
						.withImportationProbability(0D)
						.withR0(2.5)
					// .setInHostConfiguration(StochasticModel.DEFAULT)
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

	private Experiment(String name, ImmutableExperimentConfiguration config) {
		this.name = name;
		this.config = config;
	}

}
