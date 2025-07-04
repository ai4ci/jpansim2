package io.github.ai4ci.config;

import io.github.ai4ci.flow.StateExporter.ExportSelector;
import io.github.ai4ci.output.ImmutableContactCSV;
import io.github.ai4ci.output.ImmutableDebugParametersCSV;
import io.github.ai4ci.output.ImmutableInfectivityProfileCSV;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakFinalStateCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonDemographicsCSV;
import io.github.ai4ci.output.ImmutablePersonStateCSV;

public enum Exporters {

	SUMMARY (ExportSelector.of(ImmutableOutbreakCSV.class)),
	INTERNAL_STATE (ExportSelector.of(ImmutablePersonStateCSV.class)),
	HISTORICAL_TESTS (ExportSelector.of(ImmutableOutbreakHistoryCSV.class)),
	CONTACT_NETWORK (ExportSelector.of(ImmutableContactCSV.class)),
	INFECTIVITY_PROFILE (ExportSelector.of(ImmutableInfectivityProfileCSV.class)),
	DEMOGRAPHICS (ExportSelector.of(ImmutablePersonDemographicsCSV.class)),
	DEBUG_PARAMETERS (ExportSelector.of(ImmutableDebugParametersCSV.class)),
	FINAL_STATE (ExportSelector.of(ImmutableOutbreakFinalStateCSV.class))
	;
	
	ExportSelector<?> selector;
	Exporters(ExportSelector<?> selector) {
		this.selector = selector;
	}
	ExportSelector<?> getSelector() {return selector;}
	
}
