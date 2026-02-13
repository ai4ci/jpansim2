package io.github.ai4ci.abm;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.config.setup.AgeStratifiedDemography;

public class TestAgeStrat {

	
	@Test
	void testBuild() {
		
		TestUtils tu = TestUtils.defaultWithSetup(
			setup -> setup.setDemographics(AgeStratifiedDemography.DEFAULT)
		);
				
		Outbreak out = 	tu.getOutbreak();
		
		//out.getPeople()
		System.out.println("average social network degree: "+out.getAverageNetworkDegree());
		
		System.out.println("contacts per day: "+Calibration.contactsPerPersonPerDay(out));
		System.out.println("max R0: "+Calibration.maxR0(out));
		System.out.println("average contact degree: "+Calibration.averageContactDegree(out));
		System.out.println("percolation: "+Calibration.percolationThreshold(out));
		System.out.println("inv percolation: "+1.0/Calibration.percolationThreshold(out));
	}
}
