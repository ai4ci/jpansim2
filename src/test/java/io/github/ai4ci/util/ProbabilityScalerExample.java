package io.github.ai4ci.util;

import java.util.Arrays;

public class ProbabilityScalerExample {

	// ------------------------ EXAMPLE USAGE ------------------------

    public static void main(String[] args) {
        System.out.println("=== Probability Power Scaling Examples ===\n");

        // Example 1: Increase sum (S > original sum)
        double[] p1 = {0.8, 0.6, 0.2};
        double S1 = 2.1;
        demoScaling(p1, S1);

        // Example 2: Decrease sum (S < original sum)
        double[] p2 = {0.9, 0.7, 0.5, 0.3};
        double S2 = 1.8;
        demoScaling(p2, S2);

        // Example 3: Edge case with p_i = 0 and 1
        double[] p3 = {1.0, 0.5, 0.0};
        double S3 = 1.7; // Feasible range: (1.0, 3.0)
        demoScaling(p3, S3);
    }

    private static void demoScaling(double[] p, double targetSum) {
        System.out.println("Original probabilities: " + Arrays.toString(p));
        System.out.printf("Original sum: %.6f%n", Arrays.stream(p).sum());
        System.out.printf("Target sum: %.6f%n", targetSum);

        try {
            double k = ProbabilityScaler.findScalingFactor(p, targetSum);
            double[] scaled = ProbabilityScaler.scaleProbabilities(p, k);
            double actualSum = Arrays.stream(scaled).sum();

            System.out.printf("Scaling factor k: %.6f (alpha = 1/k = %.6f)%n", k, 1.0/k);
            System.out.println("Scaled probabilities: " + Arrays.toString(scaled));
            System.out.printf("Actual sum: %.6f (error: %.2e)%n", actualSum, Math.abs(actualSum - targetSum));
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        System.out.println();
    }
	
}
