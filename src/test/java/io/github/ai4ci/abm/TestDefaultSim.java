package io.github.ai4ci.abm;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;

import io.github.ai4ci.SlurmAwareLogger;
import io.github.ai4ci.config.ExampleConfig;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.flow.SimulationMonitor;

class TestDefaultSim {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		ExperimentConfiguration tmp = 
				//ExampleConfig.DEFAULT.config;
				ExampleConfig.BEHAVIOUR.config; 
		Path dir = SystemUtils.getUserHome().toPath().resolve("tmp");
		
		SlurmAwareLogger.setupLogger(tmp, dir, Level.INFO, Level.DEBUG);
		SimulationMonitor mon = new SimulationMonitor(tmp, dir);
		mon.run();
		
		System.out.println("finished");
	} 

}
