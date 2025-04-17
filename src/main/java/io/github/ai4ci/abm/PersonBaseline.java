package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.abm.StateMachine.BehaviourState;
import io.github.ai4ci.config.InHostConfiguration;

@Value.Immutable
public interface PersonBaseline extends Serializable {

	//Integer getAge();
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
	Double getSelfIsolationDepth();
	Double getAppUseProbability();
	
	
	
}
