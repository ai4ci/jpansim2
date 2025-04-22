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
import io.github.ai4ci.config.SetupConfiguration;

/**
 * Configuration of simulations, setup and baselining. This runs as a 
 * deamon thread and caches  
 */
public class SimulationFactory extends Thread {

	public static int CACHE_SIZE = 2;
	
	static Logger log = LoggerFactory.getLogger(SimulationFactory.class);
	
	volatile Queue<Outbreak> queue; 
	volatile boolean halt;
	volatile boolean complete;
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
		return queue.poll();
	}
	
	public void halt() {
		log.debug("Requesting simulation factory shutdown");
		this.halt = true;
	}
	
	public boolean completed() {
		return this.complete;
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
			ExperimentBuilder setupBuilder;
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
					setupBuilder = new ExperimentBuilder();
					setupBuilder.setupOutbreak(setups.get(setup), urnBase);
				}
				
				ExperimentBuilder builder2 = setupBuilder.copy();
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
			
			if (!halt) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					halt = true;
				}
			}
			
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
