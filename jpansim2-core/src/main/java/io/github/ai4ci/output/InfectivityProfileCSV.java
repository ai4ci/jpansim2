package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Infectivity profile records exported at baseline.
 *
 * <p>
 * Main purpose: record the infectivity profile used by the outbreak baseline as
 * a time indexed distribution. Records are produced during the BASELINE stage
 * and exported per model to a CSV file ({@code ip.csv}).
 *
 * <p>
 * Downstream uses: used by plotting utilities, analytic pipelines and any code
 * that reconstructs per‑time infectivity curves for sensitivity analysis.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(
		stage = Stage.BASELINE,
		value = "ip.csv",
		size = 16,
		selector = InfectivityProfileCSV.Selector.class,
		writer = CSVWriter.class
)
public interface InfectivityProfileCSV extends CommonCSV.Model {

	/**
	 * Selector class implements Export.Selector to provide infectivity profile
	 * extraction from Outbreak simulations. Retrieves infectivity profile data
	 * using CSVMapper for CSV serialization.
	 */
	static class Selector implements Export.Selector {
		@Override
		public Stream<InfectivityProfileCSV> apply(Outbreak o) {
			return CSVMapper.INSTANCE.infectivityProfile(o);
		}
	}

	/**
	 * Probability of transmission at the given time step.
	 *
	 * @return the infection probability (density) at the given time step; values
	 *         are typically normalised so that the profile integrates to the
	 *         baseline infectiousness over the infectious period
	 */
	double getProbability();

	/**
	 * Time step in the infectivity profile.
	 *
	 * @return the time step index (tau) corresponding to the probability value
	 */
	int getTau();

}