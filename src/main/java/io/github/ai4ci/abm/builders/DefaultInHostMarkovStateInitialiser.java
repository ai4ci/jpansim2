package io.github.ai4ci.abm.builders;

import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.inhost.InHostMarkovState;
import io.github.ai4ci.abm.inhost.InHostMarkovState.DiseaseState;
import io.github.ai4ci.abm.inhost.InHostMarkovState.SymptomState;
import io.github.ai4ci.abm.inhost.ImmutableInHostMarkovState;
import io.github.ai4ci.abm.inhost.ImmutableInHostMarkovStateMachine;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ReflectionUtils;
import io.github.ai4ci.util.Sampler;

public interface DefaultInHostMarkovStateInitialiser {

	default InHostMarkovState initialiseInHostModel(MarkovStateModel configuration, ExecutionConfiguration execConfig, Optional<PersonDemographic> person, Sampler rng, int time) {
		
	if (person.isPresent()) {
		configuration = ReflectionUtils.modify(
				configuration,
				execConfig.getDemographicAdjustment(),
				person.get()
		);
	}
		double infectiousDuration = configuration.getInfectiousDuration().sample(rng);
		
		//developing symptoms can only happen whilst infectious.
		//however infection duration i s a 95% quantile 
		//and on average people are only infectious for less time than this.
		double meanInfectiousDuration = 1/Conversions.rateFromQuantile(infectiousDuration, 0.95);
		
		double symptomDuration = configuration.getSymptomDuration().sample(rng);
		
		// likewise the mean symptomDuration is shorter than this:
		
		double meanSymptomDuration = 1/Conversions.rateFromQuantile(symptomDuration, 0.95);
		
		// A per day probability that symptoms have finished assuming duration is
		// based on a 95% probability of resolution.
		double dailyProbabilityResolutionSymptoms = Conversions.probabilityFromQuantile(symptomDuration, 0.95);
		double dailyProbabilityUnresolvedSymptoms = 1-dailyProbabilityResolutionSymptoms; 
		
		double dailyProbabilityFatalityGivenCase =  Conversions.probabilityFromQuantile(meanSymptomDuration,
			execConfig.getCaseFatalityRate()
		);
		
		double dailyProbabilityHospitalisationGivenCase =  Conversions.probabilityFromQuantile(meanSymptomDuration,
				execConfig.getCaseHospitalisationRate()
			);
		
		
		return ImmutableInHostMarkovState.builder()
				.setTime(time)
				.setConfig(configuration)
				.setInfectionCaseRate(execConfig.getInfectionCaseRate())
				.setInfectionHospitalisationRate(execConfig.getInfectionHospitalisationRate())
				.setInfectionFatalityRate(execConfig.getInfectionFatalityRate())
				.setDiseaseState(DiseaseState.SUSCEPTIBLE)
				.setSymptomState(SymptomState.ASYMPTOMATIC)
				.setMachine(
						ImmutableInHostMarkovStateMachine.builder()
							.setPExposedInfectious(
								Conversions.probabilityFromPeriod(configuration.getIncubationPeriod().sample(rng))	
							)
							.setPInfectiousImmune(
								Conversions.probabilityFromQuantile(infectiousDuration, 0.95)
							)
							.setPImmuneSusceptible(
								Conversions.probabilityFromHalfLife(configuration.getImmuneWaningHalfLife().sample(rng))
							)
							
							.setPAsymptomaticSymptomatic(
								// The total proportion of symptomatic cases is the result of the per day probability
								// aggregated over the infectious duration.
								Conversions.probabilityFromQuantile(meanInfectiousDuration, execConfig.getInfectionCaseRate())
							)
							.setPSymptomaticAsymptomatic(dailyProbabilityResolutionSymptoms)
							.setPSymptomaticDead(dailyProbabilityUnresolvedSymptoms*dailyProbabilityFatalityGivenCase)
							.setPSymptomaticHospitalised(
								(
									dailyProbabilityUnresolvedSymptoms + dailyProbabilityUnresolvedSymptoms*dailyProbabilityFatalityGivenCase
								) * dailyProbabilityHospitalisationGivenCase
							)
							
							.setPHospitalisedAsymptomatic(dailyProbabilityResolutionSymptoms)
							// TODO: review logic for fatality rate in Markov state in host model 
							.setPHospitalisedDead(dailyProbabilityUnresolvedSymptoms*dailyProbabilityFatalityGivenCase)
							
							.build()
				)
				.build();
		
	}
	
}
