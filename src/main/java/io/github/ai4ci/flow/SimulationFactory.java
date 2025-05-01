package io.github.ai4ci.flow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.output.StateExporter;

/**
 * Configuration of simulations, setup and baselining. This runs as a 
 * deamon thread and caches  
 */
public class SimulationFactory extends Thread {

	static int OOM_REPEAT = 10;
	
	public static void runExperiments(ExperimentConfiguration config, String urnBase, Updater updater, StateExporter exporter, int toStep, StateExporter finalState ) throws InterruptedException {
		
//		Scheduler configScheduler = Schedulers.single(); // For configuration
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
		
		SimulationFactory factory = SimulationFactory.startFactory(config, urnBase);
		
		while (!factory.completed()) {
			
			
			if (!factory.oom()) {
			
				if (!factory.ready()) log.debug("Execution thread waiting for simulation to be built");
				while (!factory.ready()) {
					Thread.yield();
				}
				
				log.debug("Executing new simulation - memory free: "+freeMem());
				{
					Outbreak outbreak = factory.deliver();
					IntStream.range(0, toStep).forEach(i -> {
						updater.update(outbreak);
						exporter.export(outbreak);
					});
					finalState.export(outbreak);
					exporter.flushAll();
					finalState.flushAll();
				}
				log.debug("Finishing simulation - memory free: "+freeMem());
				System.gc();
				log.debug("Post clean up - memory free: "+freeMem());
			
			} else {
				
				log.debug("Execution thread waiting for simulation but factory is out of memory.");
				for (int i = 0; i<OOM_REPEAT; i++) {
					System.gc();
					Thread.sleep(100);
					if (factory.oom()) {
						log.debug("Execution thread waiting for memory to be free... "+i+"/"+OOM_REPEAT);
					} else {
						break;
					}
				}
				if (factory.oom()) {
					log.error("Could not clear low memory issue. Aborting.");
					throw new RuntimeException("Out of memory.");
				}
				
			}
		}
		
	}
	
	public static int CACHE_SIZE = 2;
	
	static Logger log = LoggerFactory.getLogger(SimulationFactory.class);
	
	volatile Queue<Outbreak> queue; 
	volatile boolean halt;
	volatile boolean complete;
	volatile boolean waiting = false;
	Object semaphore = new Object();
	ExperimentConfiguration config;
	String urnBase;
	int from; 
	int to;
	long objSize=0;
	
	public boolean ready() {
		return !queue.isEmpty();
	}
	
	public boolean oom() {
		return freeMem() < objSize;
	}
	
	Outbreak deliver() {
		Outbreak out = queue.poll();
		if (waiting) synchronized(semaphore) { semaphore.notifyAll(); }
		return out;
	}
	
	public void halt() {
		log.debug("Requesting simulation factory shutdown");
		this.halt = true;
	}
	
	public boolean completed() {
		return this.complete & queue.isEmpty();
	}
	
	public static SimulationFactory startFactory(ExperimentConfiguration config, String urnBase) {
		return startFactory(config, urnBase, 0, config.getSetup().size());
	}
	
	public static SimulationFactory startFactory(ExperimentConfiguration config, String urnBase, int from, int to) {
		log.debug("Initialising simulation builder thread; building configurations "+from+" to "+to);
		SimulationFactory tmp = new SimulationFactory(config, urnBase, from, to);
		tmp.setDaemon(true);
		// Try and get this higher priority than computation 
		tmp.setPriority(10);
		tmp.setName("Simulation factory");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tmp.halt();
			}
		});
		tmp.start();
		return tmp;
	}
	
	public SimulationFactory(ExperimentConfiguration config, String urnBase, int from, int to) {
		this.queue = new ConcurrentLinkedQueue<>();
		halt = false;
		this.config = config;
		this.urnBase = urnBase;
		this.from = from;
		this.to = to;
		this.complete = false;
	}
	
	public void run() {
		
		// Lazy iterator.
		Iterator<Outbreak> build = new Iterator<>() {

			List<SetupConfiguration> setups = config.getSetup();
			List<ExecutionConfiguration> execs = config.getExecution();
			ExecutionBuilder setupBuilder;
			int setup = from;
			int exec = 0;
			
			@Override
			public boolean hasNext() {
				return setup < to & exec < execs.size();
			}

			@Override
			public Outbreak next() {
				
				if (setup >= to) throw new NoSuchElementException("Iterator exhausted");
				
				if (exec == 0) {
					// Click back to new setup
					setupBuilder = new ExecutionBuilder(
							setups.get(setup)
					);
					setupBuilder.setupOutbreak(urnBase);
				}
				
				ExecutionBuilder builder2 = setupBuilder.copy();
				ExecutionConfiguration exCfg = execs.get(exec); 
				builder2.baselineModel(exCfg);
				builder2.initialiseStatus(exCfg);
				
				if (exec >= execs.size()) {
					setup += 1;
					exec = 0;
				} else {
					exec +=1;
				}
				
				return builder2.build();
			}
			
		};
		
		halt = halt & build.hasNext(); 
		
		do {
			
			while (queue.size() < CACHE_SIZE & build.hasNext()) {
				if (freeMem() > objSize) {
					log.debug("Simulation build process started; "+queue.size()+" already cached.");
					Outbreak tmp = build.next();
					if (objSize == 0) {objSize = estimateSize(tmp);	}
					queue.add(tmp);
					log.debug("Simulation build process finished: "+tmp.getUrn());
				} else {
					log.warn("Cannot build another simulation due to low memory.");
				}
			}
			
			halt = halt | !build.hasNext();
			
			while (!halt & queue.size() >= CACHE_SIZE) {
				try {
					synchronized(semaphore) {
						waiting = true;
						semaphore.wait();
					}
				} catch (InterruptedException e) {
					halt=true;
				}
			}
			waiting = false;
			
		} while (!halt);
		
		log.debug("Simulation factory shutting down");
		if (build.hasNext()) log.debug("Simulation factory did not complete all simulations.");
		complete = true;
	}
	
	
	private static long freeMem() {
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
		// allocatedMemory = allocatedMemory > maxMem ? maxMem : allocatedMemory;  
		return (runtime.maxMemory() - allocatedMemory);
	}
	
	private static long estimateSize(Object obj) {
		long objSize = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.close();
			objSize = baos.size();
			log.debug("Each simulation takes up about "+String.format("%1.2f", ((double) objSize)/(1024*1024*1024))+" Gb memory before running.");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return objSize;
	}
}
