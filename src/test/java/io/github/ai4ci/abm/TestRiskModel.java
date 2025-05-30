package io.github.ai4ci.abm;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.behaviour.ReactiveTestAndIsolate;
import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;

public class TestRiskModel {

	TestUtils config = ImmutableTestUtils.builder().setExecutionTweak(
			PartialExecutionConfiguration.builder()
				.setInHostConfiguration(MarkovStateModel.DEFAULT)
				.build()
		).build();
		

@Test
void testRiskModel() {
	Outbreak o = config.getOutbreak();
	o.getPeople().stream().forEach(p ->
		p.getStateMachine().forceTo(ReactiveTestAndIsolate.REACTIVE_PCR)
	);
	 
	Updater u = new Updater();
//	u.withPersonProcessor(
//			pp -> pp.getCurrentState().getTime() == 2, 
//			(builder,person,rng) -> builder.setImportationExposure(1.0)
//	);
	
	for (int i =0; i<=200; i++ ) {
//		o.getPeople().stream().filter(p -> p.getCurrentState().isSymptomatic()).forEach(p -> {
//			//if (!p.getCurrentState().isReportedSymptomatic())
//				//System.out.println(p.getCurrentState());
//			System.out.println(p.getCurrentState().getRiskModel().getProbabilityInfectiousToday());
//		});
		u.update(o);
		double avEstLocalPrev = o.getPeople().stream().mapToDouble(p -> p.getCurrentState().getPresumedLocalPrevalence()).average().orElse(0);
		double avTrueLocalPrev = o.getPeople().stream().mapToDouble(p -> p.getCurrentState().getTrueLocalPrevalence()).average().orElse(0);
		double globalPrevalence = o.getCurrentState().getPrevalence();
		double geomAvTrueLocalLikelihood = globalPrevalence == 0 ? 1 : o.getPeople().stream().mapToDouble(p -> {
			double localPrevalence = p.getCurrentState().getTrueLocalPrevalence();
			double localOddsRatio = localPrevalence/globalPrevalence;
			return localOddsRatio;
		}).average().orElse(1);
		double geomAvOdds = Math.exp(o.getPeople().stream().mapToDouble(p -> p.getCurrentState().getRiskModel().getLogOddsInfectiousToday()).average().orElse(0));
		
		System.out.println(
				"est prev:"+String.format("%1.3f%%", avEstLocalPrev*100)+
				"\tobs prev:"+String.format("%1.3f%%", avTrueLocalPrev*100)+
				"\test odds:"+String.format("%1.3f", geomAvOdds)+
				"\tobs odds:"+String.format("%1.3f", geomAvTrueLocalLikelihood)
		);
	}
}
	
}
