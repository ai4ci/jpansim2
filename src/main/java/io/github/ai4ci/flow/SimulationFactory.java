package io.github.ai4ci.flow;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Cloner;
import io.github.ai4ci.util.PauseableThread;

/**
 * Configuration, setup and baselining of simulations in this batch. This runs as a 
 * daemon thread and caches  
 */
public class SimulationFactory extends PauseableThread {

	public static int CACHE_SIZE = 2;
	
	static Logger log = LoggerFactory.getLogger(SimulationFactory.class);
	
	SimulationMonitor mon;
	volatile ConcurrentLinkedQueue<Outbreak> queue; 
	volatile AtomicInteger cacheSize = new AtomicInteger(CACHE_SIZE);
//	List<SetupConfiguration> setups;
//	List<ExecutionConfiguration> executions;
	String urnBase;
	long objSize=-1;
	Iterator<Outbreak> builder;
	int setupStage = 0;
	int setupSize;
	int execStage = 0;
	int execSize;
	String activity = "starting";
	
	
	public static SimulationFactory startFactory(
			List<SetupConfiguration> setups, 
			List<ExecutionConfiguration> executions,
			String urnBase,
			SimulationMonitor mon
		) {
		
		SimulationFactory tmp = new SimulationFactory(setups, executions, urnBase, mon);
		tmp.start();
		return tmp;
	}
	
	private SimulationFactory(List<SetupConfiguration> setups, 
			List<ExecutionConfiguration> executions, String urnBase,
			SimulationMonitor mon) {
		
		super("Simulation factory",10);
		
		this.queue = new ConcurrentLinkedQueue<>();
		this.setupSize = setups.size();
		this.execSize = executions.size();
		this.urnBase = urnBase;
		this.mon = mon;
		
		this.builder = new Iterator<Outbreak>() {

			ExecutionBuilder setupBuilder;
			
			int count = 0;
			@Override
			public boolean hasNext() {
				return count < setups.size() * executions.size();
			}

			@Override
			public Outbreak next() {
				
				int setup = count / executions.size();
				int exec = count % executions.size();
				
				if (!hasNext()) throw new NoSuchElementException("Iterator exhausted");
				
				SimulationFactory.this.execStage = exec;
				SimulationFactory.this.setupStage = setup;
				
				if (exec == 0) {
					// Click back to new setup
					SetupConfiguration setupCfg = setups.get(setup);
					SimulationFactory.this.activity = "building new model: "+setupCfg.getName()+":"+setupCfg.getReplicate();
					setupBuilder = new ExecutionBuilder(setupCfg);
					setupBuilder.setupOutbreak(urnBase);
				}
				
				{
					ExecutionBuilder builder2;
					if (execSize > 1) {
						if (objSize == -1) {
							SimulationFactory.this.activity = "sizing model";
							try {
								objSize = Cloner.estimateSize(setupBuilder.outbreak);
								log.info("Each simulation takes up about "+String.format("%1.0f", ((double) objSize)/(1024*1024))+"Mb before configuration.");
							} catch (Exception e) {
								log.error("Could't establish initial simulation size. It is possibly too big to be cloned.");
								objSize = 20*1024*1024;
							}
						}
						SimulationFactory.this.activity = "cloning model";
						builder2 = setupBuilder.copy(objSize);
					} else {
						builder2 = setupBuilder;
					}
					
					ExecutionConfiguration exCfg = executions.get(exec);
					SimulationFactory.this.activity = "initialising model baseline: "+exCfg.getName()+":"+exCfg.getReplicate();
					builder2.baselineModel(exCfg);
					SimulationFactory.this.activity = "initialising model state: "+exCfg.getName()+":"+exCfg.getReplicate();
					builder2.initialiseStatus(exCfg);
					SimulationFactory.this.activity = "model ready: "+exCfg.getName()+":"+exCfg.getReplicate();
					
					count += 1;
					
					return builder2.build();
				}
			}
			
		};
	}
	
	
	
	@Override
	public void setup() {
		// nothing to do here
	}

	public void unpause() {
		if (!cacheFull()) super.unpause();
	}
	
	public boolean cacheFull() {
		return this.queue.size() >= this.cacheSize.get();
	}
	
	@Override
	public void doLoop() {
		if (cacheFull()) {
			this.pause();
		} else {
			Outbreak tmp = builder.next();
			queue.add(tmp);
			mon.notifyFactoryReady(this);
		}
		
	}

	@Override
	public boolean isComplete() {
		return !this.builder.hasNext();
	}

	@Override
	public void shutdown(boolean completedNormally) {
		if (!completedNormally) {
			log.error("Simulation factory did not complete all simulations.");
			this.activity = "factory terminated without completing";
		} else {
			this.activity = "all simulations built";
		}
		
	}
	
	/** 
	 * There are no more simulations available. 
	 * @return true if all the simulations have been created and consumed.
	 */
	public boolean finished() {
		return queue.isEmpty() && this.isComplete();
	}
	
	/**
	 * @return true if the factory is ready to deliver a configured simulation for execution
	 */
	public boolean ready() {
		return !queue.isEmpty();
	}
	
	/**
	 * @return a configured simulation or null if the factory is finished or
	 * not ready.
	 * @param resume if paused (because the cache is full)
	 */
	Outbreak deliver() {
		Outbreak out = queue.poll();
		return out;
	}
	
	/**
	 * Increase simulation queue if loads of memory and risk simulation waiting
	 * for factory.
	 * 
	 * <br> TODO: Parallelise the simulation factory. 
	 * The cache being part of the factory is a problem because we can't
	 * parallelise it. Increasing the cache size probably wont improve 
	 * performance if the factory is the limiting factor in the overall speed.
	 */
	public void increaseCacheSize(int items) {
		this.cacheSize.set(items);
	}
	
	/**
	 * @return the rough size of a configured simulation (before it is executed)
	 * if we know it yet.
	 */
	public OptionalLong getSimulationSize() {
		if (objSize == -1) return OptionalLong.empty();
		return OptionalLong.of(objSize);
	}
	
	public String status() {
		return String.format(
				"model %d/%d; execution %d/%d; queued %d/%d; %s - (%s)",
				this.setupStage+1,
				this.setupSize,
				this.execStage+1,
				this.execSize,
				this.queue.size(),
				this.cacheSize.get(),
				this.activity,
				super.status()
		);
	}
}
