package io.github.ai4ci.abm;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.BehaviourModel.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.mechanics.StateMachine;
import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.config.InHostConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.StochasticModel;
import io.github.ai4ci.util.DelayDistribution;
import io.github.ai4ci.util.Sampler;

public class TestViralLoadModel {

	TestUtils config = ImmutableTestUtils.builder().setExecutionTweak(
				PartialExecutionConfiguration.builder()
					.setInHostConfiguration(StochasticModel.DEFAULT)
					.build()
			).build();
			
	
	@Test
	void testViralLoad() {
		Sampler rng = Sampler.getSampler();
		InHostStochasticState state2 = (InHostStochasticState) InHostModelState.test(
			(StochasticModel) config.getOutbreak().getExecutionConfiguration().getInHostConfiguration(),
			rng);
		// TODO: this test does not work as outside of simulation there is no 
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
		
		DelayDistribution dd = InHostConfiguration.getInfectivityProfile(
				config.getOutbreak().getExecutionConfiguration().getInHostConfiguration(),
				100, 100);
		System.out.println(dd);
		
		double[] vl = InHostConfiguration.getViralLoadProfile(
				config.getOutbreak().getExecutionConfiguration().getInHostConfiguration(),
				100, 100);
		System.out.println(Arrays.toString(vl));
		
	}
	
	@Test
	void testStateModel() {
		Person p = config.getPerson();
		StateMachine sm = p.getStateMachine();
		sm.forceTo(ReactiveTestAndIsolate.REACTIVE_PCR);
		 
		Updater u = new Updater();
		
		for (int i =0; i<=10; i++ ) {
			System.out.println(sm.getState().toString()+" symptoms:"+p.getCurrentState().isSymptomatic()+" compliant:"+p.getCurrentState().isCompliant());
			System.out.println(p.getCurrentState().getInHostModel());
			u.update(p.getOutbreak());
			
		}
		
		
	}
	
	@Test
	void testClone() {
		
		Outbreak copy = SerializationUtils.clone(config.getOutbreak());
		System.out.println(copy);
		
	}
	
	@Test
	void applyNoise() {
		Sampler rng = Sampler.getSampler();
		System.out.println(IntStream.range(0, 100000).map(i -> 
			TestParameters.applyNoise(0, 0.8, 0.95, rng) > 1 ? 0:1
			).average());
		
		System.out.println(IntStream.range(0, 100000).map(i -> 
			TestParameters.applyNoise(1, 0.8, 0.95, rng) < 1 ? 0 : 1
		).average());
	}
	
}
