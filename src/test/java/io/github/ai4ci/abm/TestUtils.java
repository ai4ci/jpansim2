package io.github.ai4ci.abm;

import static io.github.ai4ci.functions.SimpleDistribution.point;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

import com.google.common.collect.Streams;

import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.abm.policy.Trigger;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.PartialSetupConfiguration;
import io.github.ai4ci.config.execution.DemographicAdjustment;
import io.github.ai4ci.config.execution.ImmutableExecutionConfiguration;
import io.github.ai4ci.config.inhost.ImmutableMarkovStateModel;
import io.github.ai4ci.config.setup.ImmutableErdosReyniConfiguration;
import io.github.ai4ci.config.setup.ImmutableSetupConfiguration;
import io.github.ai4ci.config.setup.ImmutableUnstratifiedDemography;
import io.github.ai4ci.example.Kernels;
import io.github.ai4ci.flow.ExecutionBuilder;
import io.github.ai4ci.flow.mechanics.Updater;
import io.github.ai4ci.util.ReflectionUtils;

/**
 * Allows a builder based construction of a test outbreak
 */
public class TestUtils {

	static {
		System.out.println("Booting up logger...");
		Configurator.initialize(new DefaultConfiguration());
		Configurator.setRootLevel(Level.INFO);
	}

	static Logger log = LogManager.getLogger();

	Function<PartialSetupConfiguration.Builder,PartialSetupConfiguration.Builder> setupTweak;
	Function<PartialExecutionConfiguration.Builder,PartialExecutionConfiguration.Builder> executionTweak;
	ModifiableOutbreak outbreak;
	Updater updater;

	private TestUtils(
			Function<PartialSetupConfiguration.Builder,PartialSetupConfiguration.Builder> setupTweak,
			Function<PartialExecutionConfiguration.Builder,PartialExecutionConfiguration.Builder> executionTweak) {
		log.info("Starting test...");
		this.setupTweak = setupTweak;
		this.executionTweak = executionTweak;
		this.outbreak = (ModifiableOutbreak) ExecutionBuilder.buildExperiment(
				ReflectionUtils.merge(
						MINIMAL_SETUP,
						setupTweak.apply(PartialSetupConfiguration.builder()).build()
						),
				ReflectionUtils.merge(
						MINIMAL_EXECUTION,
						executionTweak.apply(PartialExecutionConfiguration.builder()).build()
						),
				"experiment");
	}

	public static ImmutableErdosReyniConfiguration MINI_COMPLETE = ImmutableErdosReyniConfiguration.builder()
			.setNetworkSize(10)
			.setNetworkDegree(9)
			.build();

	public static ImmutableUnstratifiedDemography UNIFORM_RELATIONSHIPS = 
			ImmutableUnstratifiedDemography.builder()
			.setRelationshipStrengthDistribution( point(1.0) )
			.build(); 

	/**
	 * A setup for a fully connected network of equal strength relationships
	 * that has 10 nodes and no initial import of disease.
	 */
	public static ImmutableSetupConfiguration MINIMAL_SETUP = ImmutableSetupConfiguration.builder()
			.setName("test-setup")
			.setInitialImports(0)
			.setDemographics(UNIFORM_RELATIONSHIPS)
			.setNetwork(MINI_COMPLETE)
			.build();


	/**
	 * A minimal in host viral load markov model that has a very short 
	 * incubation period, and a exactly 10 day infectious period. 
	 */
	public static ImmutableMarkovStateModel MINIMAL_IN_HOST = ImmutableMarkovStateModel.builder()
			.setImmuneWaningHalfLife( point(100.0) )
			.setIncubationPeriod( point(0.0) )
			.setInfectiousDuration( point(10.0) )
			.setSymptomDuration( point(10.0) )
			.build();

