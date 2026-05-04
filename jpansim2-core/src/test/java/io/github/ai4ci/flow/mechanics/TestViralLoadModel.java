package io.github.ai4ci.flow.mechanics;

import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.TestUtils;
import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.abm.inhost.InHostStochasticState;
import io.github.ai4ci.config.inhost.InHostConfiguration;
import io.github.ai4ci.config.inhost.StochasticModel;
import io.github.ai4ci.util.Sampler;

public class TestViralLoadModel {

	TestUtils config = TestUtils.defaultWithExecution(
		exec -> exec.setInHostConfiguration(StochasticModel.DEFAULT)
	);

	@Test
	void testViralLoad() {
		var rng = Sampler.getSampler();
		var state2 = (InHostStochasticState) InHostModelState
			.test(
				(StochasticModel) this.config.getOutbreak()
					.getExecutionConfiguration()
					.getInHostConfiguration(),
				this.config.getOutbreak()
					.getExecutionConfiguration(),
				rng
			);
		// this test does not work as outside of simulation there is no
		// viral exposure history.

		for (var i = 0; i <= 10; i++) {
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

		var dd = this.config.getOutbreak()
			.getBaseline()
			.getInfectivityProfile();
		System.out.println(dd);

		var vl = this.config.getOutbreak()
			.getExecutionConfiguration()
			.getViralLoadProfile();
		System.out.println(Arrays.toString(vl));

		var v2 = InHostConfiguration.getSeverityProfile(
			this.config.getOutbreak()
				.getExecutionConfiguration(),
			100,
			100
		);
		System.out.println(v2);
	}

	@Test
	void testClone() {

		Outbreak copy = SerializationUtils.clone(this.config.getOutbreak());
		System.out.println(copy);

	}

}
