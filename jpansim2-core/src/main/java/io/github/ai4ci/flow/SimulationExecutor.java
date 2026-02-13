package io.github.ai4ci.flow;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.flow.mechanics.Updater;
import io.github.ai4ci.flow.output.SimulationExporter;
import io.github.ai4ci.util.PauseableThread;

/**
 * Executes a single simulation to completion under supervision by the monitor.
 * The monitor will throttle running simulations by pausing simulation threads
 * if memory is getting tight. The executor does the main loop of exporting data
 * from the simulation (via {@link SimulationExporter}) and then incrementing
 * the time of the simulation (via
 * {@link io.github.ai4ci.flow.mechanics.Updater}).
 */
public class SimulationExecutor extends PauseableThread {

	int step = 0;
	int toStep;
	SimulationMonitor mon;
	Outbreak outbreak;
	Updater updater;
	SimulationExporter exporter;

	/**
	 * Create a new simulation executor for the given outbreak, exporter and
	 * number
	 *
	 * @param mon      the monitor that will supervise this execution
	 * @param outbreak the outbreak to execute
	 * @param exporter the exporter to use for exporting data during execution
	 * @param toStep   the number of steps to execute (e.g., 100 for 100 days)
	 */
	public SimulationExecutor(
			SimulationMonitor mon, Outbreak outbreak, SimulationExporter exporter,
			int toStep
	) {
		super("Simulation runner: " + outbreak.getUrn(), 9);
		this.mon = mon;
		this.outbreak = outbreak;
		this.exporter = exporter;
		this.toStep = toStep;
		this.updater = new Updater();
	}

	@Override
	public void doLoop() {
		try {
			this.step += 1;
			this.exporter.export(this.outbreak);
			this.updater.update(this.outbreak);
		} catch (Exception e) {
			this.mon.handle(e);
			this.halt();
		}
	}

	@Override
	public boolean isComplete() { return this.step >= this.toStep; }

	@Override
	public void setup() {
		// Nothing to do here
	}

	@Override
	public void shutdown(boolean completedNormally) {
		this.exporter.finalise(this.outbreak);
		this.mon.notifyExecutionComplete(this);
	}

	@Override
	public String status() {
		return String.format(
				"%d/%d [%1.0f%%] - %s - %s", this.step, this.toStep,
				((float) this.step) / this.toStep * 100, super.status(),
				this.outbreak.getUrn()
		);
	}
}
