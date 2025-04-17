package io.github.ai4ci.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestDistribution {

	@Test
	void test() {
		System.out.println(
				SimpleDistribution.unimodalBeta(0.99, 0.01).sample()
				);
	}

	
	@Test
	void testConversion() {
		System.out.println(
				Conversions.probabilityFromRate(1.0/10)
				);
	}
}
