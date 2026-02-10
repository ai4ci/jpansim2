/**
 * 
 */
package io.github.ai4ci.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the DelayDistribution class.
 * Tests cover all major functionality including probability distributions,
 * statistical properties, and edge cases.
 */
class TestDelayDistribution {

	private DelayDistribution simpleDist;
	private DelayDistribution emptyDist;
	
	@BeforeEach
	void setUp() {
		// Simple distribution: [1, 2, 3] with pAffected = 1.0
		simpleDist = DelayDistribution.unnormalised(1.0, 2.0, 3.0);
		emptyDist = DelayDistribution.empty();
	}
	
	@Test
	void testEmptyDistribution() {
		assertNotNull(emptyDist);
		assertEquals(0, emptyDist.size());
		assertEquals(0, emptyDist.density().length);
		assertEquals(0, emptyDist.condDensity().length);
		assertEquals(0, emptyDist.survival().length);
		assertEquals(0, emptyDist.hazard().length);
	}
	
	@Test
	void testSimpleDistributionCreation() {
		assertNotNull(simpleDist);
		assertEquals(3, simpleDist.size());
		assertArrayEquals(new double[]{1.0, 2.0, 3.0}, simpleDist.getProfile(), 1e-10);
		assertEquals(1.0, simpleDist.getPAffected(), 1e-10);
	}
	
	@Test
	void testTotalCalculation() {
		assertEquals(6.0, simpleDist.total(), 1e-10);
		assertEquals(0.0, emptyDist.total(), 1e-10);
	}
	
	@Test
	void testDensityCalculation() {
		double[] expectedDensity = {1.0/6.0, 2.0/6.0, 3.0/6.0};
		assertArrayEquals(expectedDensity, simpleDist.density(), 1e-10);
		
		// Test that sum of densities equals pAffected
		assertEquals(1.0, sum(simpleDist.density()), 1e-10);
	}
	
	@Test
	void testConditionalDensityCalculation() {
		double[] expectedCondDensity = {1.0/6.0, 2.0/6.0, 3.0/6.0};
		assertArrayEquals(expectedCondDensity, simpleDist.condDensity(), 1e-10);
		
		// Test that sum of conditional densities equals 1
		assertEquals(1.0, sum(simpleDist.condDensity()), 1e-10);
	}
	
	@Test
	void testSurvivalFunction() {
		double[] expectedSurvival = {
			1.0 - 1.0/6.0,
			1.0 - 1.0/6.0 - 2.0/6.0,
			1.0 - 1.0/6.0 - 2.0/6.0 - 3.0/6.0
		};
		assertArrayEquals(expectedSurvival, simpleDist.survival(), 1e-10);
		
		// Survival should be non-increasing
		double[] survival = simpleDist.survival();
		for (int i = 1; i < survival.length; i++) {
			assertTrue(survival[i] <= survival[i-1]);
		}
	}
	
	@Test
	void testHazardFunction() {
		double[] survival = simpleDist.survival();
		double[] density = simpleDist.density();
		double[] expectedHazard = {
			density[0] / 1.0,
			density[1] / survival[0],
			density[2] / survival[1]
		};
		assertArrayEquals(expectedHazard, simpleDist.hazard(), 1e-10);
	}
	
	@Test
	void testIndividualAccessors() {
		// Test profile accessor
		assertEquals(1.0, simpleDist.profile(0), 1e-10);
		assertEquals(2.0, simpleDist.profile(1), 1e-10);
		assertEquals(3.0, simpleDist.profile(2), 1e-10);
		assertEquals(0.0, simpleDist.profile(3), 1e-10); // Out of bounds
		assertEquals(0.0, simpleDist.profile(-1), 1e-10); // Out of bounds
		
		// Test density accessor
		double[] density = simpleDist.density();
		for (int i = 0; i < density.length; i++) {
			assertEquals(density[i], simpleDist.density(i), 1e-10);
		}
		assertEquals(0.0, simpleDist.density(3), 1e-10); // Out of bounds
		
		// Test conditional density accessor
		double[] condDensity = simpleDist.condDensity();
		for (int i = 0; i < condDensity.length; i++) {			assertEquals(condDensity[i], simpleDist.condDensity(i), 1e-10);
		}
		
		// Test hazard accessor
		double[] hazard = simpleDist.hazard();
		for (int i = 0; i < hazard.length; i++) {
			assertEquals(hazard[i], simpleDist.hazard(i), 1e-10);
		}
	}
	
	@Test
	void testMeanCalculation() {
		// For [1,2,3] with weights [1/6, 2/6, 3/6]
		// Expected mean = 0*(1/6) + 1*(2/6) + 2*(3/6) = (0 + 2 + 6)/6 = 8/6 ≈ 1.333
		assertEquals(8.0/6.0, simpleDist.mean(), 1e-10);
	}
	
	@Test
	void testCumulativeFunction() {
		// CDF should be 1 - survival
		double[] survival = simpleDist.survival();
		for (int i = 0; i < survival.length; i++) {
			assertEquals(1.0 - survival[i], simpleDist.cumulative(i), 1e-10);
		}
		
		// Test boundary conditions
		assertEquals(0.0, simpleDist.cumulative(-1), 1e-10);
		assertEquals(1.0, simpleDist.cumulative(3), 1e-10);
		assertEquals(1.0, simpleDist.cumulative(100), 1e-10);
	}
	
