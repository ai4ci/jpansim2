package io.github.ai4ci.abm;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.config.setup.AgeStratifiedDemography;
import io.github.ai4ci.config.setup.PartialSetupConfiguration;

public class TestAgeStrat {

	
	@Test
	void testBuild() {
		
		TestUtils tu = TestUtils.builder
				.setSetupTweak(
					PartialSetupConfiguration.builder().setDemographics(
						AgeStratifiedDemography.DEFAULT
					).build()
				)
//				.setExecutionTweak(
//					PartialExecutionConfiguration.builder().setDemographicAdjustment(
//						PartialDemographicAdjustment.builder().setMaximumSocialContactReduction(
//								SimpleFunction.class
//						)
//					)
				.build();
				
		Outbreak out = 	tu.getOutbreak();
		
		//out.getPeople()
		System.out.println("average social network degree: "+out.getSocialNetwork().size()*2 / out.getPopulationSize());
		
		System.out.println("contacts per day: "+Calibration.contactsPerPersonPerDay(out));
		System.out.println("max R0: "+Calibration.maxR0(out));
		System.out.println("average contact degree: "+Calibration.averageContactDegree(out));
		System.out.println("percolation: "+Calibration.percolationThreshold(out));
		System.out.println("inv percolation: "+1.0/Calibration.percolationThreshold(out));
	}
}
