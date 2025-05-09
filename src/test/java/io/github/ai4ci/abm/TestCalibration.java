package io.github.ai4ci.abm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.config.InHostConfiguration;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.DelayDistribution;

class TestCalibration {

	Outbreak out = TestUtils.mockOutbreak();
	
	@Test
	void testR0() {
		
		System.out.println(Calibration.contactsPerPersonPerDay(out));
		
		double[] pTrans_0 = Calibration.inferTransmissionProbability(out, 1.0);
		System.out.println("R0=1, trans="+Arrays.toString(pTrans_0));
		
		
		
		double pTrans[] = Calibration.inferTransmissionProbability(out, 1.5);
		System.out.println("R0=1.5, trans="+Arrays.toString(pTrans));
		
		System.out.println(Arrays.stream(pTrans).sum()*Calibration.contactsPerPersonPerDay(out));
		
		assertThrows(RuntimeException.class, () -> {
			double[] pTrans_2 = Calibration.inferTransmissionProbability(out, 1000);
			System.out.println(pTrans_2);
		});
	}

	@Test
	void testIFREtcetera() {
		System.out.println(Calibration.inferSeverityCutoff(out,0.1));
		System.out.println(Calibration.inferSeverityCutoff(out,0.5));
		System.out.println(Calibration.inferSeverityCutoff(out,0.9));
		System.out.println(Calibration.inferSeverityCutoff(out,0.99));
		System.out.println(Calibration.inferSeverityCutoff(out,0.999));
	}
	
	@Test
	void testConversions() {
		
		DelayDistribution dd = InHostConfiguration.getInfectivityProfile(
				out.getExecutionConfiguration().getInHostConfiguration(),
				100, 100);
		dd = dd.withPAffected(0.4);
		System.out.println(dd.totalHazard(0.2)+"\n");
		
		Arrays.stream(
		InHostConfiguration.getViralLoadProfile(
				out.getExecutionConfiguration().getInHostConfiguration(),
				100, 100)
		).forEach(System.out::println);
		
		
		
		System.out.println(Conversions.oddsRatio(0.25, 0.5));
		System.out.println(Conversions.oddsRatio(0.5, 0.25));
		System.out.println(Conversions.oddsRatio(0.1, 0.4));
	}
	
	@Test
	void testProb() {
		System.out.println(Conversions.probabilityFromPeriod(10));
	}
}
