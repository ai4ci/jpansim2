package io.github.ai4ci.abm;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;

import io.github.ai4ci.SlurmAwareLogger;
import io.github.ai4ci.config.BatchConfiguration;
import io.github.ai4ci.config.ExampleConfig;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.Exporters;
import io.github.ai4ci.config.inhost.MarkovStateModel;
import io.github.ai4ci.config.inhost.PhenomenologicalModel;
import io.github.ai4ci.config.setup.BarabasiAlbertConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.config.setup.WattsStrogatzConfiguration;
import io.github.ai4ci.flow.SimulationMonitor;

class TestDefaultSim {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		ExperimentConfiguration tmp = 
				ExampleConfig.AGE_STRAT.config;
				//ExampleConfig.TEST_CONCURRENCY.config;
				//ExampleConfig.NETWORKS.config;
				//ExampleConfig.DEFAULT.config;
				//ExampleConfig.BEHAVIOUR.config; 
		Path dir = SystemUtils.getUserHome().toPath().resolve("tmp/test");
		
		SlurmAwareLogger.setupLogger(tmp, dir, Level.INFO, Level.DEBUG);
		SimulationMonitor mon = new SimulationMonitor(tmp, dir);
		mon.run();
		
		System.out.println("finished");
	} 

}
