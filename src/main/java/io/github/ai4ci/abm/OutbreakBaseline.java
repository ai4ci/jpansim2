package io.github.ai4ci.abm;

import java.io.Serializable;

import org.immutables.value.Value;

import io.github.ai4ci.abm.StateMachine.PolicyState;

@Value.Immutable
public interface OutbreakBaseline extends Serializable {

	// Simulation baseline viral factors
	@Value.Default default Double getBaselineViralInfectionRate() {return 1D;}
	@Value.Default default Double getBaselineViralReplicationRate() {return 4D;}

	/**
	 * Given the parameters for the virus, what is the unit of virions. This is
	 * a calibration parameter, and defines the limit of what is considered 
	 * disease. It should be the lowest value that is transmissible and defines
	 * a patient as "infected" it does not follow that an uninfected patient
	 * does not have a small in-host viral load.
	 * @return the cutoff
	 */
	@Value.Default default Integer getVirionsDiseaseCutoff() {return 1000;}
	
	/**
	 * The proportion of target cells that are required to be inoperational
	 * (i.e. in infected or removed state) before the patient exhibits 
	 * symptoms. 
	 * @return a percentage cutoff, larger numbers means fewer symptoms.
	 */
	@Value.Default default Double getTargetSymptomsCutoff() {return 0.2D;}
	
	PolicyState getDefaultPolicyState();
	
	
}
