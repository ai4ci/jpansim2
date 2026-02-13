package io.github.ai4ci.example;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;

import io.github.ai4ci.SlurmAwareLogger;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.flow.SimulationMonitor;

class RunExampleSim {

	public static void main(String[] args)
			throws IOException, InterruptedException {

		String name;
		if (args.length > 0) {
			name = args[0];
		} else {
			name = Experiment.BEHAVIOUR.name();
		}
		var expt = Experiment.valueOf(name);
		ExperimentConfiguration config = expt.config;

		var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		var dtfs = dtf.format(LocalDateTime.now());

		var dir = SystemUtils.getUserHome()
			.toPath()
			.resolve("tmp/test")
			.resolve(name.toLowerCase())
			.resolve(dtfs);
		SlurmAwareLogger.setupLogger(config, dir, Level.INFO, Level.DEBUG);
		var mon = new SimulationMonitor(config, dir);
		mon.run();

		System.out.println("finished");
	}

}
