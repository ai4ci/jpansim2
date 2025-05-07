package io.github.ai4ci.flow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.ExperimentConfiguration;
import io.github.ai4ci.config.SetupConfiguration;

/**
 * Configuration of simulations, setup and baselining. This runs as a 
 * deamon thread and caches  
 */
public class SimulationFactory extends Thread {

	static int OOM_REPEAT = 10;
	
	public static int CACHE_SIZE = 2;
	
	static Logger log = LoggerFactory.getLogger(SimulationFactory.class);
	
	volatile Queue<Outbreak> queue; 
	volatile boolean halt;
	volatile boolean complete;
	volatile boolean waiting = false;
	Object semaphore = new Object();
	List<SetupConfiguration> setups;
	List<ExecutionConfiguration> executions;
	String urnBase;
	long objSize=0;
	
	public boolean ready() {
		return !queue.isEmpty();
	}
	
	public boolean oom() {
		return SimulationMonitor.freeMem() < objSize;
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
	
	public static SimulationFactory startFactory(
			List<SetupConfiguration> setups, 
			List<ExecutionConfiguration> executions,
			String urnBase) {
		
		log.debug("Initialising simulation builder thread; building configurations");
		SimulationFactory tmp = new SimulationFactory(setups, executions, urnBase);
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
	
	public SimulationFactory(List<SetupConfiguration> setups, 
			List<ExecutionConfiguration> executions, String urnBase) {
		this.queue = new ConcurrentLinkedQueue<>();
		halt = false;
		this.setups = setups;
		this.executions = executions;
		this.urnBase = urnBase;
		this.complete = false;
	}
	
	public void run() {
		
		// Lazy iterator.
		Iterator<Outbreak> build = new Iterator<>() {

			ExecutionBuilder setupBuilder;
			int setup = 0;
			int exec = 0;
			
			@Override
			public boolean hasNext() {
				return setup < setups.size() && exec < executions.size();
			}

			@Override
			public Outbreak next() {
				
				if (!hasNext()) throw new NoSuchElementException("Iterator exhausted");
				
				if (exec == 0) {
					// Click back to new setup
					setupBuilder = new ExecutionBuilder(setups.get(setup));
					setupBuilder.setupOutbreak(urnBase);
				}
				
				ExecutionBuilder builder2 = setupBuilder.copy();
				ExecutionConfiguration exCfg = executions.get(exec); 
				builder2.baselineModel(exCfg);
				builder2.initialiseStatus(exCfg);
				
				if (exec >= executions.size()) {
					setup += 1;
					exec = 0;
				} else {
					exec += 1;
				}
				
				return builder2.build();
			}
			
		};
		
		halt = halt && build.hasNext(); 
		
		do {
			
			while (queue.size() < CACHE_SIZE & build.hasNext()) {
				if (SimulationMonitor.freeMem() > objSize) {
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
			
			while (!halt && queue.size() >= CACHE_SIZE) {
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
