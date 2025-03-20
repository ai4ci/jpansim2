package io.github.ai4ci.abm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.flow.ExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableExperimentConfiguration;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutablePersonCSV;

class TestJackson {

	@Test
	void testJson() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.setSerializationInclusion(Include.NON_NULL);
		String json = om.writeValueAsString(
		    ExperimentConfiguration.DEFAULT);
		System.out.println(json);
		
		ExperimentConfiguration rt = om.readerFor(ImmutableExperimentConfiguration.class).readValue(json);
		assertEquals(rt, ExecutionConfiguration.DEFAULT);
	}

	@Test
	void testJacksonCSV() throws IOException {
		
		ImmutablePersonCSV tmp = CSVMapper.INSTANCE.toCSV(
			TestUtils.mockPersonState()	
		);
		
		CsvMapper cm = CsvMapper.builder()
				.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
				.build();
		// cm.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		CsvSchema sch = cm.schemaFor(ImmutablePersonCSV.class).withHeader();
		try (StringWriter strW = new StringWriter()) {
			  SequenceWriter seqW = cm.writer(sch)
			    .writeValues(strW);
			  seqW.write(tmp);
			  seqW.write(tmp);
			  System.out.println(strW.toString());
		}
		
	}
	
//	void testSimpleCSV() throws IOException {
//		
//		ImmutablePersonHistory tmp = ImmutablePersonHistory.builder()
//			.setPersonId(1)
//			.setState(InfectionState.RECOVERED)
//			.setTime(10)
//			.build();
//		
//		CsvProcessor<ImmutablePersonHistory> csvProcessor = new CsvProcessor<ImmutablePersonHistory>(ImmutablePersonHistory.class);
//		
//		try (StringWriter strW = new StringWriter()) {
//			BufferedWriter bw = new BufferedWriter(strW);
//			csvProcessor.writeHeader(bw, true /* write header */);
//			csvProcessor.writeRow(bw, tmp, true);
//			csvProcessor.writeRow(bw, tmp, true);
//			System.out.println(strW.toString());
//		}
//		
//	}
}

