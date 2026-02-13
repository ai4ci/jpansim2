package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.flow.mechanics.State;

/**
 * Baseline parameters for a person in the simulation. These are parameters that
 * are fixed for the person and do not change over the course of the simulation.
 * They are set once during configuration but their effects may be modified by
 * the person's behaviour through the {@link PersonState}.
 *
 * <p>
 * These represent the core features of this individual that are unchanged and
 * to which the person will return during the simulation.
 *
 * @author Rob Challen
 */
@Value.Immutable
public interface PersonBaseline extends Serializable {

	/**
	 * How likely is this person to use an app on any given day.
	 *
	 * @return the baseline probability of app use for this person, which is
	 *         modified by their behaviour and the behaviours of others in the
	 *         network
	 */
	Double getAppUseProbability();

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
	 * The default behaviour state for this person. This is the state that they
	 * will revert to when not influenced by their own behaviour or the
	 * behaviours of others in the network. It is also the state that they will
	 * start in at the beginning of a simulation.
	 *
	 * @return the default behaviour state for this person, which is modified by
	 *         their behaviour and the behaviours of others in the network
	 */
	State.BehaviourState getDefaultBehaviourState();

	/**
	 * A probability of contact of another person in the social network.
	 *
	 * @return the baseline probability of contact for this person, which is
	 *         modified by their behaviour and the behaviours of others in the
	 *         network
	 */
	Double getMobilityBaseline();

	/**
	 * The minimum mobility modifier that a person will experience. This is an
	 * odds ratio that is applied to their baseline probability
	 *
	 * @return the minimum mobility modifier for this person, which is applied to
	 *         their baseline probability and modified by their behaviour and the
	 *         behaviours of others in the network
	 */
	Double getSelfIsolationDepth();

	/**
	 * How likely is a this person to have no symptoms when infected
	 *
	 * @return the baseline probability of false negative symptoms for this
	 *         person, which is modified by their behaviour and the behaviours of
	 *         others in the network
	 */
	Double getSymptomSensitivity();

	/**
	 * How likely is a this person to have false positive symptoms when not
	 * infected
	 *
	 * @return the baseline probability of false positive symptoms for this
	 *         person, which is modified by their behaviour and the behaviours of
	 *         others in the network
	 */
	Double getSymptomSpecificity();

}
