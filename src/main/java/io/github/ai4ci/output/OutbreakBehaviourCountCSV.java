package io.github.ai4ci.output;

import java.util.stream.Stream;

import org.immutables.value.Value;

import io.github.ai4ci.Export;
import io.github.ai4ci.Export.Stage;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.CSVWriter;

/**
 * A long format key value list where key is behaviour type and value 
 */
@Value.Immutable
@Export(stage = Stage.UPDATE,value = "behaviours.csv",size = 16, selector=OutbreakBehaviourCountCSV.Selector.class, writer=CSVWriter.class)
public interface OutbreakBehaviourCountCSV extends CommonCSV.State {

	public String getPolicy();
	public String getBehaviour();
	public Long getCount();
	
	public class Selector implements Export.Selector {

		@Override
		public Stream<OutbreakBehaviourCountCSV> apply(Outbreak t) {
			return CSVMapper.INSTANCE.toBehaviourCSV(t);
		}

	}

}
