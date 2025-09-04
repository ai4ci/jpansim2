package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.StateMachine.BehaviourState;

@Value.Immutable
public interface PersonBaseline extends Serializable {

	/** 
	 * A probability of contact of another person in the social network.
	 */
	Double getMobilityBaseline();
	
	/**
	 * How likely is this person to comply with instructions compared to another
	 */
	Double getComplianceBaseline();
	
//	Double getLowRiskMobilityIncreaseTrigger();
//	Double getHighRiskMobilityDecreaseTrigger();
//	Double getHighRiskMobilityModifier();
	
	/**
	 * How likely is a this person to have false positive symptoms when not 
	 * infected
	 */
	Double getSymptomSpecificity();
	
	/**
	 * How likely is a this person to have no symptoms when infected
	 */
	Double getSymptomSensitivity();
	
	BehaviourState getDefaultBehaviourState();
	
	/**
	 * The minimum mobility modifier that a person will experience. This is an
	 * odds ratio that is applied to their baseline probability
	 * @return
	 */
	Double getSelfIsolationDepth();
	
	/**
	 * How likely is this person to use an app on any given day.
	 */
	Double getAppUseProbability();
	
	
	
}
