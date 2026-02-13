package io.github.ai4ci.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import io.github.ai4ci.abm.TestUtils;
import io.github.ai4ci.abm.behaviour.SmartAgentTesting;
import io.github.ai4ci.abm.policy.NoControl;
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.setup.AgeStratifiedDemography;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.functions.SimpleDistribution;
import io.github.ai4ci.output.CSVMapper;
import io.github.ai4ci.output.ImmutableLineListCSV;
import io.github.ai4ci.util.CSVUtil;

class TestJackson {

	@Test
	void testJson() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		om.enable(SerializationFeature.INDENT_OUTPUT);
		om.registerModules(new GuavaModule());
		om.setSerializationInclusion(Include.NON_NULL);
		om.setSerializationInclusion(Include.NON_EMPTY);
		
		ExperimentConfiguration tmp0 = ExperimentConfiguration.DEFAULT;
		
		String json0 = om.writeValueAsString(tmp0);
		System.out.println(json0);
		System.out.print("\n\n");
		
		ExperimentConfiguration tmp = 
			ExperimentConfiguration.DEFAULT
			.withSetupConfig(
				SetupConfiguration.DEFAULT
					.withDemographics(AgeStratifiedDemography.DEFAULT)
			)
			.withFacet(
				"behaviour",
				PartialExecutionConfiguration.builder()
					.setName("smart-agent")
					.setDefaultPolicyModelName(NoControl.class.getSimpleName())
					.setDefaultBehaviourModelName(SmartAgentTesting.class.getSimpleName())
					.build()
			)
			.withFacet(
				"in-host",
				PartialExecutionConfiguration.builder()
					.setName("longer-incubation")
					.setInHostConfiguration(
							PartialPhenomenologicalModel.builder()
								.setIncubationPeriod(
										SimpleDistribution.logNorm(10D, 4D)
								)
								.build()
					).build()
				);
		
		String json = om.writeValueAsString(tmp);
		System.out.println(json);
		
		ExperimentConfiguration rt = om.readerFor(ExperimentConfiguration.class).readValue(json);
		String json2 = om.writeValueAsString(rt);
		assertEquals(json, json2);
		
		List<ExecutionConfiguration> ecfg = rt.getExecution(); 
		ecfg.stream().map(e -> e.toString()).forEach(System.out::println);
	}

	@Test
	void testJacksonCSV() throws IOException {
		
		ImmutableLineListCSV tmp = CSVMapper.INSTANCE.toCSV(
			TestUtils.mockPersonState()	
		);
		
//		CsvMapper cm = CsvMapper.builder()
//				.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
//				.build();
//		// cm.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
//		CsvSchema sch = cm.schemaFor(ImmutablePersonStateCSV.class).withHeader();
//		try (StringWriter strW = new StringWriter()) {
//			  SequenceWriter seqW = cm.writer(sch)
//			    .writeValues(strW);
//			  seqW.write(tmp);
//			  seqW.write(tmp);
//			  System.out.println(strW.toString());
//		}
		
		CSVUtil<ImmutableLineListCSV> conv = new CSVUtil<ImmutableLineListCSV>(ImmutableLineListCSV.class);
		
		System.out.println(conv.headers());
		System.out.println(conv.row(tmp));
	}
	

}

