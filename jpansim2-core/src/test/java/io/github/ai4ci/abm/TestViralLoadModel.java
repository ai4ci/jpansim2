package io.github.ai4ci.abm;

import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.abm.inhost.InHostStochasticState;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.functions.DelayDistribution;
import io.github.ai4ci.util.Sampler;

public class TestViralLoadModel {

	TestUtils config = TestUtils.defaultWithExecution(
		exec -> exec.setInHostConfiguration(StochasticModel.DEFAULT)
	);
			
	
	@Test
	void testViralLoad() {
		Sampler rng = Sampler.getSampler();
		InHostStochasticState state2 = (InHostStochasticState) InHostModelState.test(
			(StochasticModel) config.getOutbreak().getExecutionConfiguration().getInHostConfiguration(),
			config.getOutbreak().getExecutionConfiguration(),
			rng);
		// this test does not work as outside of simulation there is no 
		// viral exposure history.
		
		for (int i =0; i<=10; i++ ) {
			System.out.println(state2.toString());
			state2 = state2.update(
					rng,
					i == 1 ? 1D : 0D, // viralExposure
							0 // immunisation
					);
		}
	}
	
	@Test
	void testInfectivityProfile() {
		
		DelayDistribution dd = 	config.getOutbreak().getBaseline().getInfectivityProfile();
		System.out.println(dd);
		
		double[][] vl = InHostConfiguration.getViralLoadProfile(
				config.getOutbreak().getExecutionConfiguration(),
				100, 100);
		System.out.println(Arrays.toString(vl));
		
		DelayDistribution  v2 = InHostConfiguration.getSeverityProfile(
				config.getOutbreak().getExecutionConfiguration(),
				100, 100);
		System.out.println(v2);
	}
	
	
	
	@Test
	void testClone() {
		
		Outbreak copy = SerializationUtils.clone(config.getOutbreak());
		System.out.println(copy);
		
	}
	
	
	
}
