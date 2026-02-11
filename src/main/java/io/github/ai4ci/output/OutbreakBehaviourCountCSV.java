package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

/**
 * Behaviour counts exported during simulation updates in long format.
 *
 * <p>
 * Main purpose: provide a long format key/value listing of counts per behaviour
 * type for a simulation execution. Records are produced at the UPDATE stage and
 * are aggregated per simulation state (not per individual), and written to a
 * CSV file ({@code behaviours.csv}).
 *
 * <p>
 * Downstream uses: aggregated behaviour counts are used for trend analysis,
 * policy evaluation and for combining with other summary outputs.
 *
 * @author Rob Challen
 */
@Value.Immutable
@Export(
		stage = Stage.UPDATE,
		value = "behaviours.csv",
		size = 16,
		selector = OutbreakBehaviourCountCSV.Selector.class,
		writer = CSVWriter.class
)
public interface OutbreakBehaviourCountCSV extends CommonCSV.State {

	/**
	 * Selector for outbreak behaviour count CSV export. This is used to extract
	 * the relevant data from the outbreak state and convert it into a stream of
	 * records to be written to the CSV file.
	 */
	public class Selector implements Export.Selector {

		@Override
		public Stream<OutbreakBehaviourCountCSV> apply(Outbreak t) {
			return CSVMapper.INSTANCE.toBehaviourCSV(t);
		}

	}

	/**
	 * Behaviour type key.
	 *
	 * @return the name of the behaviour being counted (key in the long format)
	 */
	public String getBehaviour();

	/**
	 * Count value for the behaviour.
	 *
	 * @return the frequency count of agents exhibiting the given behaviour in
	 *         the current simulation state
	 */
	public Long getCount();

	/**
	 * Policy name associated with the count.
	 *
	 * @return the policy label under which the behaviour counts were computed
	 */
	public String getPolicy();

}