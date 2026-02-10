package io.github.ai4ci.abm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.behaviour.NonCompliant;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.setup.BarabasiAlbertConfiguration;
import io.github.ai4ci.functions.DelayDistribution;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.functions.ImmutableDelayDistribution;

class TestCalibration {
	
	Outbreak out = TestUtils.mockOutbreak();
	
	@Test
	void testR0() {
		
		Outbreak out2 = TestUtils.defaultWithAdjustments(
				setup -> setup.setNetwork(BarabasiAlbertConfiguration.DEFAULT
								.withNetworkSize(500)
								.withNetworkDegree(100)),
				exec -> exec
						.setR0(3.0)
						.setDefaultPolicyModelName(NoControl.class.getSimpleName())
						.setDefaultBehaviourModelName(NonCompliant.class.getSimpleName())
				).getOutbreak(); 
		
		System.out.println("average social network degree: "+out2.getSocialNetwork().size()*2 / out2.getPopulationSize());
		
		System.out.println("contacts per day: "+Calibration.contactsPerPersonPerDay(out2));
		System.out.println("max R0: "+Calibration.maxR0(out2));
		System.out.println("average contact degree: "+Calibration.averageContactDegree(out2));
		System.out.println("percolation: "+Calibration.percolationThreshold(out2));
		System.out.println("inv percolation: "+1.0/Calibration.percolationThreshold(out2));
		
		
		
		
		double param1 = Calibration.inferViralLoadTransmissionParameter(out2, 1.0);
		double param15 = Calibration.inferViralLoadTransmissionParameter(out2, 1.5);
		
		double param15q = Calibration.inferViralLoadTransmissionParameterQuick(out2, 1.5);
		
		System.out.println(param15+"="+param15q);
		
		double[][] prof = out2.getExecutionConfiguration().getViralLoadProfile();
		System.out.println(	InHostConfiguration.getInfectivityProfile(prof, param15));
		
		
		DelayDistribution pTrans_0 = InHostConfiguration.getInfectivityProfile(prof, param1);
		System.out.println("R0=1, param="+param1+", trans="+Arrays.toString(pTrans_0.getProfile()));
		
		
		
		DelayDistribution pTrans = InHostConfiguration.getInfectivityProfile(prof, param15); 
		System.out.println("R0=1.5, param="+param15+", trans="+Arrays.toString(pTrans.getProfile()));
		
		System.out.println(
				(1-Arrays.stream(pTrans.getProfile()).map(d -> 1-d).reduce((d1, d2)->d1*d2).getAsDouble())
					*Calibration.contactsPerPersonPerDay(out2)
			);
		
		assertThrows(RuntimeException.class, () -> {
			double param1000 = Calibration.inferViralLoadTransmissionParameter(out2, 1000);
			System.out.println(param1000);
		});
		
		double param30 = Calibration.inferViralLoadTransmissionParameter(out2, prof, Calibration.maxR0(out2)-0.01);
		DelayDistribution pTrans_3 = InHostConfiguration.getInfectivityProfile(prof, param30);
		System.out.println("R0="+(Calibration.maxR0(out2)-0.01)+", param="+param30+", trans="+Arrays.toString(pTrans_3.getProfile()));
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
		double param30 = Calibration.inferViralLoadTransmissionParameter(out, 2.0);
		ImmutableDelayDistribution dd = (ImmutableDelayDistribution) InHostConfiguration.getInfectivityProfile(
				out.getExecutionConfiguration(), param30, 100, 100);
		dd = dd.withPAffected(0.4);
		
		Arrays.stream(
		InHostConfiguration.getViralLoadProfile(
				out.getExecutionConfiguration(),
				100, 100)
		).forEach(System.out::println);
		
		
		
		System.out.println(Conversions.oddsRatio(0.25, 0.5));
		System.out.println(Conversions.oddsRatio(0.5, 0.25));
		System.out.println(Conversions.oddsRatio(0.1, 0.4));
	}
		
	@Test
	void testExponential() {
		
		System.out.println(1-Math.pow(1-Conversions.probabilityFromQuantile(10, 0.95),10));
		System.out.println(Conversions.probabilityFromQuantile(10, 0.95));
		
	}
	
	@Test
	void testProb() {
		System.out.println(Conversions.probabilityFromPeriod(10));
	}
}
