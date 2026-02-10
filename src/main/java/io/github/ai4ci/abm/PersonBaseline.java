package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.flow.mechanics.State;

@Value.Immutable
public interface PersonBaseline extends Serializable {

	/**
	 * A probability of contact of another person in the social network.
	 * 
	 * @return the baseline probability of contact for this person, which is
	 *         modified by their behaviour and the behaviours of others in the
	 *         network
	 */
	Double getMobilityBaseline();
	
	/**
	 * How likely is this person to comply with instructions compared to another
	 * 
	 * @return the baseline compliance probability for this person, which is
	 *         modified by their behaviour and the behaviours of others in the
	 *         network
	 */
	Double getComplianceBaseline();
	
//	Double getLowRiskMobilityIncreaseTrigger();
//	Double getHighRiskMobilityDecreaseTrigger();
//	Double getHighRiskMobilityModifier();
	
	/**
	 * How likely is a this person to have false positive symptoms when not infected
	 * 
	 * @return the baseline probability of false positive symptoms for this person,
	 *         which is modified by their behaviour and the behaviours of others in
	 *         the network
	 */
	Double getSymptomSpecificity();
	
	/**
	 * How likely is a this person to have no symptoms when infected
	 * 
	 * @return the baseline probability of false negative symptoms for this person,
	 *         which is modified by their behaviour and the behaviours of others in
	 *         the network
	 */
	Double getSymptomSensitivity();
	
	/**
	 * The default behaviour state for this person. This is the state that they will
	 * revert to when not influenced by their own behaviour or the behaviours of
	 * others in the network. It is also the state that they will start in at the
	 * beginning of a simulation.
	 * 
	 * @return the default behaviour state for this person, which is modified by
	 *         their behaviour and the behaviours of others in the network
	 */
	State.BehaviourState getDefaultBehaviourState();
	
	/**
	 * The minimum mobility modifier that a person will experience. This is an odds
	 * ratio that is applied to their baseline probability
	 * 
	 * @return the minimum mobility modifier for this person, which is applied to
	 *         their baseline probability and modified by their behaviour and the
	 *         behaviours of others in the network
	 */
	Double getSelfIsolationDepth();
	
	/**
	 * How likely is this person to use an app on any given day.
	 * 
	 * @return the baseline probability of app use for this person, which is
	 *         modified by their behaviour and the behaviours of others in the
	 *         network
	 */
	Double getAppUseProbability();
	
	
	
}
