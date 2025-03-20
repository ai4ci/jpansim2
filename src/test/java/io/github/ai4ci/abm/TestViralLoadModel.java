package io.github.ai4ci.abm;

import java.util.stream.IntStream;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.BehaviourModel.ReactiveTestAndIsolate;
import io.github.ai4ci.util.Distribution;
import io.github.ai4ci.util.Sampler;

public class TestViralLoadModel {

	@Test
	void testViralLoad() {
		ModifiablePerson p = TestUtils.mockPerson();
		
		ViralLoadState state2 = ViralLoadState.initialise(p);
		
		Sampler rng = Sampler.getSampler();
		for (int i =0; i<=10; i++ ) {
			System.out.println(state2.toString());
			state2 = state2.update(rng);
		}
	}
	
	@Test
	void testStateModel() {
		Person p = TestUtils.mockPerson();
		StateMachine sm = p.getStateMachine();
		sm.forceTo(ReactiveTestAndIsolate.REACTIVE_PCR);
		 
		Updater u = new Updater();
		Sampler rng = Sampler.getSampler();
		for (int i =0; i<=10; i++ ) {
			System.out.println(sm.getState().toString()+" symptoms:"+p.getCurrentState().isSymptomatic()+" compliant:"+p.getCurrentState().isCompliant());
			System.out.println(p.getCurrentState().getViralLoad());
			u.update(p.getOutbreak());
			
		}
		
		Outbreak copy = SerializationUtils.clone(p.getOutbreak());
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
