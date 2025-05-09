package io.github.ai4ci.flow;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.mechanics.Updater;
import io.github.ai4ci.util.PauseableThread;

/**
 * Executes a single simulation to completion under supervision by the monitor.
 * The monitor 
 */
public class SimulationExecutor extends PauseableThread {

	int step = 0;
	int toStep;
	SimulationMonitor mon;
	
	Outbreak outbreak;
	Updater updater;
	StateExporter exporter;
	
	public SimulationExecutor(SimulationMonitor mon, Outbreak outbreak, StateExporter exporter, int toStep) {
		super("Simulation runner: "+outbreak.getUrn(), 9);
		this.mon = mon;
		this.outbreak = outbreak;
		this.exporter = exporter;
		this.toStep = toStep;
		this.updater = new Updater();
	}
	
	@Override
	public void setup() {
		// Nothing to do here
	}

	@Override
	public void doLoop() {
		step += 1;
		exporter.export(outbreak);
		updater.update(outbreak);
	}

	@Override
	public boolean isComplete() {
		return step >= toStep;
	}

	@Override
	public void shutdown(boolean completedNormally) {
		exporter.finalise(outbreak);
		mon.notifyExecutionComplete(this);
	}
	
	public String status() {
		return String.format(
				"%d/%d [%1.0f%%] - %s - %s",
				step,
				toStep,
				((float) step)/toStep*100,
				super.status(),
				outbreak.getUrn());
	}
}
