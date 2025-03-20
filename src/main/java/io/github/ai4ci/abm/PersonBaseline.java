package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.StateMachine.BehaviourState;

@Value.Immutable
public interface PersonBaseline extends Serializable {

	//Integer getAge();
	/** 
	 * A probability of contact of another person in the social network.
	 * @return
	 */
	Double getMobilityBaseline();
	/**
	 * A probability of transmission to someone else given an infectious contact
	 * which is likely to depend on behaviour.
	 * @return
	 */
	Double getTransmissibilityBaseline();
	
	Double getComplianceBaseline();
	
//	Double getLowRiskMobilityIncreaseTrigger();
//	Double getHighRiskMobilityDecreaseTrigger();
//	Double getHighRiskMobilityModifier();
	
	@Value.Default default Integer getTargetCellCount() {return 10000;}
	@Value.Default default Double getImmuneTargetRatio() {return 1.0;}
	@Value.Default default Double getImmuneActivationRate() {return 1.0;}
	@Value.Default default Double getImmuneWaningRate() {return 1.0/200;}
	@Value.Default default Double getInfectionCarrierProbability() {return 0D;}
	@Value.Default default Double getTargetRecoveryRate() {return 1.0/7;}
	
//	@Value.Default default TestingStrategy getDefaultTestingStrategy() {return TestingStrategy.NONE;}
//	@Value.Default default BehaviourStrategy getDefaultBehaviourStrategy() {return BehaviourStrategy.NONE;}
	
	@Value.Default default Double getSymptomSpecificity() {return 0.95;}
	@Value.Default default Double getSymptomSensitivity() {return 0.9;}
	BehaviourState getDefaultBehaviourState();
	double getSelfIsolationDepth();
	double getAppUseProbability();
	
}
