package io.github.ai4ci.flow;

import java.io.IOException;
import java.lang.Thread.State;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import io.github.ai4ci.config.BatchConfiguration;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

public class SimulationMonitor implements Runnable {

	static Logger log = LoggerFactory.getLogger(SimulationMonitor.class);
	
	SimulationFactory factory;
	StateExporter exporter;
	SimulationExecutor executor;
	int duration;
	
	Object trigger = new Object();
	
	public SimulationMonitor(ExperimentConfiguration config, Path baseDirectory) throws IOException {
		this.exporter = config.exporter(baseDirectory);
		exporter.writeInputConfiguration(config);
		List<SetupConfiguration> setups = config.getBatchSetupList();
		List<ExecutionConfiguration> executions = config.getExecution();
		factory = SimulationFactory.startFactory(setups, executions, config.getBatchConfig().getUrnBase(), this);
		duration = config.getBatchConfig().getSimulationDuration();
	}
	
	
	static long freeMem() {
		SystemInfo si = new SystemInfo();
		HardwareAbstractionLayer hal = si.getHardware();
		
		return hal.getMemory().getAvailable();
		
//		Runtime runtime = Runtime.getRuntime();
//		long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
//		// allocatedMemory = allocatedMemory > maxMem ? maxMem : allocatedMemory;  
//		return (runtime.maxMemory() - allocatedMemory);
	}
	
	static String freeMemG() {
		return String.format("%1.2f Gb", ((float) freeMem())/(1024*1024*1024));
	}
	
	private boolean isRunning(SimulationExecutor ex) {
		return ex != null &&  !ex.getState().equals(State.TERMINATED);
	}
	
	public void run() {
		SimulationExecutor executor = null;
		try {
			
			SystemInfo si = new SystemInfo();
			HardwareAbstractionLayer hal = si.getHardware();
			long maxMem = hal.getMemory().getTotal();
			
			while (!factory.finished() || isRunning(executor)) {
				
				
				int checkAgainInMs = 1000;
				// long memLimit = factory.getSimulationSize().orElse(0);
				
				if (freeMem() < maxMem * 0.20) {
					System.gc();
					factory.pause();
					log.warn("Low memory. Throttling factory production. Memory: "+freeMemG());
				} else {
					// This will only be successful if the 
					factory.unpause();
				}
				
				if (freeMem() < maxMem * 0.10) {
					if (executor != null) {
						executor.pause();
						log.warn("Very low memory. Throttling simulation execution. Memory: "+freeMemG());
					} else {
						log.warn("Very low memory. Delaying simulation execution. Memory: "+freeMemG());
					}
					checkAgainInMs = 1000;
				} else {
					if ( isRunning(executor) ) {
						executor.unpause();
					} else {
						if (executor != null) {
							// Executor is not running because it is complete
							// log the final state:
							log.info(executor.status());
							executor = null;
						}
						// Start a new simulation
						if (factory.ready()) {
							executor = new SimulationExecutor(this, factory.deliver(), exporter, duration);
							executor.start();
							log.info("Starting new simulation");
						} else {
							log.warn("Waiting for simulation to run");
							factory.unpause();
							checkAgainInMs = 1000;
						}
					}
				}
				
				if (freeMem() < maxMem * 0.05) {
					
					if (
							exporter.allWaiting() &&
							(executor == null || executor.isWaiting()) &&
							factory.isWaiting()
					) {
						log.error("Critically low memory. All processes blocked. Terminating early: "+freeMemG());
						throw new InterruptedException("Out of memory");
					} else {
						log.warn("Critically low memory. Waiting for exporters to clear backlog.: "+freeMemG());
						System.gc();
						checkAgainInMs = 1000;
					}
					
				}
				
				log.info("Factory: "+factory.status());
				if (isRunning(executor)) log.info("Executor: "+executor.status()+" - "+freeMemG());
				
				// Sleep the monitor thread until a simulation finishes or a 
				// new simulation is queued.
				waitForEvent(checkAgainInMs);
			}
			
			while (!exporter.allWaiting()) {
				log.info("Waiting for output to complete...");
				Thread.sleep(1000);
			}
			log.info("Completed.");
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			if(factory != null) factory.halt();
			if(exporter != null) exporter.close();
			if(executor != null) executor.halt();
		}
	}
	
	private void waitForEvent(long ms) throws InterruptedException {
		synchronized(this.trigger) {this.trigger.wait(ms);}
	}
	
	protected void notifyExecutionComplete(SimulationExecutor executor) {
		log.info("Execution complete: "+executor.status());
		synchronized(this.trigger) {this.trigger.notifyAll();}
	};
	
	protected void notifyFactoryReady(SimulationFactory factory) {
		log.info("Build complete: "+factory.status());
		synchronized(this.trigger) {this.trigger.notifyAll();}
	};
	
}


//Scheduler configScheduler = Schedulers.single(); // For configuration
//Scheduler executionScheduler = Schedulers.single(); // For parallel execution
//
//Iterator<SetupConfiguration> cfgSetup = config.getSetup().iterator();
//
//Flowable
//	.create(emitter -> {
//		if (cfgSetup.hasNext()) {
//			emitter.onNext(cfgSetup.next());
//		} else {
//			emitter.onComplete();
//		}
//	}, BackpressureStrategy.BUFFER)
//	.map(cfg -> {
//		ExperimentBuilder builder = new ExperimentBuilder();
//		builder.setupOutbreak((SetupConfiguration) cfg, urnBase);
//		return builder;
//	})
//	.flatMapStream(builder -> {
//		return config.getExecution().stream().map(exCfg -> {
//			ExperimentBuilder builder2 = builder.copy();
//			builder2.baselineModel(exCfg);
//			builder2.initialiseStatus(exCfg);
//			return builder2;
//		});	
//	}, 2) //prefetch 2 items
//	.onBackpressureBuffer(2)
//	.subscribeOn(configScheduler)
//	.observeOn(executionScheduler)
//	.blockingSubscribe( builder2 -> {
//		log.debug("Executing new simulation - memory free: "+freeMem());
//		{
//			// Limit scope of outbreak
//			Outbreak outbreak = builder2.build();
//			IntStream.range(0, toStep).forEach(i -> {
//				updater.update(outbreak);
//				exporter.export(outbreak);
//			});
//			finalState.export(outbreak);
//		}
//		log.debug("Finishing simulation - memory free: "+freeMem());
//		System.gc();
//		log.debug("Post clean up - memory free: "+freeMem());
//	})
//	;

