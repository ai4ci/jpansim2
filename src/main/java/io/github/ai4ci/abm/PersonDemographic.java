package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
public interface PersonDemographic extends Serializable {
	
	public static ImmutablePersonDemographic stub(Person person) {
		return ImmutablePersonDemographic.builder()
				.setEntity(person)
				.build();
	}

	Person getEntity();
	@Value.Default default double getAge() {return Double.NaN;}
	@Value.Default default double getLocationX() {
		return this.getHilbertCoordinates()[0];
	}
	@Value.Default default double getLocationY() {
		return this.getHilbertCoordinates()[1];
	}
	
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
