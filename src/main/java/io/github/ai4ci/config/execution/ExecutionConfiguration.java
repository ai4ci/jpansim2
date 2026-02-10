package io.github.ai4ci.config.execution;

import static io.github.ai4ci.functions.SimpleDistribution.unimodalBeta;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.abm.ModelUpdate.PersonUpdaterFn;
import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.abm.PersonBaseline;
import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.behaviour.BehaviourModel;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.policy.PolicyModel;
import io.github.ai4ci.abm.policy.ReactiveLockdown;
import io.github.ai4ci.abm.policy.Trigger;
import io.github.ai4ci.abm.riskmodel.RiskModel;
import io.github.ai4ci.config.TestParameters;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.example.Kernels;
import io.github.ai4ci.flow.mechanics.StateUtils;
import io.github.ai4ci.functions.DelayDistribution;
import io.github.ai4ci.functions.DiscreteFunction;
import io.github.ai4ci.functions.Distribution;
import io.github.ai4ci.functions.EmpiricalKernel;
import io.github.ai4ci.functions.GaussianKernel;
import io.github.ai4ci.functions.KernelFunction;
import io.github.ai4ci.util.ShallowList;

/**
 * Central configuration for executing epidemiological Agent-Based Model simulations.
 * 
 * <p>Controls all aspects of ABM execution including:
 * <ul>
 *   <li>Disease transmission parameters (R0, progression rates)</li>
 *   <li>Contact patterns and mobility</li>
 *   <li>Intervention policies (testing, lockdowns)</li>
 *   <li>Behavioral responses by individuals</li>
 *   <li>Within-host disease progression</li>
 * </ul>
 *
 * <p>The configuration influences both:
 * <ul>
 *   <li>Population-level dynamics (through policies and global parameters)</li>
 *   <li>Individual behaviors (through contact patterns, compliance, testing)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ExecutionConfiguration config = ExecutionConfiguration.DEFAULT
 *     .withReplicate(1)
 *     .withName("scenario1");
 * }</pre>
 *
 * @author Rob Challen
 * @see BehaviourModel
 * @see PolicyModel
 * @see InHostConfiguration
 */
