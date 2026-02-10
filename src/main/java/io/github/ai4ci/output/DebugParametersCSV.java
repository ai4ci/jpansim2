package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * DebugParametersCSV represents debugging and parameter configuration data
 * exported at simulation start.
 *
 * <p>Main purpose: capture key model parameters and derived diagnostics used
 * for debugging and sensitivity analysis. Records are produced during the
 * START stage and are exported per simulation execution to CSV files
 * ({@code debug.csv}).
 *
 * <p>Downstream uses: consumed by diagnostic pipelines, regression tests and
 * analysis notebooks that require a snapshot of the model parameters used to
 * produce a particular simulation run.
 *
 * @author Rob Challen
 */
@Value.Immutable
// @Data
@Export(stage = Stage.START, value = "debug.csv", size = 64, selector = DebugParametersCSV.Selector.class, writer=CSVWriter.class)
public interface DebugParametersCSV extends CommonCSV.Execution {
	
	/**
	 * Selector class implements Export.Selector to provide debug parameter extraction
	 * from Outbreak simulations. Transforms baseline configuration data into
	 * parameter records using CSVMapper for CSV serialization.
	 */
	static class Selector implements Export.Selector {
		@Override
		public Stream<DebugParametersCSV> apply(Outbreak o) {
			return Stream.of(CSVMapper.INSTANCE.toCSV(o, o.getBaseline()));
		}
	}
	
	/**
	 * The viral load transmissibility parameter.
	 *
	 * @return the transmissibility scaling applied to viral load when computing
	 * per‑contact transmission probability
	 */
	double getViralLoadTransmissibilityParameter();
	/**
	 * Severity threshold used to determine symptomatic presentation.
	 *
	 * @return the threshold value above which an infected person is considered
	 * symptomatic for the purposes of reporting and clinical progression
	 */
	double getSeveritySymptomsCutoff();
	/**
	 * Severity threshold used to determine hospitalisation.
	 *
	 * @return the threshold value above which an infected person is considered
	 * likely to require hospital admission
	 */
	double getSeverityHospitalisationCutoff();
	/**
	 * Severity threshold used to determine fatal outcomes.
	 *
	 * @return the threshold value above which an infected person is considered
	 * likely to have a fatal outcome
	 */
	double getSeverityDeathCutoff();
	/**
	 * The infectious duration assumed by the baseline.
	 *
	 * @return the infective duration in model time units used by the baseline
	 * infectivity profile
	 */
	int getInfectiveDuration();
	/**
	 * Average contact degree derived from the baseline network configuration.
	 *
	 * @return the average number of contacts per agent used as a diagnostic
	 * for network sparsity and mixing assumptions
	 */
	double getAverageContactDegree();
	/**
	 * Approximate bond percolation threshold for the configured contact network.
	 *
	 * @return the critical percolation probability used to understand epidemic
	 * connectivity in the baseline network
	 */
	double getPercolationThreshold();
}