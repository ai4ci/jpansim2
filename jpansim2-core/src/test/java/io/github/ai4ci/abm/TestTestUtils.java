package io.github.ai4ci.abm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.util.Conversions;

public class TestTestUtils {
	
	@Test
	void testTestUtilsBuildsAndRuns() {
		
		// fully connected network with 10 nodes.
		var tu = TestUtils.defaultTest().withPatientZero();
		
		// All patients contact each other every day
		assertEquals(
				9.0,
				Calibration.contactsPerPersonPerDay(tu.getOutbreak()));
		
		// The maximum possible R0 is 9 if all people are infected
		assertEquals(
				9.0,
				Calibration.maxR0(tu.getOutbreak())
				);
		
		// Transmission is a function of viral load. Calibration is done with an
		assertEquals(
			0.0,
			tu.getOutbreak().getBaseline().getTransmissibilityBaseline(0)
		);
		
		IntStream.range(0, 10)
			.mapToDouble(i ->  tu.getOutbreak().getBaseline().getTransmissibilityBaseline((double) i))
			.forEach(d -> assertTrue(d<=1 && d>=0));
		
		double infDur = TestUtils.MINIMAL_IN_HOST.getInfectiousDuration().sample();
		assertTrue(
			Math.abs(1.0/Conversions.rateFromQuantile(infDur, 0.95) - 
			tu.getOutbreak().getBaseline().getInfectivityProfile().mean()) < 0.2
		);
		
		tu.stream(20)
			.map(mo -> mo.getCurrentState())
			.map(os -> os.getCumulativeInfections())
			.forEach(System.out::println);
	}
	
}
