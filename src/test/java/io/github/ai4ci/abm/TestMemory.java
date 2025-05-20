package io.github.ai4ci.abm;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.flow.SimulationMonitor;

public class TestMemory {

	@Test
	void testFreeMem() throws IOException {
		System.out.println(SimulationMonitor.freeMemG());
	}
	
}
