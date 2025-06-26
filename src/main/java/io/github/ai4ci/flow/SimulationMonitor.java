package io.github.ai4ci.flow;

import java.io.IOException;
import java.lang.Thread.State;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

public class SimulationMonitor implements Runnable {

	private static final long WAIT_FOR_MEMORY = 5*60*1000;
	// reserve at least 512Mb for system
	private static final long RESERVE = 512*1024*1024;
	
	static SystemInfo si = new SystemInfo();
	static HardwareAbstractionLayer hal = si.getHardware();
	static Logger log = LoggerFactory.getLogger(SimulationMonitor.class);
	
	SimulationFactory factory;
	StateExporter exporter;
	
	int duration;
	
	Object trigger = new Object();
	volatile private boolean halt = false;
	
	public SimulationMonitor(ExperimentConfiguration config, Path baseDirectory) throws IOException {
		this.exporter = config.exporter(baseDirectory);
		exporter.writeInputConfiguration(config);
		List<SetupConfiguration> setups = config.getBatchSetupList();
		List<ExecutionConfiguration> executions = config.getExecution();
		factory = SimulationFactory.startFactory(setups, executions, config.getBatchConfig().getUrnBase(), this);
		duration = config.getBatchConfig().getSimulationDuration();
	}
	
	public static double usedOfAvailable(long sysReserved) {
		
		float sysAvail = ((float) hal.getMemory().getAvailable()-sysReserved)/(1024*1024*1024);
		float freeHeap = ((float) Runtime.getRuntime().freeMemory())/(1024*1024*1024);
		float maxHeap = ((float) Runtime.getRuntime().maxMemory())/(1024*1024*1024);
		float currentHeap = ((float) Runtime.getRuntime().totalMemory())/(1024*1024*1024);
		float allocatable = Math.min(maxHeap-currentHeap, sysAvail);
		float allocated = currentHeap - freeHeap;
		return allocated/(currentHeap+allocatable);
	}
	
	public static String freeMemG() {
		float sys = ((float) hal.getMemory().getAvailable())/(1024*1024*1024);
		float freeHeap = ((float) Runtime.getRuntime().freeMemory())/(1024*1024*1024);
		float maxHeap = ((float) Runtime.getRuntime().maxMemory())/(1024*1024*1024);
		float currentHeap = ((float) Runtime.getRuntime().totalMemory())/(1024*1024*1024);
		float allocatable = Math.min(maxHeap-currentHeap, sys);
		float allocated = currentHeap - freeHeap;
		return String.format("%1.2f Gb remaining; %1.2f Gb used (%1.2f%%) [%1.2f Gb system]", 
				allocatable+freeHeap,
				allocated,
				allocated/(currentHeap+allocatable)*100,
				sys
		);
	}
	
	private boolean isRunning(SimulationExecutor ex) {
		return ex != null &&  !ex.getState().equals(State.TERMINATED);
	}
	
	public void run() {
		SimulationExecutor executor = null;
		try {
			
			double freeSysGb = hal.getMemory().getAvailable()/(1024*1024*1024);
			
			long abortTime = Long.MAX_VALUE;
			
			while (!factory.finished() || isRunning(executor)) {
				
				if (halt) break;
				
				int checkAgainInMs = 1000;
				// long memLimit = factory.getSimulationSize().orElse(0);
				
				if (usedOfAvailable(RESERVE) > 0.80 || freeSysGb < 1) {
					factory.pause();
					log.warn("Low memory. Throttling factory production. Memory: "+freeMemG());
				} else {
					// This will only be successful if the cache is not already full;
					factory.unpause();
				}
				
				if (usedOfAvailable(RESERVE) > 0.90 || freeSysGb < 0.5) {
					System.gc();
					abortTime = Math.min(abortTime, System.currentTimeMillis() + WAIT_FOR_MEMORY);
					if (executor != null) {
						executor.pause();
						log.warn("Very low memory. Throttling simulation execution. Memory: "+freeMemG());
					} else {
						log.warn("Very low memory. Delaying simulation execution. Memory: "+freeMemG());
					}
					log.warn("Aborting in "+(abortTime-System.currentTimeMillis())/1000+" secs unless memory improves");
					log.info("Exporters: "+exporter.report());
					
					if (exporter.allWaiting()) {
						log.warn("Exporters all empty. Trying to clear simulation despite very low memory.");
						if (executor != null) { executor.unpause(); }
					}
					
					checkAgainInMs = 1000;
					
				} else {
					abortTime = Long.MAX_VALUE;
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
				
				if (usedOfAvailable(RESERVE) > 0.95 || freeSysGb < 0.25) {
					
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
				if (System.currentTimeMillis() > abortTime) {
					log.error("Low memory has not been cleared after "+WAIT_FOR_MEMORY/1000+" seconds. Aborting");
					break;
				}
				waitForEvent(checkAgainInMs);
			}
			exporter.finaliseAll();
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
	}

	public void handle(Exception e) {
		halt = true;
		e.printStackTrace();
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

