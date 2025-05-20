package io.github.ai4ci.abm.mechanics;

import org.junit.jupiter.api.Test;

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
}
