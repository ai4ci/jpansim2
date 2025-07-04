package io.github.ai4ci.abm;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.inhost.ExposureModel;
import io.github.ai4ci.abm.inhost.InHostModelState;
import io.github.ai4ci.abm.inhost.InHostPhenomenologicalState;
import io.github.ai4ci.abm.inhost.ExposureModel.BiPhasicLogistic;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.util.Sampler;
import io.github.ai4ci.config.PartialExecutionConfiguration;

class TestPhenomenologicalModel {

	TestUtils config = ImmutableTestUtils.builder().setExecutionTweak(
			PartialExecutionConfiguration.builder()
				.setInHostConfiguration(PhenomenologicalModel.DEFAULT)
				.build()
		).build();
	
	@Test
	void testLogistic() {
		BiPhasicLogistic viralLoadModel = BiPhasicLogistic.calibrateViralLoad(
				5D, //onset time
				7D, //peak time 
				15D, //duration, 
				0.1D, //threshold 
				0.5D // peak
		);
		
		ExposureModel tmp = ExposureModel.createExposure(
				10, //exposure
				0,  // immunity
				viralLoadModel, 
				5 //time)
		);
		
		IntStream.range(0, 40)
			.mapToDouble(d -> 
				tmp.getExposureViralLoad(d, viralLoadModel))
			.forEach(System.out::println);
	}
	
	@Test
	void testViralLoad() {
		Sampler rng = Sampler.getSampler();
		PhenomenologicalModel tmp = (PhenomenologicalModel) config.getOutbreak().getExecutionConfiguration().getInHostConfiguration();
		InHostPhenomenologicalState state2 = (InHostPhenomenologicalState) InHostModelState.test(tmp, config.getOutbreak().getExecutionConfiguration(), rng);
		
		for (int i =0; i<=10; i++ ) {
			System.out.println(state2.toString());
			state2 = state2.update(
					rng,
					i == 1 ? 1D : 0D, // viralExposure
					0 // immunisation
					);
		}
	}

}
