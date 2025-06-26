package io.github.ai4ci.config;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.abm.behaviour.BehaviourModel;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
import io.github.ai4ci.abm.policy.PolicyModel;
import io.github.ai4ci.abm.policy.ReactiveLockdown;
import io.github.ai4ci.abm.policy.Trigger;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.riskmodel.Kernels;
import io.github.ai4ci.config.riskmodel.RiskKernelConfiguration;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.ShallowList;
// import io.reactivex.rxjava3.annotations.Nullable;

import static io.github.ai4ci.config.riskmodel.RiskKernelConfiguration.*;
import static io.github.ai4ci.util.SimpleDistribution.*;

@Value.Immutable
@JsonSerialize(as = ImmutableExecutionConfiguration.class)
@JsonDeserialize(as = ImmutableExecutionConfiguration.class)
public interface ExecutionConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable,
		DemographicAdjustment.Execution<Distribution, Double> {

	default ExecutionConfiguration withReplicate(Integer i) {
		return this;
	}

	default ExecutionConfiguration withName(String name) {
		return this;
	}

	@Partial
	@Value.Immutable
	@SuppressWarnings("immutables")
	@JsonSerialize(as = PartialExecutionConfiguration.class)
	@JsonDeserialize(as = PartialExecutionConfiguration.class)
	public interface _PartialExecutionConfiguration
			extends ExecutionConfiguration, Abstraction.Modification<ExecutionConfiguration> {
		default _PartialExecutionConfiguration self() {
			return this;
		}
	}

	public static ImmutableExecutionConfiguration DEFAULT = ImmutableExecutionConfiguration.builder()
			.setName("execution").setR0(1.75).setAsymptomaticFraction(0.5).setCaseHospitalisationRate(0.05)
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

			.setRiskModelSymptomKernel(from(Kernels.DEFAULT_SYMPTOM_ONSET_KERNEL))
			.setRiskModelContactsKernel(from(Kernels.DEFAULT_CONTACT_KERNEL))
			.setRiskModelTestKernel(from(Kernels.DEFAULT_TEST_SAMPLE_KERNEL))

			.setMaximumSocialContactReduction(unimodalBeta(0.25, 0.1)).setAvailableTests(TestResult.defaultTypes())

			.setImportationProbability(0D).setDemographicAdjustment(DemographicAdjustment.EMPTY)

			.setComplianceDeteriorationRate(0.02).setComplianceImprovementRate(0.01)
			.setOrganicRateOfMobilityChange(1.0 / 4)

			.setSmartAppRiskTrigger(0.05)

			.build();

	// must have no optionals but everything must be nullable for partially
	// populated config. I.e. no primitives in the config package.
	Double getR0();

	Double getAsymptomaticFraction();

	Double getCaseHospitalisationRate();

	Double getCaseFatalityRate();

	Double getInitialScreeningProbability();

	@JsonIgnore
	default Double getInfectionCaseRate() {
		return 1 - getAsymptomaticFraction();
	}

	@JsonIgnore
	default Double getInfectionHospitalisationRate() {
		return getInfectionCaseRate() * this.getCaseHospitalisationRate();
	}

	@JsonIgnore
	default Double getInfectionFatalityRate() {
		return getInfectionCaseRate() * this.getCaseFatalityRate();
	}

	/**
	 * The proportion of a persons social network that they see each day in a fully
	 * mobile population. The mobility baseline is determined from this but as the
	 * contact is the product of 2 peoples mobility the average probability of a
	 * contact will be the square of the central value. In the case of a
	 * Watts-Strogatz or similar social network setting this to be a uniform
	 * distribution is recommended to get an approximate scale free contact
	 * distribution.
	 * {@link io.github.ai4ci.abm.PersonBaseline#getMobilityBaseline()}
	 */
	Distribution getContactProbability();

	Double getContactDetectedProbability();

	Distribution getComplianceProbability();

	Distribution getAppUseProbability();

	// Distribution getRiskTrigger();

//	Double getRiskTriggerRatio();
//	Double getHighRiskMobilityModifier();
//	Double getHighRiskTransmissibilityModifier();

//	Double getLockdownStartPrevalenceTrigger();
//	Integer getLockdownMinDuration();
//	Double getLockdownEndPrevalenceTrigger();
//	Double getLockdownMobility();
//	Double getLockdownTransmissibility();

//	Distribution getScreeningPeriod();
//	Double getProbabilityScreened();

	InHostConfiguration getInHostConfiguration();

	ShallowList<TestParameters> getAvailableTests();

	Distribution getSymptomSensitivity();

	Distribution getSymptomSpecificity();

	Double getInitialEstimateInfectionDuration();

	Double getInitialEstimateIncubationPeriod();

	Double getInitialEstimateSymptomSensitivity();

	Double getInitialEstimateSymptomSpecificity();

	RiskKernelConfiguration getRiskModelSymptomKernel();

	RiskKernelConfiguration getRiskModelTestKernel();

	RiskKernelConfiguration getRiskModelContactsKernel();

	Distribution getMaximumSocialContactReduction();

	String getDefaultBehaviourModelName();

	String getDefaultPolicyModelName();

	Trigger.Value getLockdownTriggerValue();

	/**
	 * The absolute rate of decrease in compliance in situations where the person is
	 * asked to self isolate
	 */
	Double getComplianceDeteriorationRate();

	/**
	 * The absolute rate of decrease in compliance in situations where it is deemed
	 * to improve
	 */
	Double getComplianceImprovementRate();

	/**
	 * Speed at which people change behaviour in response to symptoms. I.e. the
	 * lambda of an exponential decay towards the maximum social contact reduction
	 * or back to baseline
	 * 
	 * @return
	 */
	Double getOrganicRateOfMobilityChange();

	/**
	 * At what simulation wide prevalence estimate is lockdown released. Estimated
	 * using test positives.
	 */
	Double getLockdownReleaseTrigger();

	/**
	 * At what simulation wide prevalence estimate is lockdown initiated. Estimated
	 * using test positives.
	 */
	Double getLockdownStartTrigger();

	/**
	 * This is work in progress. Importations are defined in the
	 * PersonUpdaterFn.IMPORTATION_PROTOCOL and use this to define a random exposure
	 * uniform across the population depending on individual mobility.
	 */
	Double getImportationProbability();

	/**
	 * The local risk profile (assessed through tests, symptoms, contacts etc) which
	 * is sufficient to trigger a request for testing and reactive self isolation
	 */
	Double getSmartAppRiskTrigger();

	/**
	 * Adjustment of different parameters as a function of age. In general
	 * probabilistic parameters will be adjusted as an odds ratio and other real
	 * values quantities will be simply scaled by the adjustment value. Adjustments
	 * are matched by name.
	 * 
	 * @see DemographicAdjustment.Execution
	 * @see DemographicAdjustment.Phenomenological
	 * @see DemographicAdjustment.Markov
	 */
	PartialDemographicAdjustment getDemographicAdjustment();

	// @SuppressWarnings("unchecked")
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

	// @SuppressWarnings("unchecked")
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
	 * For a configuration gets an average viral load profile. This is not a
	 * probability. A linear function of this defines the probability of
	 * transmission but this needs to be calibrated to get a population R0. The
	 * connection between viral load and infectivity profile is actually a hazard
	 * function.
	 */
	@JsonIgnore
	@Value.Lazy
	default double[][] getViralLoadProfile() {
		return InHostConfiguration.getViralLoadProfile(this, 100, 100);
	}

	@JsonIgnore
	@Value.Lazy
	default DelayDistribution getSeverityProfile() {
		return InHostConfiguration.getSeverityProfile(this, 100, 100);
	}

}
