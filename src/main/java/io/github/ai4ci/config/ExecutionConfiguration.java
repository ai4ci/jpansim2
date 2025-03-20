package io.github.ai4ci.config;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.abm.BehaviourModel;
import io.github.ai4ci.abm.PolicyModel;
import io.github.ai4ci.abm.TestParameters;
import io.github.ai4ci.abm.TestResult;
import io.github.ai4ci.util.Data.Partial;
// import io.reactivex.rxjava3.annotations.Nullable;
import io.github.ai4ci.util.Distribution;
import io.github.ai4ci.util.Sampler;


@Value.Immutable
public interface ExecutionConfiguration extends Abstraction.Named, Serializable {

	@Partial @Value.Modifiable 
	public interface _PartialExecutionConfiguration extends ExecutionConfiguration {}
	
	public static ExecutionConfiguration DEFAULT = ImmutableExecutionConfiguration.builder()
			.setName("default-execution")
			.setRO(2.0)
			
			.setContactProbability( Distribution.unimodalBeta(0.5, 0.25))
			.setConditionalTransmissionProbability( Distribution.point(0.25D) )
			.setContactDetectedProbability( Distribution.unimodalBeta(0.9, 0.1))
			.setComplianceProbability( Distribution.unimodalBeta(0.99, 0.001))
			.setAppUseProbability( Distribution.beta(0.97, 0.1))
			
//			.setRiskTrigger( Distribution.point(0.05) )
//			.setRiskTriggerRatio(1.1)
//			.setHighRiskMobilityModifier(0.95)
//			.setHighRiskTransmissibilityModifier(0.95)
			
			.setTargetCellCount(10000)
			.setImmuneTargetRatio( Distribution.logNorm(1D, 0.1))
			.setImmuneActivationRate( Distribution.logNorm(1D, 0.1))
			.setImmuneWaningRate( Distribution.logNorm(1D/150, 0.01))
			.setInfectionCarrierProbability( Distribution.point(0D))
			.setTargetRecoveryRate( Distribution.logNorm( 1D/7, 0.1) )
			
//			.setLockdownStartPrevalenceTrigger(0.05)
//			.setLockdownMinDuration(14)
//			.setLockdownEndPrevalenceTrigger(0.01)
//			.setLockdownMobility(0.5)
//			.setLockdownTransmissibility(0.5)
			
			.setScreeningPeriod( Distribution.gamma(7.0,1D) )
			.setProbabilityScreened(0.01)
			
			.setDefaultBehaviourModelName( BehaviourModel.ReactiveTestAndIsolate.class.getSimpleName() )
			.setDefaultPolicyModelName( PolicyModel.ReactiveLockdown.class.getSimpleName() )
			
			.setSymptomSensitivity(Distribution.unimodalBeta(0.5, 0.01))
			.setSymptomSpecificity(Distribution.unimodalBeta(0.99, 0.01))
			
			.setPresumedInfectionDuration(10.0)
			.setMaximumSocialContactReduction(Distribution.unimodalBeta(0.4, 0.01))
			.setAvailableTests(TestResult.defaultTypes())
			
			.setImportationProbability(0.001D)
			
			.build();
	 
	// must have no optionals.
	Double getRO();
	
	Distribution getContactProbability();
	Distribution getConditionalTransmissionProbability();
	Distribution getContactDetectedProbability();
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

	Distribution getScreeningPeriod();
	Double getProbabilityScreened();
	
	Integer getTargetCellCount();
	Distribution getImmuneTargetRatio();
	Distribution getImmuneActivationRate();
	Distribution getImmuneWaningRate();
	Distribution getInfectionCarrierProbability();
	Distribution getTargetRecoveryRate();
	
	List<TestParameters> getAvailableTests();
	
	Distribution getSymptomSensitivity();
	Distribution getSymptomSpecificity();
	
	Double getPresumedInfectionDuration();
	Distribution getMaximumSocialContactReduction();
	
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

	

	

	
	
}
