package io.github.ai4ci.abm;

import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface OutbreakHistory extends OutbreakTemporalState {
	
	Long getInfectedCount();
	Long getTestPositives();
	Long getTestNegatives();
	Optional<OutbreakHistory> getPrevious();
	

}