	@Test
	void testExpectedValue() {
		// For sample size 1: sum(i * density[i])
		double manualExpected = 0.0;
		double[] density = simpleDist.density();
		for (int i = 0; i < density.length; i++) {
			manualExpected += i * density[i];
		}
		assertEquals(manualExpected, simpleDist.expected(), 1e-10);
		
		// For sample size 10
		assertEquals(manualExpected * 10.0, simpleDist.expected(10.0), 1e-10);
	}
	
	@Test
	void testConditionedOn() {
		DelayDistribution halfAffected = simpleDist.conditionedOn(0.5);
		assertEquals(0.5, halfAffected.getPAffected(), 1e-10);
		
		// Densities should be half of original
		double[] originalDensity = simpleDist.density();
		double[] halfDensity = halfAffected.density();
		for (int i = 0; i < originalDensity.length; i++) {
			assertEquals(originalDensity[i] * 0.5, halfDensity[i], 1e-10);
		}
	}
	
	@Test
	void testToString() {
		String str = simpleDist.toString();
		assertTrue(str.startsWith("P(["));
		assertTrue(str.contains("1.0"));
		assertTrue(str.endsWith("|1.0)"));
	}
	
	@Test
	void testAffectedMethod() {
		// affected(i) should equal cumulative(i)
		for (int i = 0; i < simpleDist.size(); i++) {
			assertEquals(simpleDist.cumulative(i), simpleDist.affected(i), 1e-10);
		}
	}
	
	@Test
	void testConvolutionMethods() {
		double[] input = {1.0, 2.0, 3.0, 4.0};
		
		// Convolve with profile
		double[] convolvedProfile = simpleDist.convolveProfile(input);
		assertEquals(input.length, convolvedProfile.length);
		
		// Convolve with density
		double[] convolvedDensity = simpleDist.convolveDensity(input);
		assertEquals(input.length, convolvedDensity.length);
		
		// Convolution with density should be probability-weighted
		assertTrue(sum(convolvedDensity) < sum(convolvedProfile));
	}
	
	@Test
	void testStaticFactoryMethods() {
		// Test unnormalised with trailing zeros
		DelayDistribution withZeros = DelayDistribution.unnormalised(1.0, 2.0, 0.0, 0.0);
		assertArrayEquals(new double[]{1.0, 2.0}, withZeros.getProfile(), 1e-10);
		
		// Test empty
		DelayDistribution empty = DelayDistribution.empty();
		assertEquals(0, empty.size());
	}
	
	@Test
	void testDiscretisedGamma() {
		DelayDistribution gammaDist = DelayDistribution.discretisedGamma(5.0, 2.0);
		assertNotNull(gammaDist);
		assertTrue(gammaDist.size() > 0);
		
		// Sum of conditional densities should be approximately 1
		assertEquals(1.0, sum(gammaDist.condDensity()), 0.01);
		
		// Mean should be approximately the specified mean
		assertEquals(5.0, gammaDist.mean(), 1.0); // Allow some tolerance for discretisation
	}
	
	@Test
	void testGetQuantile() {
		// For simple distribution, quantiles should match cumulative
		for (double q : new double[]{0.1, 0.5, 0.9}) {
			int quantile = simpleDist.getQuantile(q);
			assertTrue(quantile >= 0 && quantile <= simpleDist.size());
			
			// The quantile should be the smallest index where cumulative > q
			if (quantile > 0) {
				assertTrue(simpleDist.cumulative(quantile - 1) <= q);
			}
			assertTrue(simpleDist.cumulative(quantile) > q || quantile == simpleDist.size());
		}
	}
	
	@Test
	void testTrimUtilities() {
		double[] testArray = {1.0, 2.0, 3.0, 0.0, 0.0};
		
		// Test trimZeros
		double[] trimmed = DelayDistribution.trimZeros(testArray);
		assertArrayEquals(new double[]{1.0, 2.0, 3.0}, trimmed, 1e-10);
		
		// Test trimTail with absolute tolerance
		double[] tailTrimmed = DelayDistribution.trimTail(testArray, 5.0, true);
		assertTrue(tailTrimmed.length <= testArray.length);
	}
	
	@Test
	void testEdgeCases() {
		// Test single element distribution
		DelayDistribution single = DelayDistribution.unnormalised(5.0);
		assertEquals(1, single.size());
		assertEquals(1.0, single.density()[0], 1e-10);
		assertEquals(0.0, single.survival()[0], 1e-10); // All affected immediately
		
		// Test with pAffected < 1
		DelayDistribution partial = DelayDistribution.unnormalised(1.0, 1.0).conditionedOn(0.5);
		assertEquals(0.5, sum(partial.density()), 1e-10);
		assertEquals(1.0, sum(partial.condDensity()), 1e-10);
	}
	
	// Helper method to sum array elements
	private double sum(double[] array) {
		double total = 0.0;
		for (double value : array) {
			total += value;
		}
		return total;
	}
}
