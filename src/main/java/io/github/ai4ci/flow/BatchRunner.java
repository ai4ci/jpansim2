package io.github.ai4ci.flow;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.config.BatchConfiguration;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.output.StateExporter;

public class BatchRunner implements SimulationMonitor {

	public static Logger log = LoggerFactory.getLogger(BatchRunner.class);
	BatchConfiguration cfg;
	
	public BatchRunner(BatchConfiguration cfg) {
		this.cfg = cfg;
	}
	
	public void run() throws IOException, InterruptedException {
		
		StateExporter exporter = cfg.exporter();
		Updater updater = new Updater();
		
		ExperimentConfiguration config = ExperimentConfiguration.readConfig(cfg.getExecutionConfigurationPath());
		exporter.writeInputConfiguration(config);
		
		int size = config.getSetup().size();
		int chunkSize = (int) Math.ceil( ((double) size) / cfg.getBatchTotal());
		
		// This is going to be made up of a list of setups and replicates.
		// each replicate is stand alone so we can just filter this list to 
		// get to the list for this node.	
		List<SetupConfiguration> setups = config.getSetup().subList(
				(cfg.getBatchNumber()-1) * chunkSize,
				Math.min(cfg.getBatchNumber() * chunkSize, size)
		);
		
		List<ExecutionConfiguration> executions = config.getExecution();
		
		runExperiments(
				setups, executions,
				cfg.getUrnBase(), updater, exporter, cfg.getSimulationDuration()
		);
		
		exporter.close();
		
	}
	
	// TODO: refactor this so that the execution happens in another thread and
	// this is a proper monitor. 
	
	@Deprecated
	public static void runExperiments(
			ExperimentConfiguration config, String urnBase, Updater updater, StateExporter exporter, int toStep) throws InterruptedException {
		runExperiments(
				config.getSetup(), config.getExecution(),
				urnBase, updater, exporter, toStep
		);
	}
	
	public static void runExperiments(
			List<SetupConfiguration> setups, 
			List<ExecutionConfiguration> executions,
			String urnBase, Updater updater, StateExporter exporter, int toStep) throws InterruptedException {
		
		
		
		SimulationFactory factory = SimulationFactory.startFactory(setups, executions, urnBase);
		
		while (!factory.completed()) {
			
			if (!factory.oom()) {
			
				if (!factory.ready()) SimulationFactory.log.debug("Execution thread waiting for simulation to be built");
				while (!factory.ready()) {
					Thread.yield();
				}
				
				SimulationFactory.log.debug("Executing new simulation - memory free: "+SimulationMonitor.freeMem());
				{
					Outbreak outbreak = factory.deliver();
					IntStream.range(0, toStep).forEach(i -> {
						exporter.export(outbreak);
						updater.update(outbreak);
					});
					exporter.finalise(outbreak);
				}
				SimulationFactory.log.debug("Finishing simulation - memory free: "+SimulationMonitor.freeMem());
				System.gc();
				SimulationFactory.log.debug("Post clean up - memory free: "+SimulationMonitor.freeMem());
			
			} else {
				
				SimulationFactory.log.debug("Execution thread waiting for simulation but factory is out of memory.");
				for (int i = 0; i<SimulationFactory.OOM_REPEAT; i++) {
					System.gc();
					Thread.sleep(100);
					if (factory.oom()) {
						SimulationFactory.log.debug("Execution thread waiting for memory to be free... "+i+"/"+SimulationFactory.OOM_REPEAT);
					} else {
						break;
					}
				}
				if (factory.oom()) {
					SimulationFactory.log.error("Could not clear low memory issue. Aborting.");
					throw new RuntimeException("Out of memory.");
				}
				
			}
		}
		
	}

}

//Scheduler configScheduler = Schedulers.single(); // For configuration
//		Scheduler executionScheduler = Schedulers.single(); // For parallel execution
//		
//		Iterator<SetupConfiguration> cfgSetup = config.getSetup().iterator();
//		
//		Flowable
//			.create(emitter -> {
//				if (cfgSetup.hasNext()) {
//					emitter.onNext(cfgSetup.next());
//				} else {
//					emitter.onComplete();
//				}
//			}, BackpressureStrategy.BUFFER)
//			.map(cfg -> {
//				ExperimentBuilder builder = new ExperimentBuilder();
//				builder.setupOutbreak((SetupConfiguration) cfg, urnBase);
//				return builder;
//			})
//			.flatMapStream(builder -> {
//				return config.getExecution().stream().map(exCfg -> {
//					ExperimentBuilder builder2 = builder.copy();
//					builder2.baselineModel(exCfg);
//					builder2.initialiseStatus(exCfg);
//					return builder2;
//				});	
//			}, 2) //prefetch 2 items
//			.onBackpressureBuffer(2)
//			.subscribeOn(configScheduler)
//			.observeOn(executionScheduler)
//			.blockingSubscribe( builder2 -> {
//				log.debug("Executing new simulation - memory free: "+freeMem());
//				{
//					// Limit scope of outbreak
//					Outbreak outbreak = builder2.build();
//					IntStream.range(0, toStep).forEach(i -> {
//						updater.update(outbreak);
//						exporter.export(outbreak);
//					});
//					finalState.export(outbreak);
//				}
//				log.debug("Finishing simulation - memory free: "+freeMem());
//				System.gc();
//				log.debug("Post clean up - memory free: "+freeMem());
//			})
//			;
