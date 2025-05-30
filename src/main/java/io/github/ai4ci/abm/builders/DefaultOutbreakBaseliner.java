package io.github.ai4ci.abm.builders;

import io.github.ai4ci.abm.Calibration;
import io.github.ai4ci.abm.ImmutableOutbreakBaseline;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Sampler;

public interface DefaultOutbreakBaseliner {

	
	default ImmutableOutbreakBaseline baselineOutbreak(ImmutableOutbreakBaseline.Builder builder, Outbreak outbreak,
			Sampler sampler) {
		ExecutionConfiguration configuration = outbreak.getExecutionConfiguration();
	
		//N.B. happens after people are baselined.., I think
		
		builder
			.setDefaultPolicyState( configuration.getDefaultPolicyModel() )
			.setViralLoadTransmissibilityProbabilityFactor( 
					Calibration.inferViralLoadTransmissionProbabilityFactor(outbreak, configuration.getRO()) 
			)
			.setExpectedContactsPerPersonPerDay(
					Calibration.contactsPerPersonPerDay(outbreak)
			)
			.setSeveritySymptomsCutoff(
					configuration.getInHostConfiguration().getSeveritySymptomsCutoff(outbreak, configuration)
			)
			.setSeverityHospitalisationCutoff(
					configuration.getInHostConfiguration().getSeverityHospitalisationCutoff(outbreak, configuration)
			)
			.setSeverityDeathCutoff(
					configuration.getInHostConfiguration().getSeverityFatalityCutoff(outbreak, configuration)
			)
			.setInfectiveDuration(
				(int) configuration.getInfectivityProfile().getQuantile(0.95)	
			)
			.setSymptomDuration(
				(int) configuration.getSeverityProfile().getQuantile(0.95)
			)
			;
		outbreak.getStateMachine().init( configuration.getDefaultPolicyModel() );
		return builder.build();
	}
	
}
