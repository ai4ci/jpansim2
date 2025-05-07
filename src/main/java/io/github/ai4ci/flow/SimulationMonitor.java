package io.github.ai4ci.flow;

public interface SimulationMonitor {

	static long freeMem() {
		Runtime runtime = Runtime.getRuntime();
		long allocatedMemory = runtime.totalMemory() - runtime.freeMemory();
		// allocatedMemory = allocatedMemory > maxMem ? maxMem : allocatedMemory;  
		return (runtime.maxMemory() - allocatedMemory);
	}

}
