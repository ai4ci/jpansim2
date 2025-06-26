package io.github.ai4ci.config;

import io.github.ai4ci.flow.ExportSelector;
import io.github.ai4ci.output.ImmutableContactCSV;
import io.github.ai4ci.output.ImmutableDebugParametersCSV;
import io.github.ai4ci.output.ImmutableInfectivityProfileCSV;
import io.github.ai4ci.output.ImmutableLineListCSV;
import io.github.ai4ci.output.ImmutableOutbreakBehaviourCountCSV;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakContactCountCSV;
import io.github.ai4ci.output.ImmutableOutbreakFinalStateCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonDemographicsCSV;

public enum Exporters {

	SUMMARY (ExportSelector.of(ImmutableOutbreakCSV.class)),
	LINELIST (ExportSelector.of(ImmutableLineListCSV.class)),
	HISTORICAL_TESTS (ExportSelector.of(ImmutableOutbreakHistoryCSV.class)),
	CONTACT_NETWORK (ExportSelector.of(ImmutableContactCSV.class)),
	INFECTIVITY_PROFILE (ExportSelector.of(ImmutableInfectivityProfileCSV.class)),
	DEMOGRAPHICS (ExportSelector.of(ImmutablePersonDemographicsCSV.class)),
	DEBUG_PARAMETERS (ExportSelector.of(ImmutableDebugParametersCSV.class)),
	FINAL_STATE (ExportSelector.of(ImmutableOutbreakFinalStateCSV.class)),
	BEHAVIOUR (ExportSelector.of(ImmutableOutbreakBehaviourCountCSV.class)),
	CONTACT_COUNTS (ExportSelector.of(ImmutableOutbreakContactCountCSV.class))
	;
	
	ExportSelector<?> selector;
	Exporters(ExportSelector<?> selector) {
		this.selector = selector;
	}
	ExportSelector<?> getSelector() {return selector;}
	
}