@Value.Immutable
@JsonSerialize(as = ImmutableExecutionConfiguration.class)
@JsonDeserialize(as = ImmutableExecutionConfiguration.class)
public interface ExecutionConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable,
		DemographicAdjustment.Execution<Distribution, Double> {

	/**
	 * Default implementations of withReplicate and withName that return the same
	 * instance. This allows for immutability while still providing a fluent
	 * interface for setting these properties.
	 * 
	 * @param i the replicate number to set for this configuration
	 * @return the same instance of ExecutionConfiguration with the replicate number
	 *         set (default implementation returns the same instance)
	 */
	default ExecutionConfiguration withReplicate(Integer i) {
		return this;
	}

	/**
	 * Default implementation of withName that returns the same instance. This
	 * allows for immutability while still providing a fluent interface for setting
	 * the name property.
	 * 
	 * @param name the name to set for this configuration
	 * @return the same instance of ExecutionConfiguration with the name set
	 *         (default implementation returns the same instance)
	 */
	default ExecutionConfiguration withName(String name) {
		return this;
	}

	
	/**
	 * Default immutable configuration instance with baseline epidemiological parameters.
	 * 
	 * <p>This configuration provides reasonable defaults for:
	 * <ul>
	 *   <li>Transmission (R0=1.75)</li>
	 *   <li>Disease severity (5% hospitalisation, 1% fatality)</li>
	 *   <li>Testing (1% screening probability)</li>
	 *   <li>Contact patterns (50% baseline contact probability)</li>
	 *   <li>Behaviour models (ReactiveTestAndIsolate)</li>
	 *   <li>Policy models (ReactiveLockdown)</li>
	 * </ul>
	 *
	 * <p>Population-level impacts:
	 * <ul>
	 *   <li>Defines baseline transmission potential</li>
	 *   <li>Sets lockdown triggers (5% positivity to start, 1% to release)</li>
	 *   <li>Configures default testing parameters</li>
	 * </ul>
	 *
	 * <p>Individual-level impacts:
	 * <ul>
	 *   <li>Sets baseline contact probabilities</li>
	 *   <li>Configures symptom reporting behavior</li>
	 *   <li>Defines compliance rates (99% baseline)</li>
	 * </ul>
	 */
	public static ImmutableExecutionConfiguration DEFAULT = ImmutableExecutionConfiguration.builder()
			.setName("execution")
			.setR0(1.75)
			.setAsymptomaticFraction(0.5)
			.setCaseHospitalisationRate(0.05)
			.setCaseFatalityRate(0.01)

			.setContactProbability( unimodalBeta(0.5, 0.25))
			//.setContactProbability(uniform())
			.setAppUseProbability(unimodalBeta(0.97, 0.1))
			.setContactDetectedProbability(0.9)

			.setComplianceProbability(unimodalBeta(0.99, 0.1))

//			.setRiskTrigger( Distribution.point(0.05) )
//			.setRiskTriggerRatio(1.1)
//			.setHighRiskMobilityModifier(0.95)
//			.setHighRiskTransmissibilityModifier(0.95)

			.setInHostConfiguration(PhenomenologicalModel.DEFAULT)

			.setLockdownStartTrigger(0.05).setLockdownReleaseTrigger(0.01)
			.setLockdownTriggerValue(Trigger.Value.SCREENING_TEST_POSITIVITY)

//			.setLockdownMinDuration(14)
//			.setLockdownMobility(0.5)
//			.setLockdownTransmissibility(0.5)

//			.setScreeningPeriod( Distribution.gamma(7.0,1D) )
//			.setProbabilityScreened(0.01)
			.setInitialScreeningProbability(0.01)

			.setDefaultBehaviourModelName(ReactiveTestAndIsolate.class.getSimpleName())
			.setDefaultPolicyModelName(ReactiveLockdown.class.getSimpleName())

			.setSymptomSensitivity(unimodalBeta(0.5, 0.1)).setSymptomSpecificity(unimodalBeta(0.95, 0.1))

			.setInitialEstimateSymptomSensitivity(0.5).setInitialEstimateSymptomSpecificity(0.95)

			.setInitialEstimateIncubationPeriod(4.0).setInitialEstimateInfectionDuration(10.0)

			.setRiskModelSymptomKernel(Kernels.DEFAULT_SYMPTOM_ONSET_KERNEL.kernel())
			.setRiskModelContactsKernel(Kernels.DEFAULT_CONTACT_KERNEL.kernel())
			.setRiskModelTestKernel(Kernels.DEFAULT_TEST_SAMPLE_KERNEL.kernel())

			.setMaximumSocialContactReduction(unimodalBeta(0.25, 0.1))
			.setAvailableTests(TestResult.defaultTypes())

			.setImportationProbability(0D).setDemographicAdjustment(DemographicAdjustment.EMPTY)

			.setComplianceDeteriorationRate(0.02)
			.setComplianceImprovementRate(0.01)
			.setOrganicRateOfMobilityChange(1.0 / 4)

			.setSmartAppRiskTrigger(0.05)

			.build();

	/**
	 * Gets the basic reproduction number (R0) for the disease.
	 * 
	 * This is used by the {@link io.github.ai4ci.abm.Calibration calibration} 
	 * in conjunction with the {@link #getContactProbability() contact probability},
	 * the viral load profile from {@link #getViralLoadProfile()}, and the network 
	 * structure to calibrate the baseline per contact 
	 * {@link io.github.ai4ci.abm.OutbreakBaseline#getViralLoadTransmissibilityParameter() transmission parameter}.
	 * This is in turn used to produce per contact transmission probability based
	 * on the in host viral load of the infected individual and the contact 
	 * probabilities of the infected and susceptible individual.
	 * 
	 * @return Average number of secondary cases from one infected individual
	 * @see #getContactProbability() 
	 */
	Double getR0();

	/**
	 * The fraction of infections that remain asymptomatic. 
	 * 
	 * This is used by the {@link io.github.ai4ci.abm.Calibration calibration} to 
	 * set a threshold value for the normalised severity of the in-host model 
	 * below which the patient is deemed to be asymptomatic.
	 * 
	 * @return Value between 0 and 1
	 * @see #getInfectionCaseRate()
	 */
	Double getAsymptomaticFraction();

	/**
	 * The case hospitalisation rate for symptomatic cases. This is used by
	 * the {@link io.github.ai4ci.abm.Calibration calibration} to set a
	 * threshold value for the normalised severity of the in-host model above which the patient is deemed to be hospitalised.
	 * 
	 * @return Value between 0 and 1
	 * @see #getInfectionHospitalisationRate()
	 */
	Double getCaseHospitalisationRate();

	/**
	 * The case fatality rate for symptomatic cases. This is used by
	 * the {@link io.github.ai4ci.abm.Calibration calibration} to set a threshold value for the normalised severity 
	 * of the in-host model above which the patient is deemed to have died.
	 * 
	 * @return Value between 0 and 1
	 * @see #getInfectionFatalityRate() 
	 */
	Double getCaseFatalityRate();

	/**
	 * The baseline probability of random screening tests across the
	 * whole Outbreak.
	 * @return Value between 0 and 1
	 * @see #getAvailableTests()
	 */
	Double getInitialScreeningProbability();

	/**
	 * The overall rate of symptomatic cases across the whole
	 * Outbreak.
	 * @return 1 - asymptomaticFraction
	 */
	@JsonIgnore
	default Double getInfectionCaseRate() {
	    return 1 - getAsymptomaticFraction();
	}

	/**
	 * The overall hospitalisation rate across all infections across the whole
	 * Outbreak.
	 * @return infectionCaseRate * caseHospitalisationRate 
	 */
	@JsonIgnore
	default Double getInfectionHospitalisationRate() {
	    return getInfectionCaseRate() * this.getCaseHospitalisationRate();
	}

	/**
	 * The overall fatality rate across all infections across the whole
	 * Outbreak. 
	 * 
	 * @return infectionCaseRate * caseFatalityRate
	 */
	@JsonIgnore
	default Double getInfectionFatalityRate() {
	    return getInfectionCaseRate() * this.getCaseFatalityRate();
	}


	/**
	 * The proportion of a persons social network that they see each day in a fully
	 * mobile population. The {@link io.github.ai4ci.abm.PersonBaseline#getMobilityBaseline() mobility baseline} is determined from this but as the
	 * contact is the product of 2 peoples mobility the average probability of a
	 * contact will be the square of the central value. In the case of a
	 * Watts-Strogatz or similar social network setting this to be a uniform
	 * distribution is recommended to get an approximate scale free contact
	 * distribution.
	 * {@link io.github.ai4ci.abm.PersonBaseline#getMobilityBaseline()}
	 * <p>Individual-level impact (PersonBaseline):
	 * <ul>
	 *   <li>Used to determine each person's baseline mobility</li>  
	 *   <li>Directly affects transmission probability between agents</li>
	 * </ul>
	 * @return Probability distribution (typically unimodal beta)
	 */
	Distribution getContactProbability();

	/** 
	 * How likely was it that a contact was detected?
	 * 
	 * This describes the baseline probability that a contact with another 
	 * person is detected and recorded {@link OutbreakState#getContactDetectedProbability()}
	 * given both participants are using an app and make contact. It
	 * describes the technical quality of a contact tracing app.
	 * <p>Population-level impact (OutbreakState):
	 * @return a probability. 
	 */
	Double getContactDetectedProbability();

	/** 
	 * How likely was it that a person complies with guidance? 
	 * 
	 * This distribution is sampled from for each person in the network to set their
	 * baseline probability of compliance with isolation {@link PersonBaseline#getComplianceBaseline()} and other guidance when they are 
	 * asked to self isolate or testing is advised when they are deemed to be at high risk. This is 
	 * a key parameter for the effectiveness of interventions and can be adjusted 
	 * to reflect different levels of public adherence to health guidelines.
	 * <p>Individual-level impact (PersonBaseline)
	 * 
	 * @return a probability distribution of the population compliance probability.
	 */
	Distribution getComplianceProbability();

	/** 
	 * How likely was it that a person uses a contact tracing app?
	 * 
	 * This distribution is sampled from for each person in the network to set their
	 * baseline probability of using a contact tracing app {@link PersonBaseline#getAppUseProbability()}. 
	 * This parameter is important for modelling the effectiveness
	 * of digital contact tracing interventions, as higher app usage can lead to 
	 * more effective identification and isolation of contacts, thereby reducing 
	 * transmission. Adjusting this parameter allows for exploration of 
	 * different scenarios regarding public adoption of contact tracing technologies.
	 * <p>Individual-level impact (PersonBaseline)
	 * 
	 * @return a probability distribution of the population app usage probability.
	 */
	Distribution getAppUseProbability();

	// Distribution getRiskTrigger();

//	Double getRiskTriggerRatio();
//	Double getHighRiskMobilityModifier();
//	Double getHighRiskTransmissibilityModifier();

//	Double getLockdownMobility();
//	Double getLockdownTransmissibility();

//	Distribution getScreeningPeriod();
//	Double getProbabilityScreened();
	
	/**
	 * Gets the within-host disease progression configuration. The
	 * configuration defines the model used for simulating viral load dynamics, 
	 * symptom onset, and disease severity in infected individuals.
	 * 
	 * <p>Options include phenomenological models (e.g. viral load profiles based on empirical data)
	 * mechanistic Markov models of disease progression, and 
	 * mechanistic stochastic compartment models.
	 *
	 * <p>Individual-level impact (InHostConfiguration):
	 * <ul>  
	 *   <li>Controls viral load dynamics in infected individuals</li>
	 *   <li>Affects infectiousness profiles and symptom onset</li>
	 * </ul>
	 * @return InHostConfiguration instance
	 * @see io.github.ai4ci.config.inhost.PhenomenologicalModel
	 * @see io.github.ai4ci.config.inhost.MarkovStateModel
	 * @see io.github.ai4ci.config.inhost.StochasticModel
	 */
	InHostConfiguration getInHostConfiguration();

	/**
	 * Gets prevalence trigger value for initiating lockdowns. Used directly
	 * by {@link io.github.ai4ci.abm.policy.ReactiveLockdown} to
	 * determine when to activate restrictions based on the monitored epidemiological indicator.
	 * <p>Population-level impact (PolicyModel):
	 * <ul>
	 *   <li>Threshold trigger value rate to activate restrictions</li>
	 *   <li>Must be paired with PolicyModel implementation</li>  
	 * </ul>
	 * @return Trigger value (typically 0.05)
	 * @see #getLockdownTriggerValue()
	 */
	Double getLockdownStartTrigger();

	/**
	 * Gets prevalence trigger value for lifting lockdowns. Used directly
	 * by {@link io.github.ai4ci.abm.policy.ReactiveLockdown} to determine when 
	 * to lift restrictions based on the monitored epidemiological indicator.
	 * <p>Population-level impact (PolicyModel):
	 * <ul>
	 *   <li>Threshold trigger value to remove restrictions</li>
	 *   <li>Provides hysteresis to prevent rapid cycling</li>
	 * </ul>
	 * @return Trigger value (typically 0.01)
	 * @see #getLockdownTriggerValue()
	 */
	Double getLockdownReleaseTrigger();

	/**
	 * The type of trigger used for lockdown policies. This defines the 
	 * metric that is monitored to determine when to activate or lift lockdown measures.
	 * <p>Population-level impact:
	 *  <ul>
	 *  <li>Determines the epidemiological indicator used for lockdown decisions</li>
	 *  <li>Common options include screening test positivity, symptomatic case rates, or hospitalisation rates</li>
	 *  </ul>
	 * @return Trigger.Value enum indicating the type of trigger 
	 * (e.g., SCREENING_TEST_POSITIVITY, SYMPTOMATIC_CASES, HOSPITALISATION_RATE)
	 */
	Trigger.Value getLockdownTriggerValue();

	/**
	 * Gets the set of available test types in the simulation. Each test type is 
	 * defined by its parameters such as sensitivity, specificity, and turnaround time.
	 * <p>Standard defaults are provided for {@link io.github.ai4ci.abm.TestResult#defaultTypes() common test types} 
	 * (e.g., PCR, Lateral Flow) but this can be customized to explore different 
	 * testing strategies and their impact on outbreak dynamics.
	 * @return List of TestParameters defining the characteristics of each test type available for use in the simulation.
	 */
	ShallowList<TestParameters> getAvailableTests();

	/**
	 * Gets the sensitivity of symptom-based detection. This parameter defines 
	 * the probability at a population level that infected individuals will 
	 * experience symptoms when they have disease {@link io.github.ai4ci.abm.PersonBaseline#getSymptomSensitivity()}. This is sampled for each 
	 * individual to determine their baseline personal symptom sensitivity, which in turn 
	 * affects the likelihood of symptom onset. 
	 * <p>Individual-level impact (PersonBaseline):
	 * <ul>
	 *  <li>Determines the likelihood of symptom onset in infected agents</li>
	 *  <li>Affects the effectiveness of symptom-based interventions (e.g., self-isolation, testing)</li>
	 *  </ul>
	 * @return A probability distribution representing the population-level symptom sensitivity (e.g., unimodal beta distribution)
	 */
	Distribution getSymptomSensitivity();

	/**
	 * Gets the specificity of symptom-based detection. The complement of this 
	 * parameter defines the probability at a population level that 
	 * uninfected individuals will experience symptoms that are mistaken for 
	 * disease {@link io.github.ai4ci.abm.PersonBaseline#getSymptomSpecificity()}. This is sampled for 
	 * each individual to determine their baseline personal symptom specificity, 
	 * which in turn affects the baseline likelihood of false symptom onset.
	 * <p>Individual-level impact (PersonBaseline):
	 * <ul>
	 * <li>Determines the likelihood of false symptom onset in uninfected agents
	 * </li>
	 * <li>Affects the rate of false positives in symptom-based interventions</li>
	 * </ul>
	 * 
	 * @return A probability distribution representing the population-level symptom specificity (e.g., unimodal beta distribution)
	 */
	Distribution getSymptomSpecificity();

	/**
	 * The initial estimate of the duration of infectiousness 
	 * defines {@link io.github.ai4ci.abm.OutbreakState#getPresumedInfectiousPeriod() the presumed infectious period} 
	 * used in the initial outbreak state. The assumption of infectious period
	 * affects the duration of interventions such as isolation.
	 * <p>Population-level impact (OutbreakState)
	 * @return Initial estimate of infectious period duration in days (e.g., 10.0)
	 */
	Double getInitialEstimateInfectionDuration();

	/**
	 * The initial estimate of the duration of incubation period
	 * defines {@link io.github.ai4ci.abm.OutbreakState#getPresumedIncubationPeriod() the presumed incubation period} 
	 * used in the initial outbreak state. The assumption of the incubation 
	 * period affects decisions about the validity of recent tests.
	 * <p>Population-level impact (OutbreakState)
	 * @return Initial estimate of incubation period duration in days (e.g., 10.0)
	 */
	Double getInitialEstimateIncubationPeriod();

	/**
	 * The initial estimate of the duration of symptom sensitivity
	 * defines {@link io.github.ai4ci.abm.OutbreakState#getPresumedSymptomSensitivity() the presumed symptom sensitivity} 
	 * used in the initial outbreak state. The assumption of the symptom 
	 * sensitivity affects the calculation of perceived risk in the {@link RiskModel}
	 * resulting from symptoms.
	 * <p>Population-level impact (OutbreakState)
	 * @return Initial estimate of symptom sensitivity in terms of probability of symptom onset when infected (e.g., 0.5)
	 */
	Double getInitialEstimateSymptomSensitivity();
	// N.b. It is not individualised but in theory could be adjusted for 
	// demographics (e.g. {@link DemographicAdjustment.Execution}.

	/**
	 * The initial estimate of the duration of symptom specificity
	 * defines {@link io.github.ai4ci.abm.OutbreakState#getPresumedSymptomSpecificity() the presumed symptom specificity} 
	 * used in the initial outbreak state. The assumption of the symptom 
	 * specificity affects the calculation of perceived risk in the {@link RiskModel}
	 * resulting from symptoms.
	 * <p>Population-level impact (OutbreakState)
	 * @return Initial estimate of symptom specificity in terms of 1-probability of false symptom onset when uninfected (e.g., 0.95)
	 */
	Double getInitialEstimateSymptomSpecificity();

	/**
	 * Gets the kernel function for symptom-based risk assessment in the {@link RiskModel}. 
	 * This kernel defines how the presence and timing of symptoms contribute 
	 * to an individual's perceived risk of being infectious given symptoms.
	 * This can be expressed as a {@link GaussianKernel}, {@link EmpiricalKernel}
	 * or {@link DiscreteFunction} depending on the desired shape of the risk contribution over time.
	 * @return KernelFunction defining the temporal risk contribution of symptoms in the RiskModel.
	 */
	KernelFunction getRiskModelSymptomKernel();

	/**
	 * Gets the kernel function for test results based risk assessment in the {@link RiskModel}. 
	 * This kernel defines how the presence and timing of test results contribute 
	 * to an individual's perceived risk of being infectious given a positive test result.
	 * This can be expressed as a {@link GaussianKernel}, {@link EmpiricalKernel}
	 * or {@link DiscreteFunction} depending on the desired shape of the risk contribution over time.
	 * @return KernelFunction defining the temporal risk contribution of test results in the RiskModel.
	 */
	KernelFunction getRiskModelTestKernel();
	// Does this supercede the presumed incubation / infectious period parameters?

	/**
	 * Gets the kernel function for contact based risk assessment in the
	 * {@link RiskModel}. This kernel defines how the presence and timing of
	 * contacts with people who are themselves at risk of being infectious
	 * contribute to an individual's perceived risk of being infectious. This can be
	 * expressed as a {@link GaussianKernel}, {@link EmpiricalKernel} or
	 * {@link DiscreteFunction} depending on the desired shape of the risk
	 * contribution over time.
	 * 
	 * @return KernelFunction defining the temporal risk contribution of contacts in
	 *         the RiskModel.
	 */
	KernelFunction getRiskModelContactsKernel();

	/**
	 * Each individual has a personal maximum that their social contact can be
	 * reduced by {@link PersonBaseline#getSelfIsolationDepth()}, which is sampled
	 * from this distribution. This in turn is used by the behaviour models to limit
	 * the maximum reduction in contact that an individual can achieve when they are
	 * asked to self isolate (via {@link StateUtils}).
	 * 
	 * <p>
	 * Individual-level impact (PersonBaseline):
	 * <ul>
	 * <li>Determines the maximum possible reduction in social contacts for each
	 * agent</li>
	 * <li>Limits the effectiveness of self-isolation and other contact-reducing
	 * behaviors</li>
	 * </ul>
	 * 
	 * @return Distribution representing the population-level variability in maximum
	 *         social contact reduction (e.g., unimodal beta distribution with mean
	 *         around 0.25 to reflect that most people can reduce contacts by about
	 *         75% at most)
	 */
	Distribution getMaximumSocialContactReduction();

	

	/**
	 * The absolute rate of decrease in compliance in situations where the person is
	 * asked to self isolate. This is used direction by the behaviour models (via
	 * {@link StateUtils}) to determine how quickly an individual's compliance with
	 * isolation and other guidance deteriorates over time when they are asked to
	 * self isolate or are otherwise forced to do something. This parameter captures
	 * the phenomenon of "compliance fatigue". Although this is set across the whole
	 * population it is applied at the individual level and modifies an individual
	 * compliance probability that is set.
	 * <p>
	 * Population-level impact (BehaviourModel)
	 * 
	 * @return Absolute rate of compliance deterioration (e.g., 0.02 per day)
	 * @see StateUtils#complianceFatigue(io.github.ai4ci.abm.ImmutablePersonState.Builder,
	 *      io.github.ai4ci.abm.PersonState)
	 */
	Double getComplianceDeteriorationRate();
	// N.B. Maybe this should be a distribution. or maybe more like organic rate
	// of mobility change.

	/**
	 * The absolute rate of decrease in compliance in situations where it is deemed
	 * to improve. This is used directly by the behaviour models (via
	 * {@link StateUtils}) to determine how quickly an individual's compliance with
	 * isolation and other guidance improves when they are not subject to
	 * restrictions and are otherwise free to do something. This parameter captures
	 * the phenomenon of "compliance recovery".
	 * <p>
	 * Population-level impact (BehaviourModel)
	 * 
	 * @return Absolute rate of compliance improvement (e.g., 0.01 per day)
	 * @see StateUtils#complianceRestoreSlowly(io.github.ai4ci.abm.ImmutablePersonState.Builder,
	 *      io.github.ai4ci.abm.PersonState)
	 */
	Double getComplianceImprovementRate();

	/**
	 * Gets the decay rate of natural behaviour change in response to symptoms. This
	 * is used directly by the behaviour models (via {@link StateUtils}) to
	 * determine how quickly an individual's mobility reduces in response to
	 * symptoms and then recovers over time.
	 * <p>
	 * Individual-level impact:
	 * <ul>
	 * <li>Controls how quickly agents reduce contacts when symptomatic</li>
	 * <li>Affects the speed of organic transmission reduction</li>
	 * </ul>
	 * 
	 * @return Exponential decay rate (days^-1)
	 */
	Double getOrganicRateOfMobilityChange();

	/**
	 * This defines the probability of and individual with standard mobility
	 * being infected from outside the simulated population per time step.
	 * This will create a random exposure uniform across the population 
	 * depending on individual mobility.
	 * 
	 * This feature is experimental and may be revised or removed in future versions.
	 * It is likely that it will be replaced with temporal functions.
	 * @return Probability of external infection per time step for a fully mobile individual (e.g., 0.001)
	 * @see PersonUpdaterFn#IMPORTATION_PROTOCOL
	 */
	Double getImportationProbability();

	/**
	 * The local risk profile (assessed through tests, symptoms, contacts etc) which
	 * is sufficient to trigger a request for testing and reactive self isolation.
	 * This is used directly by the behaviour models to determine when an individual
	 * is deemed to be at high risk and should be advised to get tested and self
	 * isolate.
	 * 
	 * @return Probability threshold for triggering testing and isolation based on
	 *         risk assessment (e.g., 0.05)
	 * @see io.github.ai4ci.abm.behaviour.SmartAgentTesting#REACTIVE_PCR
	 * @see io.github.ai4ci.abm.behaviour.SmartAgentLFTTesting#REACTIVE_LFT
	 */
	Double getSmartAppRiskTrigger();
	
	/**
	 * Sets the initial behaviour model to be used by agents in the simulation. 
	 * This defines the default behavioural response of agents to various 
	 * triggers such as symptoms, test results, and policy interventions.
	 * The models are defined as enums implementing the {@link BehaviourModel} 
	 * interface and can be extended to include custom behaviour models. The
	 * initial state the person starts with is the first value of the enum.
	 * <p>Individual-level impact (BehaviourModel)
	 * @return The simple name of the enum class implementing the default 
	 *  BehaviourModel (e.g., "ReactiveTestAndIsolate")
	 *  or the fully qualified class name if it is not in the default package
	 *  ({@link io.github.ai4ci.abm.behaviour}).
	 */
	String getDefaultBehaviourModelName();

	/**
	 * Sets the initial policy model to be used by the simulation. 
	 * This defines the default policy response to the evolving outbreak.
	 * The models are defined as enums implementing the {@link PolicyModel} 
	 * interface and can be extended to include custom models. The
	 * initial policy state is the first value of the enum.
	 * <p>Population-level impact (PolicyModel)
	 * @return The simple name of the enum class implementing the default 
	 *  PolicyModel (e.g., "ReactiveLockdown")
	 *  or the fully qualified class name if it is not in the default package
	 *  ({@link io.github.ai4ci.abm.policy}).
	 */
	String getDefaultPolicyModelName();

	

	/**
	 * Adjustment of different parameters as a function of age or other 
	 * demographics. In general this specifies either single valued functions
	 * or probabilistic adjustments to parameters based on demographic characteristics.
	 * These adjustments are applied to the baseline parameters defined in this 
	 * configuration to create demographic-specific parameters for individuals 
	 * in the population. For example, if the case fatality rate is 1% in the 
	 * baseline configuration and the demographic adjustment specifies an odds 
	 * ratio of 2 for a certain age group, then the adjusted case fatality rate 
	 * for that age group would be approximately 2%. 
	 * 
	 * <p>The exact method of adjustment depends on the type of parameter:
	 * probabilistic parameters will be adjusted as an odds ratio and other real
	 * values quantities will be simply scaled by the adjustment value. Adjustments
	 * are matched to their parameters by name and this is performed by 
	 * {@link io.github.ai4ci.util.ReflectionUtils#modify(X, PartialDemographicAdjustment, PersonDemographic)}.
	 * 
	 * <p>At the moment this is only defined for age but it could be extended to other demographics
	 * such as location, socioeconomic status, or comorbidities
	 * however multivariate distributions would be needed to capture interactions between demographics.
	 * 
	 * <p>Individual-level impact (PersonBaseline)
	 * 
	 * @return PartialDemographicAdjustment instance defining how parameters are adjusted based on demographics.
	 * 
	 * @see DemographicAdjustment.Execution
	 * @see DemographicAdjustment.Phenomenological
	 * @see DemographicAdjustment.Markov
	 */
	PartialDemographicAdjustment getDemographicAdjustment();

	// @SuppressWarnings("unchecked")
	/**
	 * Uses #getDefaultBehaviourModelName() to load the default behaviour model
	 * class and return the first value of the enum as the initial behaviour model
	 * for agents in the simulation. This allows for dynamic loading of behaviour
	 * models based on configuration.
	 * 
	 * @return Default BehaviourModel instance corresponding to the name specified
	 *         in the configuration.
	 */
	@JsonIgnore
	@Value.Lazy
	default BehaviourModel getDefaultBehaviourModel() {
		try {
			String name = getDefaultBehaviourModelName();
			if (!name.startsWith(BehaviourModel.class.getPackageName()))
				name = BehaviourModel.class.getPackageName() + "." + name;
			Class<?> clz = Class.forName(name);
			Method m = clz.getDeclaredMethod("values");
			BehaviourModel[] x = (BehaviourModel[]) m.invoke(null);
			return x[0];
		} catch (Exception e) {
			throw new RuntimeException("Could not find behaviour model for: " + getDefaultBehaviourModelName(), e);
		}
	}

	/**
	 * Uses #getDefaultPolicyModelName() to load the default policy model
	 * class and return the first value of the enum as the initial policy model
	 * for the simulation. This allows for dynamic loading of policy
	 * models based on configuration.
	 * 
	 * @return Default PolicyModel instance corresponding to the name specified
	 *         in the configuration.
	 */
	@JsonIgnore
	@Value.Lazy
	default PolicyModel getDefaultPolicyModel() {
		try {
			String name = getDefaultPolicyModelName();
			if (!name.startsWith(PolicyModel.class.getPackageName()))
				name = PolicyModel.class.getPackageName() + "." + name;
			Class<?> clz = Class.forName(name);
			Method m = clz.getDeclaredMethod("values");
			PolicyModel[] x = (PolicyModel[]) m.invoke(null);
			return x[0];
		} catch (Exception e) {
			throw new RuntimeException("Could not find policy model for: " + getDefaultPolicyModelName(), e);
		}
	}

	/**
	 * For a configuration gets a set of viral load profiles from an average set of
	 * participants. The profiles are generated from the in-host model defined in
	 * the configuration and are used by the {@link io.github.ai4ci.abm.Calibration
	 * calibration} to calibrate the transmission parameter to achieve the desired
	 * R0.
	 * 
	 * <p>
	 * Each profile is a time series of viral load values that represent the
	 * infectiousness of an individual over time after infection. The profiles are
	 * typically generated for a fixed number of time steps (e.g., 100) and can be
	 * used to determine the temporal dynamics of transmission in the model.
	 * 
	 * <p>
	 * Because they undergo further transformation during calibration to produce the
	 * final transmission profile, they cannot be averaged at this stage (unlike {@link #getSeverityProfile()}) 
	 * but they are generated from the same in-host model and parameters defined in the
	 * configuration to ensure consistency with the disease progression assumptions.
	 * 
	 * 
	 * @return 2D array of viral load profiles (time steps x number of profiles)
	 */
	@JsonIgnore
	@Value.Lazy
	default double[][] getViralLoadProfile() {
		return InHostConfiguration.getViralLoadProfile(this, 100, 100);
	}

	/**
	 * Generates severity profiles for disease progression from an average set of
	 * participants. The profiles are generated from the in-host model defined in
	 * the configuration and are used by the {@link io.github.ai4ci.abm.Calibration
	 * calibration} to calibrate the severity thresholds for asymptomatic,
	 * hospitalised, and fatal cases based on the desired case hospitalisation and
	 * fatality rates.
	 * 
	 * @return DelayDistribution of symptom severity patterns
	 * @see InHostConfiguration#getSeverityProfile(ExecutionConfiguration, int, int)
	 */
	@JsonIgnore
	@Value.Lazy
	default DelayDistribution getSeverityProfile() {
		return InHostConfiguration.getSeverityProfile(this, 100, 100);
	}

}