	/**
	 * A minimal configuration for the simulation execution which has most
	 * parameters fixed to 1 except zero case fatality and case 
	 * hospitalisation rate. Default R0 is 4. 
	 */
	public static ImmutableExecutionConfiguration MINIMAL_EXECUTION = ImmutableExecutionConfiguration.builder()
			.setName("test-execution")
			.setR0(4.0)
			.setAsymptomaticFraction(0.0)
			.setCaseHospitalisationRate(0.0)
			.setCaseFatalityRate(0.0)
			.setContactProbability( point(1.0) )
			.setAppUseProbability( point(1.0) )
			.setContactDetectedProbability( 1.0 )
			.setComplianceProbability( point(1.0) )
			.setInHostConfiguration(MINIMAL_IN_HOST)
			.setLockdownStartTrigger( 0.05 )
			.setLockdownReleaseTrigger( 0.01 )
			.setLockdownTriggerValue(Trigger.Value.HOSPITAL_BURDEN)
			.setInitialScreeningProbability(0.01)
			.setDefaultPolicyModelName( NoControl.class.getSimpleName() )
			.setDefaultBehaviourModelName( NonCompliant.class.getSimpleName() )
			.setSymptomSensitivity( point(1.0) )
			.setSymptomSpecificity( point(1.0) )
			.setInitialEstimateSymptomSensitivity(0.5)
			.setInitialEstimateSymptomSpecificity(0.95)
			.setInitialEstimateIncubationPeriod(4.0)
			.setInitialEstimateInfectionDuration(10.0)
			.setRiskModelSymptomKernel(Kernels.DEFAULT_SYMPTOM_ONSET_KERNEL.kernel())
			.setRiskModelContactsKernel(Kernels.DEFAULT_CONTACT_KERNEL.kernel())
			.setRiskModelTestKernel(Kernels.DEFAULT_TEST_SAMPLE_KERNEL.kernel())
			.setMaximumSocialContactReduction( point( 1.0) )
			.setAvailableTests(TestResult.defaultTypes())
			.setImportationProbability(0D)
			.setDemographicAdjustment(DemographicAdjustment.EMPTY)
			.setComplianceDeteriorationRate(0.02)
			.setComplianceImprovementRate(0.01)
			.setOrganicRateOfMobilityChange(1.0 / 4)
			.setSmartAppRiskTrigger(0.05)
			.build();

	public ModifiableOutbreak getOutbreak() {
		return outbreak;
	};

	public ModifiablePerson getPerson() {
		return (ModifiablePerson) getOutbreak()
				.getPeople().get(0);
	}

	public ModifiablePerson getPerson(Predicate<Person> test) {
		return (ModifiablePerson) getOutbreak()
				.getPeople().stream().filter(test)
				.findFirst().get();
	}

	public PersonState getPersonState() {
		return getPerson().getCurrentState();
	}

	/** 
	 * Default test setup
	 * @see TestUtils#MINIMAL_EXECUTION
	 * @see TestUtils#MINIMAL_SETUP
	 * @see TestUtils#MINIMAL_IN_HOST 
	 * @return
	 */
	public static TestUtils defaultTest() {
		return new TestUtils(
				setup -> setup,
				exec -> exec
				);
	}

	public static ModifiableOutbreak mockOutbreak() {
		return TestUtils.defaultTest().getOutbreak();
	}

	public static ModifiablePerson mockPerson() {
		return (ModifiablePerson) mockOutbreak()
				.getPeople().get(0);
	}

	public static PersonState mockPersonState() {
		return mockPerson().getCurrentState();
	}

	public static TestUtils defaultWithAdjustments(
			Function<PartialSetupConfiguration.Builder,PartialSetupConfiguration.Builder> setupTweak,
			Function<PartialExecutionConfiguration.Builder,PartialExecutionConfiguration.Builder> executionTweak
			) {
		return new TestUtils(
				setupTweak,
				executionTweak
				);
	}

	public static TestUtils defaultWithSetup(
			Function<PartialSetupConfiguration.Builder,PartialSetupConfiguration.Builder> setupTweak
			) {
		return new TestUtils(
				setupTweak,
				exec -> exec
				);
	}

	/**
	 * 
	 * @param executionTweak a test utils object that 
	 * @return
	 */
	public static TestUtils defaultWithExecution(
			Function<PartialExecutionConfiguration.Builder,PartialExecutionConfiguration.Builder> executionTweak
			) {
		return new TestUtils(
				setup -> setup,
				executionTweak
				);
	}

	/**
	 * Create an updater that introduces an infection into patient zero on day 1  
	 * @return an unpdater.
	 */
	public TestUtils withPatientZero() {
		updater = TestUtils.updaterPatientZero();
		return this;
	}

	/**
	 * Create default updater
	 * @return an updater that has a custom rule to expose person 0 with a 
	 * single standard infectious dose at time 1. 
	 */
	public static Updater updaterPatientZero() {
		Updater u = new Updater();
		u.withPersonProcessor(
				pp -> pp.getCurrentState().getTime() == 1 && pp.getId() == 0, 
				(builder,person,rng) -> builder.setImportationExposure(1.0)
				);
		return u;
	}

	/**
	 * Iterates the test model model returning intermediate states for testing.
	 * @param max maximum number of iterations.
	 * @return a stream of outbreaks in time order.
	 */
	public Stream<ModifiableOutbreak> stream(int max) {
		return 
				Streams.concat(
						Stream.of(getOutbreak()),
						IntStream.range(0, max)
						.mapToObj(i -> {
							this.updater.update(outbreak);
							return outbreak;
						})
						);
	}
}
