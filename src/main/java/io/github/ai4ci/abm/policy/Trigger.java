package io.github.ai4ci.abm.policy;

import io.github.ai4ci.abm.OutbreakState;
import io.github.ai4ci.util.Binomial;

public interface Trigger {

	Binomial select(OutbreakState state);
	
	static enum Value implements Trigger {
		
		TEST_POSITIVITY {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getPresumedTestPositivity();
			}
		},
		
		SCREENING_TEST_POSITIVITY {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getScreeningTestPositivity();
			}
		},
		
		TEST_COUNT {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getPresumedTestPositivePrevalence();
			}
		},
		
		HOSPITAL_BURDEN {
			@Override
			public Binomial select(OutbreakState state) {
				return state.getHospitalisationRate();
			}
		}
		
	}
	
}
