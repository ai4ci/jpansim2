package io.github.ai4ci.config;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import io.github.ai4ci.util.Sampler;

public class TestTestParameters {

    @Test
    void testApplyNoiseWithZeroSignal() {
        Sampler rng = Sampler.getSampler();
        double falsePositiveRate = IntStream.range(0, 100000)
            .mapToDouble(i -> TestParameters.applyNoise(0.0, 0.8, 0.95, 1.0, rng))
            .filter(signal -> signal > 1.0)
            .count() / 100000.0;
            
        assertEquals(0.05, falsePositiveRate, 0.01, 
            "False positive rate should match (1 - specificity)");
    }

    @Test
    void testApplyNoiseWithFullSignal() {
        Sampler rng = Sampler.getSampler();
        double falseNegativeRate = IntStream.range(0, 100000)
            .mapToDouble(i -> TestParameters.applyNoise(1.0, 0.8, 0.95, 1.0, rng))
            .filter(signal -> signal < 1.0)
            .count() / 100000.0;
            
        assertEquals(0.2, falseNegativeRate, 0.01, 
            "False negative rate should match (1 - sensitivity)");
    }

    @Test
    void testApplyNoiseWithLimitOfDetection() {
        Sampler rng = Sampler.getSampler();
        double detectionRate = IntStream.range(0, 100000)
            .mapToDouble(i -> TestParameters.applyNoise(0.5, 0.8, 0.95, 0.5, rng))
            .filter(signal -> signal >= 0.5)
            .count() / 100000.0;
            
        assertEquals(0.8, detectionRate, 0.01, 
            "Detection rate at limit should match sensitivity");
    }

    @Test
    void testPositiveLikelihoodRatio() {
        TestParameters params = ImmutableTestParameters.builder()
            .setTestName("TEST")
            .setSensitivity(0.9)
            .setSpecificity(0.95)
            .setMeanTestDelay(1.0)
            .setSdTestDelay(0.5)
            .setLimitOfDetection(1.0)
            .build();
            
        assertEquals(18.0, params.positiveLikelihoodRatio(), 0.001,
            "LR+ should be sensitivity/(1-specificity)");
    }

    @Test
    void testNegativeLikelihoodRatio() {
        TestParameters params = ImmutableTestParameters.builder()
            .setTestName("TEST")
            .setSensitivity(0.9)
            .setSpecificity(0.95)
            .setMeanTestDelay(1.0)
            .setSdTestDelay(0.5)
            .setLimitOfDetection(1.0)
            .build();
            
        assertEquals(0.105, params.negativeLikelihoodRatio(), 0.001,
            "LR- should be (1-sensitivity)/specificity");
    }

    @Test
    void testPositiveLikelihoodRatioEdgeCase() {
        TestParameters params = ImmutableTestParameters.builder()
            .setTestName("TEST")
            .setSensitivity(0.9)
            .setSpecificity(1.0) // Edge case
            .setMeanTestDelay(1.0)
            .setSdTestDelay(0.5)
            .setLimitOfDetection(1.0)
            .build();
            
        assertEquals(Double.POSITIVE_INFINITY, params.positiveLikelihoodRatio(),
            "LR+ should be infinite when specificity is 1");
    }

    @Test
    void testNegativeLikelihoodRatioEdgeCase() {
        TestParameters params = ImmutableTestParameters.builder()
            .setTestName("TEST")
            .setSensitivity(0.9)
            .setSpecificity(0.0) // Edge case
            .setMeanTestDelay(1.0)
            .setSdTestDelay(0.5)
            .setLimitOfDetection(1.0)
            .build();
            
        assertEquals(Double.POSITIVE_INFINITY, params.negativeLikelihoodRatio(),
            "LR- should be infinite when specificity is 0");
    }

//    @Test
//    void testApplyNoiseEdgeCase() {
//        Sampler rng = Sampler.getSampler();
//        assertThrows(ArithmeticException.class, () -> 
//            TestParameters.applyNoise(0.5, 0.5, 0.5, 1.0, rng),
//            "Should throw when sensitivity + specificity = 1");
//    }
}