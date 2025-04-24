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
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import io.github.ai4ci.abm.BehaviourModel.SmartAgentTesting;
import io.github.ai4ci.abm.PolicyModel.NoControl;
import io.github.ai4ci.config.AgeStratifiedNetworkConfiguration;
import io.github.ai4ci.config.PartialExecutionConfiguration;
import io.github.ai4ci.flow.ExperimentConfiguration;
import io.github.ai4ci.flow.ExperimentConfiguration.AgeStratifiedExperimentConfiguration;
import io.github.ai4ci.flow.ExperimentConfiguration.BasicExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableAgeStratifiedExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableBasicExperimentConfiguration;
import io.github.ai4ci.flow.ImmutableExperimentFacet;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutablePersonCSV;

class TestJackson {

	@Test
	void testJson() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		
		BasicExperimentConfiguration tmp0 = BasicExperimentConfiguration.DEFAULT;
		
		String json0 = om.writeValueAsString(tmp0);
		System.out.println(json0);
		System.out.print("\n\n");
		
		AgeStratifiedExperimentConfiguration tmp = 
				ImmutableAgeStratifiedExperimentConfiguration.copyOf(	
					AgeStratifiedExperimentConfiguration.DEFAULT
				)
			.withSetupConfig(AgeStratifiedNetworkConfiguration.DEFAULT)
			.withFacets(
						ImmutableExperimentFacet.builder()
							.putModification(
								"smart-agent",
								PartialExecutionConfiguration.builder()
									.setDefaultPolicyModelName(NoControl.class.getSimpleName())
									.setDefaultBehaviourModelName(SmartAgentTesting.class.getSimpleName())
									.build()
								)
						.setName("test")
						.build()
				)
				;
		
		
		String json = om.writeValueAsString(tmp);
		System.out.println(json);
		
		AgeStratifiedExperimentConfiguration rt = om.readerFor(AgeStratifiedExperimentConfiguration.class).readValue(json);
		String json2 = om.writeValueAsString(rt);
		assertEquals(json, json2);
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

