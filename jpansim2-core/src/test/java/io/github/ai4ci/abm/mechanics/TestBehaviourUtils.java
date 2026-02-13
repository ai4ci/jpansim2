package io.github.ai4ci.abm.mechanics;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.Person;
import io.github.ai4ci.abm.TestUtils;
import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.flow.mechanics.StateMachine;
import io.github.ai4ci.flow.mechanics.StateUtils;
import io.github.ai4ci.flow.mechanics.Updater;

public class TestBehaviourUtils {

	
	@Test
	void testDecayTo() {
		double start = 0.2;
		double target = 0.8;
		double period = 4;
		for (int i = 0; i<100; i++) {
			System.out.println(start);
			start  = StateUtils.decayTo(start,target,1/period);
		}
		System.out.println(start);
	}
	
	TestUtils config = TestUtils.defaultWithExecution(
			exec -> exec
				.setInHostConfiguration(StochasticModel.DEFAULT)
		);
	
	@Test
	void testStateModel() {
		Person p = config.getPerson();
		StateMachine sm = p.getStateMachine();
		sm.forceTo(ReactiveTestAndIsolate.REACTIVE_PCR);
		 
		Updater u = new Updater();
		u.withPersonProcessor(
				pp -> pp.getId().equals(p.getId()) && pp.getCurrentState().getTime() == 2, 
				(builder,person,rng) -> builder.setImportationExposure(1.0))
		;
		
		for (int i =0; i<=10; i++ ) {
			System.out.println(sm.getState().toString()+" symptoms:"+p.getCurrentState().isSymptomatic()+" compliant:"+p.getCurrentState().isCompliant());
			System.out.println(p.getCurrentState().getInHostModel());
			u.update(p.getOutbreak());
			
		}
		
		
	}
}
