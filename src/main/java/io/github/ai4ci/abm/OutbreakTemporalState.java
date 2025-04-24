package io.github.ai4ci.abm;

import io.github.ai4ci.abm.mechanics.Abstraction.TemporalState;

public interface OutbreakTemporalState extends TemporalState<Outbreak> {
	
	Long getInfectedCount();
	
}