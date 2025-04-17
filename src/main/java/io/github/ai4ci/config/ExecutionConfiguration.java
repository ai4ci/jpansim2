package io.github.ai4ci.config;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.abm.BehaviourModel;
import io.github.ai4ci.abm.PolicyModel;
import io.github.ai4ci.abm.TestParameters;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.config.InHostConfiguration.PhenomenologicalModel;
import io.github.ai4ci.util.Data.Partial;
import io.github.ai4ci.util.DelayDistribution;
// import io.reactivex.rxjava3.annotations.Nullable;
import io.github.ai4ci.util.SimpleDistribution;


@Value.Immutable
@JsonSerialize(as = ImmutableExecutionConfiguration.class)
@JsonDeserialize(as = ImmutableExecutionConfiguration.class)
public interface ExecutionConfiguration extends Abstraction.Named, Abstraction.Replica, Serializable {

	@Partial @Value.Immutable 
	@JsonSerialize(as = PartialExecutionConfiguration.class)
	@JsonDeserialize(as = PartialExecutionConfiguration.class)
	public interface _PartialExecutionConfiguration extends ExecutionConfiguration, Abstraction.Modification {}
	
	public static ExecutionConfiguration DEFAULT = ImmutableExecutionConfiguration.builder()
			.setName("execution")
			.setRO(2.5)
			
			.setContactProbability( SimpleDistribution.unimodalBeta(0.5, 0.05))
			
			.setContactDetectedProbability( SimpleDistribution.unimodalBeta(0.9, 0.1))
			.setComplianceProbability( SimpleDistribution.unimodalBeta(0.99, 0.001))
			.setAppUseProbability( SimpleDistribution.unimodalBeta(0.97, 0.01))
			
//			.setRiskTrigger( Distribution.point(0.05) )
//			.setRiskTriggerRatio(1.1)
//			.setHighRiskMobilityModifier(0.95)
//			.setHighRiskTransmissibilityModifier(0.95)
			
			.setInHostConfiguration( PhenomenologicalModel.DEFAULT )
			
//			.setLockdownStartPrevalenceTrigger(0.05)
//			.setLockdownMinDuration(14)
//			.setLockdownEndPrevalenceTrigger(0.01)
//			.setLockdownMobility(0.5)
//			.setLockdownTransmissibility(0.5)
			
//			.setScreeningPeriod( Distribution.gamma(7.0,1D) )
//			.setProbabilityScreened(0.01)
			
			.setDefaultBehaviourModelName( BehaviourModel.ReactiveTestAndIsolate.class.getSimpleName() )
			.setDefaultPolicyModelName( PolicyModel.ReactiveLockdown.class.getSimpleName() )
			
			.setSymptomSensitivity(SimpleDistribution.unimodalBeta(0.5, 0.01))
			.setSymptomSpecificity(SimpleDistribution.unimodalBeta(0.95, 0.01))
			
			.setInitialEstimateSymptomSensitivity(0.5)
			.setInitialEstimateSymptomSpecificity(0.95)
			
			.setInitialEstimateInfectionDuration(10.0)
			.setMaximumSocialContactReduction(SimpleDistribution.unimodalBeta(0.2, 0.01))
			.setAvailableTests(TestResult.defaultTypes())
			
			.setImportationProbability(0.001D)
			
			.build();
	 
	// must have no optionals.
	Double getRO();
	
	/**
	 * The proportion of a persons social network that they see each day
	 * in a fully mobile population. The mobility baseline is determined from
	 * the square root of this value, as the contact is the product of 2 peoples
	 * mobility.
	 * {@link AgentBaseline#contactRate}
	 */
	SimpleDistribution getContactProbability();
	SimpleDistribution getContactDetectedProbability();
	SimpleDistribution getComplianceProbability();
	SimpleDistribution getAppUseProbability();
	
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
	
	
	TestParameters.Group getAvailableTests();
	
	SimpleDistribution getSymptomSensitivity();
	SimpleDistribution getSymptomSpecificity();
	
	Double getInitialEstimateInfectionDuration();
	Double getInitialEstimateSymptomSensitivity();
	Double getInitialEstimateSymptomSpecificity();
	
	SimpleDistribution getMaximumSocialContactReduction();
	
	String getDefaultBehaviourModelName();
	String getDefaultPolicyModelName();
	
	
	/**
	 * This is work in progress. Importations are defined in the 
	 * PersonUpdaterFn.IMPORTATION_PROTOCOL and use this to define a random 
	 * exposure uniform across the population depending on individual mobility.
	 * @return
	 */
	Double getImportationProbability();
	
	
	//@SuppressWarnings("unchecked")
	@JsonIgnore
	@Value.Lazy default BehaviourModel getDefaultBehaviourModel() {
		try {
			String name = getDefaultBehaviourModelName();
			if (!name.startsWith(BehaviourModel.class.getCanonicalName())) 
				name = BehaviourModel.class.getCanonicalName() + "$" + name;
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
			if (!name.startsWith(PolicyModel.class.getCanonicalName())) 
				name = PolicyModel.class.getCanonicalName() + "$" + name;;
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
		return InHostConfiguration.getInfectivityProfile(this.getInHostConfiguration(), 100, 100);
	}
	

	
	
}
