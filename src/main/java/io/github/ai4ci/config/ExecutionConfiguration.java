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
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.ShallowList;
// import io.reactivex.rxjava3.annotations.Nullable;
import io.github.ai4ci.util.SimpleDistribution;


@Value.Immutable
@JsonSerialize(as = ImmutableExecutionConfiguration.class)
@JsonDeserialize(as = ImmutableExecutionConfiguration.class)
public interface ExecutionConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable, DemographicAdjustment.Execution<Distribution, Double> {
 
	default ExecutionConfiguration withReplicate(Integer i) {return this;}
	default ExecutionConfiguration withName(String name) {return this;}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	@JsonSerialize(as = PartialExecutionConfiguration.class)
	@JsonDeserialize(as = PartialExecutionConfiguration.class)
	public interface _PartialExecutionConfiguration extends ExecutionConfiguration, Abstraction.Modification<ExecutionConfiguration>{
		default _PartialExecutionConfiguration self() {return this;}
	}
	
	public static ImmutableExecutionConfiguration DEFAULT = ImmutableExecutionConfiguration.builder()
			.setName("execution")
			.setRO(1.75)
			.setAsymptomaticFraction(0.5)
			.setCaseHospitalisationRate(0.05)
			.setCaseFatalityRate(0.01)
			
			.setContactProbability( SimpleDistribution.unimodalBeta(0.5, 0.25))
			.setAppUseProbability( SimpleDistribution.unimodalBeta(0.97, 0.1))
			.setContactDetectedProbability( 0.9 )
			
			.setComplianceProbability( SimpleDistribution.unimodalBeta(0.99, 0.1))
			
			
//			.setRiskTrigger( Distribution.point(0.05) )
//			.setRiskTriggerRatio(1.1)
//			.setHighRiskMobilityModifier(0.95)
//			.setHighRiskTransmissibilityModifier(0.95)
			
			.setInHostConfiguration( PhenomenologicalModel.DEFAULT )
			
			.setLockdownStartPrevalenceTrigger(0.05)
//			.setLockdownMinDuration(14)
			.setLockdownReleasePrevalenceTrigger(0.01)
//			.setLockdownMobility(0.5)
//			.setLockdownTransmissibility(0.5)
			
//			.setScreeningPeriod( Distribution.gamma(7.0,1D) )
//			.setProbabilityScreened(0.01)
			
			.setDefaultBehaviourModelName( ReactiveTestAndIsolate.class.getSimpleName() )
			.setDefaultPolicyModelName( ReactiveLockdown.class.getSimpleName() )
			
			.setSymptomSensitivity(SimpleDistribution.unimodalBeta(0.5, 0.1))
			.setSymptomSpecificity(SimpleDistribution.unimodalBeta(0.95, 0.1))
			
			.setInitialEstimateSymptomSensitivity(0.5)
			.setInitialEstimateSymptomSpecificity(0.95)
			
			.setInitialEstimateIncubationPeriod(4.0)
			.setInitialEstimateInfectionDuration(10.0)
			
			.setMaximumSocialContactReduction(SimpleDistribution.unimodalBeta(0.5, 0.1))
			.setAvailableTests(TestResult.defaultTypes())
			
			.setImportationProbability(0.001D)
			.setDemographicAdjustment(DemographicAdjustment.EMPTY)
			
			.setComplianceDeteriorationRate(0.02)
			.setComplianceImprovementRate(0.01)
			.setOrganicRateOfMobilityChange(1.0/4)
			
			.setSmartAppRiskTrigger(0.05)
			
			.build();
	 
	// must have no optionals.
	Double getRO();
	Double getAsymptomaticFraction();
	Double getCaseHospitalisationRate();
	Double getCaseFatalityRate();
	
	@JsonIgnore
	default Double getInfectionCaseRate() {
		return 1-getAsymptomaticFraction();
	}
	
	@JsonIgnore
	default Double getInfectionHospitalisationRate() {
		return getInfectionCaseRate()*this.getCaseHospitalisationRate();
	}
	
	@JsonIgnore
	default Double getInfectionFatalityRate() {
		return getInfectionCaseRate()*this.getCaseFatalityRate();
	}
	
	/**
	 * The proportion of a persons social network that they see each day
	 * in a fully mobile population. The mobility baseline is determined from
	 * the square root of this value, as the contact is the product of 2 peoples
	 * mobility.
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
	
	Distribution getMaximumSocialContactReduction();
	
	String getDefaultBehaviourModelName();
	String getDefaultPolicyModelName();
	
	/**
	 * The absolute rate of decrease in compliance in situations where the
	 * person is asked to self isolate 
	 */
	Double getComplianceDeteriorationRate();
	
	/**
	 * The absolute rate of decrease in compliance in situations where it is 
	 * deemed to improve
	 */
	Double getComplianceImprovementRate();
	
	/**
	 * Speed at which people change behaviour in response to symptoms. I.e.
	 * the lambda of an exponential decay towards the maximum social contact 
	 * reduction or back to baseline 
	 * @return
	 */
	Double getOrganicRateOfMobilityChange();
	
	/**
	 * At what simulation wide prevalence estimate is lockdown released. 
	 * Estimated using test positives.
	 */
	Double getLockdownReleasePrevalenceTrigger();
	
	/**
	 * At what simulation wide prevalence estimate is lockdown initiated.
	 * Estimated using test positives.
	 */
	Double getLockdownStartPrevalenceTrigger();
	
	/**
	 * This is work in progress. Importations are defined in the 
	 * PersonUpdaterFn.IMPORTATION_PROTOCOL and use this to define a random 
	 * exposure uniform across the population depending on individual mobility.
	 */
	Double getImportationProbability();
	
	/**
	 * The local risk profile (assessed through tests, symptoms, contacts etc)
	 * which is sufficient to trigger a request for testing and reactive 
	 * self isolation 
	 */
	Double getSmartAppRiskTrigger();
	
	PartialDemographicAdjustment getDemographicAdjustment();
	
	//@SuppressWarnings("unchecked")
	@JsonIgnore
	@Value.Lazy default BehaviourModel getDefaultBehaviourModel() {
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
	
	//@SuppressWarnings("unchecked")
	@JsonIgnore
	@Value.Lazy default PolicyModel getDefaultPolicyModel() {
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

	@JsonIgnore
	@Value.Lazy default DelayDistribution getInfectivityProfile() {
		return InHostConfiguration.getInfectivityProfile(this.getInHostConfiguration(), this, 100, 100);
	}
	
	@JsonIgnore
	@Value.Lazy default DelayDistribution getSeverityProfile() {
		return InHostConfiguration.getSeverityProfile(this.getInHostConfiguration(), this, 100, 100);
	}
	
	


	

	
	
}
