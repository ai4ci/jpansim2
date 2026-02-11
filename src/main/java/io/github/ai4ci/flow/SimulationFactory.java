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
import io.github.ai4ci.config.execution.ExecutionConfiguration;
import io.github.ai4ci.config.setup.SetupConfiguration;
import io.github.ai4ci.util.Cloner;
import io.github.ai4ci.util.PauseableThread;

/**
 * Factory for pre-configuring and caching simulation instances for batch
 * execution.
 *
 * <p>
 * This class operates as a daemon thread that prepares and caches configured
 * {@link Outbreak} simulations before they are executed by
 * {@link SimulationExecutor}. It handles the computationally intensive setup
 * phase separately from execution to optimize resource utilization and enable
 * parallel execution.
 *
 * <h2>Factory Workflow</h2>
 * <ol>
 * <li><b>Setup Configuration</b>: Creates base outbreak models from setup
 * configurations</li>
 * <li><b>Execution Configuration</b>: Applies execution parameters to create
 * runnable simulations</li>
 * <li><b>Caching</b>: Maintains a cache of ready-to-execute simulations</li>
 * <li><b>Delivery</b>: Provides simulations to {@link SimulationMonitor} for
 * execution</li>
 * </ol>
 *
 * <h2>Configuration Matrix</h2>
 * <p>
 * The factory generates simulations for all combinations of setup and execution
 * configurations, creating a complete experimental design matrix: \[
 * N_{\text{simulations}} = N_{\text{setups}} \times N_{\text{executions}} \]
 *
 * <h2>Caching Strategy</h2>
 * <p>
 * Uses a size-aware caching approach:
 * <ul>
 * <li>Default cache size: {@link SimulationFactory#CACHE_SIZE} simulations</li>
 * <li>Automatic memory estimation using
 * {@link Cloner#estimateSize(Object)}</li>
 * <li>Dynamic cache size adjustment based on available memory</li>
 * <li>Pause/resume behavior to prevent memory exhaustion</li>
 * </ul>
 *
 * <h2>Integration with SimulationMonitor</h2>
 * <p>
 * The factory coordinates closely with {@link SimulationMonitor}:
 * <ul>
 * <li>Notifies monitor when simulations are ready for execution</li>
 * <li>Responds to monitor requests for simulation delivery</li>
 * <li>Accepts cache size adjustments from monitor based on system
 * resources</li>
 * <li>Reports status and progress to monitor for oversight</li>
 * </ul>
 *
 * <h2>Performance Optimization</h2>
 * <p>
 * Key optimizations include:
 * <ul>
 * <li>Model cloning to avoid redundant setup computations</li>
 * <li>Memory-efficient caching with size awareness</li>
 * <li>Background thread operation to overlap setup with execution</li>
 * <li>Batch processing of configuration combinations</li>
 * </ul>
 *
 * @see SimulationMonitor
 * @see SimulationExecutor
 * @see Outbreak
 * @see SetupConfiguration
 * @see ExecutionConfiguration
 */
public class SimulationFactory extends PauseableThread {

	/** Default cache size for pre-configured simulations */
	public static int CACHE_SIZE = 2;

	static Logger log = LoggerFactory.getLogger(SimulationFactory.class);

	/**
	 * Starts the simulation factory with the provided configuration sets.
	 *
	 * <p>
	 * Creates a complete experimental design by generating simulations for all
	 * combinations of setup and execution configurations.
	 *
	 * @param setups     list of setup configurations for model structure
	 * @param executions list of execution configurations for run parameters
	 * @param urnBase    base URN pattern for simulation identification
	 * @param mon        the simulation monitor that will execute the generated
	 *                   simulations
	 * @return a started SimulationFactory instance
	 */
	public static SimulationFactory startFactory(
			List<SetupConfiguration> setups,
			List<ExecutionConfiguration> executions, String urnBase,
			SimulationMonitor mon
	) {

		SimulationFactory tmp = new SimulationFactory(
				setups, executions, urnBase, mon
		);
		tmp.start();
		return tmp;
	}

	SimulationMonitor mon;
	volatile ConcurrentLinkedQueue<Outbreak> queue;
	volatile AtomicInteger cacheSize = new AtomicInteger(CACHE_SIZE);
	String urnBase;
	long objSize = -1;
	Iterator<Outbreak> builder;
	int setupStage = 0;
	int setupSize;
	int execStage = 0;
	int execSize;

	String activity = "starting";

