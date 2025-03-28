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
	
	Integer getTargetCellCount();
	Double getImmuneTargetRatio();
	Double getImmuneActivationRate();
	Double getImmuneWaningRate();
	Double getInfectionCarrierProbability();
	Double getTargetRecoveryRate();
	
	Double getSymptomSpecificity();
	Double getSymptomSensitivity();
	
	BehaviourState getDefaultBehaviourState();
	Double getSelfIsolationDepth();
	Double getAppUseProbability();
	
}
