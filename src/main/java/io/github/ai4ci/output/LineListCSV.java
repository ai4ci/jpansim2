package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.DuckDBWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Represents a line list CSV record for epidemic simulation output.
 * This interface defines the structure of individual-level data exported from
 * an outbreak simulation, including person identifiers, health status,
 * behavioural metrics, and epidemiological parameters.
 * 
 * <p>Records are exported during the UPDATE stage and are written per agent to
 * a DuckDB database ({@code linelist.duckdb}).
 * 
 * <p>Downstream uses: case line lists are typically consumed by analysis
 * notebooks, case investigation pipelines and for validation of individual
 * level dynamics against expected epidemic behaviour.
 * 
 * @author Rob Challen
 */
@Value.Immutable
@Export(stage = Stage.UPDATE, value = "linelist.duckdb",size = 64*64, selector = LineListCSV.Selector.class, writer=DuckDBWriter.class)
public interface LineListCSV extends CommonCSV.State {

	/**
	 * Selector class that implements Export.Selector to provide a stream of LineListCSV records
	 * from an Outbreak simulation for database export.
	 */
	static class Selector implements Export.Selector {
		@Override
		public Stream<LineListCSV> apply(Outbreak o) {
			return o.getPeople().stream().map(p -> p.getCurrentState()).map(CSVMapper.INSTANCE::toCSV);
		}
	}
	
	/**
	 * Unique identifier for the person.
	 *
	 * @return the person identifier used across exported tables to join records
	 */
	int getPersonId();
	/**
	 * Behavioural label for the person at the time of export.
	 *
	 * @return a short string naming the active behaviour model or behaviour
	 * class assigned to this person
	 */
	String getBehaviour();
	/**
	 * Whether the person is considered infectious at this time step.
	 *
	 * @return true if the person's state implies infectiousness at the exported
	 * time step, false otherwise
	 */
	boolean isInfectious(); 
	/**
	 * Whether the person is symptomatic at this time step.
	 *
	 * @return true if symptoms are present for the person at the exported time
	 * step; used for symptomatic surveillance and case counting
	 */
	boolean isSymptomatic();
	/**
	 * Whether the person requires hospitalisation at this time step.
	 *
	 * @return true if the person's clinical state indicates hospital admission
	 * is required at the time of export
	 */
	boolean isRequiringHospitalisation();
	/**
	 * Whether the person has died by this time step.
	 *
	 * @return true if the person is recorded as deceased in the current state
	 */
	boolean isDead();
	/**
	 * Whether this record marks an incident hospitalisation event.
	 *
	 * @return true if this is the first time the person is recorded as
	 * requiring hospitalisation
	 */
	boolean isIncidentHospitalisation();
	/**
	 * Whether this record marks an incident infection event.
	 *
	 * @return true if this is the first time the person is recorded as infected
	 */
	boolean isIncidentInfection();
	
	/**
	 * Normalised severity score for the person.
	 *
	 * @return normalised severity on a 0–1 scale where values nearer 1 indicate
	 * greater clinical severity
	 */
	double getNormalisedSeverity();
	/**
	 * Normalised viral load estimate for the person.
	 *
	 * @return normalised viral load on a 0–1 scale used as a proxy for
	 * infectiousness in analytic pipelines
	 */
	double getNormalisedViralLoad();
	/**
	 * Immune activity estimate for the person.
	 *
	 * @return a scalar representing the strength of immune response used by the
	 * in‑host model; interpretation depends on in‑host model type
	 */
	double getImmuneActivity();
	/**
	 * Contact exposure measure for the person.
	 *
	 * @return exposure as a scalar indicating recent contact‑related exposure
	 * risk for the person
	 */
	double getContactExposure();
	/**
	 * Presumed local prevalence as estimated by the agent.
	 *
	 * @return the agent's estimate of local prevalence used to inform behaviour
	 * models and local risk perception
	 */
	double getPresumedLocalPrevalence();
	/**
	 * True local prevalence measured by the model at the person's location.
	 *
	 * @return the ground truth local prevalence computed by the simulation
	 */
	double getTrueLocalPrevalence();
	/**
	 * Probability the person is infectious today as computed by the risk model.
	 *
	 * @return probability value in the range 0–1 representing the risk model's
	 * assessment of infectiousness for the given person and time
	 */
	double getProbabilityInfectiousToday();
	/**
	 * Log odds of being infectious today, useful for multiplicative models.
	 *
	 * @return the log odds corresponding to {@link #getProbabilityInfectiousToday()}
	 */
	double getLogOddsInfectiousToday();
	/**
	 * Count of contacts recorded for the person at this time step.
	 *
	 * @return the number of contacts observed or simulated for the person
	 */
	long getContactCount();
	/**
	 * Count of exposures recorded for the person at this time step.
	 *
	 * @return the number of exposure events attributed to the person
	 */
	long getExposureCount();
	
	/**
	 * Adjusted mobility factor after applying policies and individual behaviour.
	 *
	 * @return the mobility adjustment scalar applied to the person's baseline
	 * mobility (values typically in the range 0–1)
	 */
	double getAdjustedMobility();
	/**
	 * Adjusted transmissibility for the person after modifiers.
	 *
	 * @return the transmissibility scaling applied to the person based on
	 * behaviour and other modifiers
	 */
	double getAdjustedTransmissibility();
	/**
	 * Adjusted compliance metric for the person.
	 *
	 * @return the compliance level after adjustments used in intervention models
	 */
	double getAdjustedCompliance();
	/**
	 * Adjusted probability that the person uses a contact tracing app.
	 *
	 * @return the app use probability after adjustments, range 0–1
	 */
	double getAdjustedAppUseProbability();
	
	
	
//	default String header() {
//		return CSVUtil.headers(this.getClass());
//	}
//	
//	default String row() {
//		return CommonCSV.super.row()+","+csvFrom(
//			this.getPersonId(),
//			this.getBehaviour(),
//			this.isInfectious(),
//			this.isSymptomatic(),
//			this.getNormalisedSeverity(),
//			this.getNormalisedViralLoad(),
//			this.getContactExposure(),
//			this.getPresumedLocalPrevalence(),
//			this.getProbabilityInfectiousToday(),
//			this.getContactCount(),
//			this.getExposureCount()
//		);
//	}
}