	/**
	 * Constructs a simulation factory for the given configuration sets.
	 *
	 * <p>
	 * Initializes the factory with configuration matrices and prepares the
	 * iterator that will generate all simulation combinations.
	 *
	 * @param setups     list of setup configurations
	 * @param executions list of execution configurations
	 * @param urnBase    base URN pattern for simulation identification
	 * @param mon        the simulation monitor for coordination
	 */
	private SimulationFactory(
			List<SetupConfiguration> setups,
			List<ExecutionConfiguration> executions, String urnBase,
			SimulationMonitor mon
	) {

		super("Simulation factory", 10);

		this.queue = new ConcurrentLinkedQueue<>();
		this.setupSize = setups.size();
		this.execSize = executions.size();
		this.urnBase = urnBase;
		this.mon = mon;

		this.builder = new Iterator<>() {

			ExecutionBuilder setupBuilder;

			int count = 0;

			@Override
			public boolean hasNext() {
				return this.count < setups.size() * executions.size();
			}

			@Override
			public Outbreak next() {

				int setup = this.count / executions.size();
				int exec = this.count % executions.size();

				if (!this.hasNext())
					throw new NoSuchElementException("Iterator exhausted");

				SimulationFactory.this.execStage = exec;
				SimulationFactory.this.setupStage = setup;

				if (exec == 0) {
					// Click back to new setup
					SetupConfiguration setupCfg = setups.get(setup);
					SimulationFactory.this.activity = "building new model: "
							+ setupCfg.getName() + ":" + setupCfg.getReplicate();
					this.setupBuilder = new ExecutionBuilder(setupCfg);
					this.setupBuilder.setupOutbreak(urnBase);
				}

				{
					ExecutionBuilder builder2;
					if (SimulationFactory.this.execSize > 1) {
						if (SimulationFactory.this.objSize == -1) {
							SimulationFactory.this.activity = "sizing model";
							try {
								SimulationFactory.this.objSize = Cloner
										.estimateSize(this.setupBuilder.outbreak);
								log.info(
										"Each simulation takes up about " + String.format(
												"%1.0f",
												((double) SimulationFactory.this.objSize)
														/ (1024 * 1024)
										) + "Mb before configuration."
								);
							} catch (Exception e) {
								log.error(
										"Could't establish initial simulation size. It is possibly too big to be cloned."
								);
								SimulationFactory.this.objSize = 20 * 1024 * 1024;
							}
						}
						SimulationFactory.this.activity = "cloning model";
						builder2 = this.setupBuilder
								.copy(SimulationFactory.this.objSize);
					} else {
						builder2 = this.setupBuilder;
					}

					ExecutionConfiguration exCfg = executions.get(exec);
					SimulationFactory.this.activity = "initialising model baseline: "
							+ exCfg.getName() + ":" + exCfg.getReplicate();
					builder2.baselineModel(exCfg);
					SimulationFactory.this.activity = "initialising model state: "
							+ exCfg.getName() + ":" + exCfg.getReplicate();
					builder2.initialiseStatus(exCfg);
					SimulationFactory.this.activity = "model ready: "
							+ exCfg.getName() + ":" + exCfg.getReplicate();

					this.count += 1;

					return builder2.build();
				}
			}

		};
	}

	/**
	 * Checks if the simulation cache is full.
	 *
	 * @return true if the cache has reached its configured capacity
	 */
	public boolean cacheFull() {
		return this.queue.size() >= this.cacheSize.get();
	}

	/**
	 * Delivers a configured simulation for execution.
	 *
	 * @return a configured simulation or null if factory is finished or not
	 *         ready
	 */
	Outbreak deliver() {
		Outbreak out = this.queue.poll();
		return out;
	}

	@Override
	public void doLoop() {
		if (this.cacheFull()) {
			this.pause();
		} else {
			try {
				Outbreak tmp = this.builder.next();
				this.queue.add(tmp);
				this.mon.notifyFactoryReady(this);
			} catch (Exception e) {
				this.mon.handle(e);
				this.halt();
			}
		}

	}

	/**
	 * Checks if the factory has completed all simulations and the queue is
	 * empty.
	 *
	 * @return true if all simulations have been created and consumed
	 */
	public boolean finished() {
		return this.queue.isEmpty() && this.isComplete();
	}

	/**
	 * Gets the estimated size of a configured simulation.
	 *
	 * @return the rough size of a configured simulation if known, empty
	 *         otherwise
	 */
	public OptionalLong getSimulationSize() {
		if (this.objSize == -1) return OptionalLong.empty();
		return OptionalLong.of(this.objSize);
	}

	/**
	 * Increases the simulation cache size to improve throughput.
	 *
	 * <p>
	 * Used when there is ample memory available and simulations are waiting for
	 * factory production. Larger cache sizes allow better overlap between setup
	 * and execution phases.
	 *
	 * @param items the new cache size capacity
	 */
	public void increaseCacheSize(int items) {
		this.cacheSize.set(items);
	}

	@Override
	public boolean isComplete() { return !this.builder.hasNext(); }

	/**
	 * Checks if the factory has ready-to-execute simulations available.
	 *
	 * @return true if the factory has simulations ready for execution
	 */
	public boolean ready() {
		return !this.queue.isEmpty();
	}

	@Override
	public void setup() {
		// nothing to do here
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
	 * Provides detailed status information about factory progress.
	 *
	 * @return formatted status string with progress metrics and current activity
	 */
	@Override
	public String status() {
		return String.format(
				"model %d/%d; execution %d/%d; queued %d/%d; %s - (%s)",
				this.setupStage + 1, this.setupSize, this.execStage + 1,
				this.execSize, this.queue.size(), this.cacheSize.get(),
				this.activity, super.status()
		);
	}

	/**
	 * Unpauses the factory if cache is not full.
	 *
	 * <p>
	 * Only resumes production if there is capacity in the cache to avoid memory
	 * exhaustion from overproduction.
	 */
	@Override
	public void unpause() {
		if (!this.cacheFull()) { super.unpause(); }
	}
}
