package io.github.ai4ci.config;

import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutableContactCSV;
import io.github.ai4ci.output.ImmutableInfectivityProfileCSV;
import io.github.ai4ci.output.ImmutableOutbreakCSV;
import io.github.ai4ci.output.ImmutableOutbreakHistoryCSV;
import io.github.ai4ci.output.ImmutablePersonDemographicsCSV;
import io.github.ai4ci.output.ImmutablePersonStateCSV;
import io.github.ai4ci.output.StateExporter.ExportSelector;

public enum Exporters {

	// TODO: this could be heavily rationalised unless there is a strong reason 
	// to configure this in such a complex way.
	SUMMARY (ExportSelector.ofOne(ImmutableOutbreakCSV.class, o -> CSVMapper.INSTANCE.toCSV(o.getCurrentState()))),
	INTERNAL_STATE (ExportSelector.ofMany(ImmutablePersonStateCSV.class, o -> o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV))),
	HISTORICAL_TESTS (ExportSelector.ofMany(ImmutableOutbreakHistoryCSV.class, o -> o.getHistory().stream().map(CSVMapper.INSTANCE::toCSV))),
	CONTACT_NETWORK (ExportSelector.ofMany(ImmutableContactCSV.class, o -> o.getPeople().stream().flatMap(CSVMapper.INSTANCE::toContacts))),
	INFECTIVITY_PROFILE (ExportSelector.ofMany(ImmutableInfectivityProfileCSV.class, o -> CSVMapper.INSTANCE.infectivityProfile(o))),
	DEMOGRAPHICS (ExportSelector.ofMany(ImmutablePersonDemographicsCSV.class, o -> o.getPeople().stream().map(CSVMapper.INSTANCE::toDemog)))
	;
	
	ExportSelector<?> selector;
	Exporters(ExportSelector<?> selector) {
		this.selector = selector;
	}
	ExportSelector<?> getSelector() {return selector;}
	
}
