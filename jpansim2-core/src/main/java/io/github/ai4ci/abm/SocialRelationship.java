package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import it.unimi.dsi.fastutil.ints.IntIntImmutableSortedPair;

/**
 * Represents a weighted social relationship (an edge) between two individuals
 * in the simulation's social network. This immutable value object stores the
 * strength of the relationship and the identifiers of the two people involved.
 * It provides methods to calculate the probability of contact based on
 * individual mobility and to retrieve the associated {@link Person} objects.
 * This class is {@link Serializable} for persistence.
 */
@Value.Immutable
public interface SocialRelationship extends Serializable {
	/**
	 * Calculates the probability of a contact occurring between the two
	 * individuals in this relationship, adjusted by their respective mobility
	 * factors. The calculation is
	 * {@code getRelationshipStrength() * adjMobility1 * adjMobility2}.
	 *
	 * @param adjMobility1 The adjusted mobility factor of the first participant.
	 * @param adjMobility2 The adjusted mobility factor of the second
	 *                     participant.
	 * @return The probability of contact, a double value.
	 */
	public default double contactProbability(
			double adjMobility1, double adjMobility2
	) {
		return this.getRelationshipStrength() * adjMobility1 * adjMobility2;
	}

	/**
	 * Returns an immutable sorted pair of integer IDs representing the two
	 * individuals involved in this social relationship. The IDs are sorted to
	 * ensure that the relationship is uniquely identified regardless of the
	 * order of participants.
	 *
	 * @return An {@link IntIntImmutableSortedPair} containing the IDs of the two
	 *         people.
	 */
	IntIntImmutableSortedPair getPeopleIds();

	/**
	 * The significance or strength of the relationship, expressed as a
	 * probability or quantile. This value represents the likelihood that a
	 * contact will occur between two maximally mobile participants. A value
	 * close to 1 indicates a strong, close relationship with a high probability
	 * of contact, while a value near 0 signifies an improbable or weak
	 * connection.
	 *
	 * @return The strength of the social relationship, a double between 0 and 1.
	 */
	double getRelationshipStrength();

	/**
	 * Retrieves the {@link Person} object corresponding to the source (first)
	 * individual in this relationship. The source is determined by the first ID
	 * in the {@link #getPeopleIds() sorted pair}.
	 *
	 * @param outbreak The {@link Outbreak} instance from which to retrieve the
	 *                 person.
	 * @return The {@link Person} object for the source individual.
	 * @throws java.util.NoSuchElementException if the person with the source ID
	 *                                          is not found in the outbreak.
	 */
	default public Person getSource(Outbreak outbreak) {
		return outbreak.getPersonById(this.getPeopleIds().firstInt())
				.orElseThrow();
	}

	/**
	 * Retrieves the {@link Person} object corresponding to the target (second)
	 * individual in this relationship. The target is determined by the second ID
	 * in the {@link #getPeopleIds() sorted pair}.
	 *
	 * @param outbreak The {@link Outbreak} instance from which to retrieve the
	 *                 person.
	 * @return The {@link Person} object for the target individual.
	 * @throws java.util.NoSuchElementException if the person with the target ID
	 *                                          is not found in the outbreak.
	 */
	default public Person getTarget(Outbreak outbreak) {
		return outbreak.getPersonById(this.getPeopleIds().secondInt())
				.orElseThrow();
	}

}