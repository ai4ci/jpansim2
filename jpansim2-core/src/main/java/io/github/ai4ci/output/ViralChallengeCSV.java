package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.output.CSVWriter;
import io.github.ai4ci.flow.output.Export;
import io.github.ai4ci.flow.output.Export.Stage;

@Value.Immutable
@Export(
		stage = Stage.BASELINE,
		value = "viral-challenge.csv",
		size = 64 * 64,
		selector = ViralChallengeCSV.Selector.class,
		writer = CSVWriter.class
)
public interface ViralChallengeCSV extends CommonCSV.Individual {

	/**
	 * Selector class that implements Export.Selector to provide a stream of
	 * LineListCSV records from an Outbreak simulation for database export.
	 */
	static class Selector implements Export.Selector {
		@Override
		public Stream<ViralChallengeCSV> apply(Outbreak o) {

			var prof = o.getExecutionConfiguration()
				.getViralChallengeProfile();
			return prof.stream()
				.flatMap(
					i -> i.getStates()
						.stream()
						.map(
							ihm -> CSVMapper.INSTANCE
								.toViralChallengeCSV(o, i.getPersonId(), ihm)
						)
				);

		}
	}
}