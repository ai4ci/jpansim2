package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

/**
 * Represents the demographic information of a {@link Person} in the simulation.
 * This immutable value object holds static attributes such as age and spatial location,
 * and provides methods to access these properties. It is designed to be {@link Serializable}.
 */
@Value.Immutable
public interface PersonDemographic extends Serializable {
	
	/**
	 * Creates a stub {@link ImmutablePersonDemographic} instance associated with a given {@link Person}.
	 * This factory method is useful for initializing demographic data for a new person with default values.
	 *
	 * @param person The {@link Person} entity for which to create the demographic stub.
	 * @return A new {@link ImmutablePersonDemographic} instance.
	 */
	public static ImmutablePersonDemographic stub(Person person) {
		return ImmutablePersonDemographic.builder()
				.setEntity(person)
				.build();
	}

	/**
	 * Returns the {@link Person} entity to which these demographic details belong.
	 *
	 * @return The associated {@link Person} entity.
	 */
	Person getEntity();
	
	/**
	 * Returns the age of the person.
	 * Defaults to {@code Double.NaN} if not explicitly set.
	 *
	 * @return The age of the person.
	 */
	@Value.Default default double getAge() {return Double.NaN;}
	
	/**
	 * Returns the X-coordinate of the person's location.
	 * This is derived from the Hilbert curve coordinates.
	 *
	 * @return The X-coordinate of the person's location.
	 */
	@Value.Default default double getLocationX() {
		return this.getHilbertCoordinates()[0];
	}
	
	/**
	 * Returns the Y-coordinate of the person's location.
	 * This is derived from the Hilbert curve coordinates.
	 *
	 * @return The Y-coordinate of the person's location.
	 */
	@Value.Default default double getLocationY() {
		return this.getHilbertCoordinates()[1];
	}
	
	/**
	 * Retrieves the Hilbert curve coordinates for the associated person.
	 * These coordinates are used to determine the spatial location (X, Y) of the person.
	 *
	 * @return An array of two doubles representing the Hilbert X and Y coordinates.
	 */
	private double[] getHilbertCoordinates() {
		return this.getEntity().getOutbreak().getSetupConfiguration().getHilbertCoords(this.getEntity().getId());
	}

	// TODO: this is too slow. The calibrator has done this already for all 
	// agents and we need to reuse. see Calibration.networkDegreePerPerson()
	// We also want to rationalise this so that the networks can be easily 
	// switched into CSR format for eigenvalue estimatation.
	
	
//	@Value.Lazy default double getExpectedContactDegree() {
//		int id = this.getEntity().getId();
//		Outbreak outbreak = this.getEntity().getOutbreak();
//		return outbreak.getSocialNetwork().stream()
//				.filter(r -> r.getPeopleIds().contains(id))
//				.mapToDouble(r -> {
//					Person person1 = r.getSource(outbreak);
//					Person person2 = r.getTarget(outbreak);
//					return r.contactProbability(
//							person1.getBaseline().getMobilityBaseline(),
//							person2.getBaseline().getMobilityBaseline());
//				})
//				.sum();
//	}
	
}