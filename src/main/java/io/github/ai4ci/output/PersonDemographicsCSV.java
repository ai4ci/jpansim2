package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.DuckDBWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Person demographic records exported at simulation start.
 *
 * <p>Main purpose: provide per‑agent demographic and baseline attributes such
 * as age, mobility baseline and location. Records are produced at the START
 * stage and are written per agent to a DuckDB database ({@code demog.duckdb}).
 *
 * <p>Downstream uses: used for descriptive statistics, stratified analyses and
 * as a join table for other per‑agent outputs such as line lists.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(stage = Stage.START,value = "demog.duckdb",size = 16, selector = PersonDemographicsCSV.Selector.class, writer=DuckDBWriter.class)
public interface PersonDemographicsCSV extends CommonCSV.Execution {
	
	static class Selector implements Export.Selector {
		@Override
		public Stream<PersonDemographicsCSV> apply(Outbreak o) {
			return o.getPeople().stream().map(CSVMapper.INSTANCE::toDemog);
		}
	}
	
	/**
	 * Unique identifier for the person.
	 *
	 * @return the id used to join records for the same individual across
	 * exported tables
	 */
	int getId();
	/**
	 * Age of the person.
	 *
	 * @return the person's age in years as a double to permit fractional ages
	 */
	double getAge();
	/**
	 * Baseline mobility value for the person.
	 *
	 * @return the baseline mobility scalar used as the starting point for
	 * mobility adjustments
	 */
	double getMobilityBaseline();
	/**
	 * Baseline probability the person uses a contact tracing app.
	 *
	 * @return the app use probability in range 0–1
	 */
	double getAppUseProbability();
	/**
	 * Baseline compliance level for the person.
	 *
	 * @return the compliance baseline scalar used to model adherence to
	 * interventions
	 */
	double getComplianceBaseline();
	/**
	 * X coordinate of the person's spatial location normalised to [0,1].
	 *
	 * @return the normalised X location used for spatial analysis and joining
	 * to raster or spatial models
	 */
	double getLocationX();
	/**
	 * Y coordinate of the person's spatial location normalised to [0,1].
	 *
	 * @return the normalised Y location used for spatial analysis and joining
	 * to raster or spatial models
	 */
	double getLocationY();
	
	// double getExpectedContactDegree();
	
}