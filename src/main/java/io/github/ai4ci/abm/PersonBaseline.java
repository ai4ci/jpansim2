package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;

@Value.Immutable
public interface PersonBaseline extends Serializable {

	/** 
	 * A probability of contact of another person in the social network.
	 * @return
	 */
	Double getMobilityBaseline();
	/**
	 * Am odds ratio on the probability of transmission to someone else given 
	 * an infectious contact that reflects intrinsic host specific factors that
	 * don't vary with time. Inherent susceptibility due to e.g. age.
	 * @return
	 */
	Double getTransmissibilityModifier();
	
	Double getComplianceBaseline();
	
//	Double getLowRiskMobilityIncreaseTrigger();
//	Double getHighRiskMobilityDecreaseTrigger();
//	Double getHighRiskMobilityModifier();
	
	Double getSymptomSpecificity();
	Double getSymptomSensitivity();
	
	BehaviourState getDefaultBehaviourState();
	
	/**
	 * The minimum mobility modifier that a person will experience. This is an
	 * odds ratio that is applied to their baseline probability
	 * @return
	 */
	Double getSelfIsolationDepth();
	Double getAppUseProbability();
	
	
	
}
