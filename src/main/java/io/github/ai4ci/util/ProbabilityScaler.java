package io.github.ai4ci.util;

import java.util.Arrays;

/**
 * Utility class for scaling probabilities using power transformations.
 * Provides methods to find the appropriate scaling factor k such that
 * the scaled probabilities sum to a desired target, as well as direct
 * scaling of probabilities using a given k.
 *
 * The main method findScalingFactor uses Newton-Raphson iteration with
 * a bisection fallback to robustly solve for the scaling factor.
 *
 * Example usage:
 * <pre>
 * double[] probs = {0.1, 0.5, 0.4};
 * double targetSum = 2.0;
 * double k = ProbabilityScaler.findScalingFactor(probs, targetSum);
 * double[] scaledProbs = ProbabilityScaler.scaleProbabilities(probs, k);
 * </pre>
 *
 * @author Rob Challen
 */
public class ProbabilityScaler {

    /**
     * Scales probabilities using power transformation: q_i = p_i^(1/k)
     * Finds k such that sum(q_i) = targetSum using Newton-Raphson with bisection fallback.
     *
     * @param probabilities Array of probabilities in [0, 1]
     * @param targetSum     Desired sum of scaled probabilities (must be in feasible range)
     * @param tolerance     Convergence tolerance (default 1e-8)
     * @param maxIterations Maximum iterations for solver (default 50)
     * @return Scaling factor k > 0 such that sum(p_i^(1/k)) ≈ targetSum
     * @throws IllegalArgumentException if inputs are invalid or targetSum is infeasible
     */
    public static double findScalingFactor(double[] probabilities, double targetSum,
                                           double tolerance, int maxIterations) {
        validateInputs(probabilities, targetSum);

        int n = probabilities.length;
        double sumOriginal = Arrays.stream(probabilities).sum();

        // Special case: targetSum equals original sum → k = 1
        if (Math.abs(targetSum - sumOriginal) < tolerance) {
            return 1.0;
        }

        // Compute feasible range [S_min, S_max]
        double sMin = computeMinSum(probabilities);
        double sMax = n; // As alpha → 0+, p_i^alpha → 1 for all p_i > 0

        if (targetSum <= sMin + tolerance) {
            throw new IllegalArgumentException(
                String.format("Target sum %.6f is below feasible minimum %.6f", targetSum, sMin));
        }
        if (targetSum >= sMax - tolerance) {
            throw new IllegalArgumentException(
                String.format("Target sum %.6f is above feasible maximum %.6f", targetSum, sMax));
        }

        // Solve for alpha = 1/k using Newton-Raphson
        double alpha = solveForAlpha(probabilities, targetSum, tolerance, maxIterations);

        return 1.0 / alpha;
    }

    /**
     * Convenience method with default tolerance and iterations.
     * @see #findScalingFactor(double[], double, double, int)
     * @param probabilities Array of probabilities in [0, 1]
     * @param targetSum Desired sum of scaled probabilities (must be in feasible range)
     * @return Scaling factor k > 0 such that sum(p_i^(1/k)) ≈ targetSum
     * @throws IllegalArgumentException if inputs are invalid or targetSum is infeasible
     * 
     */
    public static double findScalingFactor(double[] probabilities, double targetSum) {
        return findScalingFactor(probabilities, targetSum, 1e-8, 50);
    }

    /**
	 * Scales a single probability using power transformation: q = p^(1/k)
	 * @param probabilities Probability in [0, 1]
	 * @param k Scaling factor (must be positive)
	 * @return Scaled probability q = p^(1/k)
	 * @throws IllegalArgumentException if k is not positive
	 */
    public static double scaleProbability(double probabilities, double k) {
    	double alpha = 1.0 / k;
    	double scaled = Math.pow(probabilities, alpha);
    	return(scaled);
    }
    
    /**
     * Applies power scaling to probabilities using given k: q_i = p_i^(1/k)
     * @param probabilities Array of probabilities in [0, 1]
     * @param k Scaling factor (must be positive)
     * @return Scaled probabilities array where q_i = p_i^(1/k)
     * @throws IllegalArgumentException if k is not positive
     * 
     */
    public static double[] scaleProbabilities(double[] probabilities, double k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Scaling factor k must be positive");
        }
        double alpha = 1.0 / k;
        double[] scaled = new double[probabilities.length];
        for (int i = 0; i < probabilities.length; i++) {
            scaled[i] = Math.pow(probabilities[i], alpha);
        }
        return scaled;
    }

    /**
     * Direct method: scales probabilities to achieve target sum in one call.
     * Internally finds the appropriate scaling factor k and applies it.
     * @throws IllegalArgumentException if inputs are invalid or targetSum is infeasible
     * @return Scaled probabilities array where sum(q_i) ≈ targetSum
     * @param probabilities Array of probabilities in [0, 1]
     * @param targetSum Desired sum of scaled probabilities (must be in feasible range)
     */
    public static double[] scaleToTargetSum(double[] probabilities, double targetSum) {
        double k = findScalingFactor(probabilities, targetSum);
        return scaleProbabilities(probabilities, k);
    }

    // ------------------------ PRIVATE HELPERS ------------------------

    private static void validateInputs(double[] p, double targetSum) {
        if (p == null || p.length == 0) {
            throw new IllegalArgumentException("Probabilities array cannot be null or empty");
        }
        if (targetSum <= 0 || targetSum >= p.length) {
            throw new IllegalArgumentException(
                String.format("Target sum must be in (0, %d), got %.6f", p.length, targetSum));
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i] < 0 || p[i] > 1) {
                throw new IllegalArgumentException(
                    String.format("Probability at index %d is %.6f; must be in [0, 1]", i, p[i]));
            }
        }
    }

    private static double computeMinSum(double[] p) {
        // As alpha → ∞: p_i^alpha → 0 if p_i < 1, → 1 if p_i == 1
        double sum = 0.0;
        for (double pi : p) {
            if (Math.abs(pi - 1.0) < 1e-12) sum += 1.0;
            // p_i = 0 contributes 0 for any alpha > 0
        }
        return sum;
    }

    private static double solveForAlpha(double[] p, double targetSum, double tol, int maxIter) {
        // Initial guess using geometric mean approximation
        double logSum = 0.0;
        int nonZeroCount = 0;
        for (double pi : p) {
            if (pi > 1e-15) { // Avoid log(0)
                logSum += Math.log(pi);
                nonZeroCount++;
            }
        }
        double pGeom = (nonZeroCount > 0) ? Math.exp(logSum / nonZeroCount) : 1e-15;
        double alpha = Math.log(targetSum / p.length) / Math.log(pGeom);
        alpha = Math.max(1e-6, Math.min(100.0, alpha)); // Clamp to reasonable range

        // Newton-Raphson
        for (int iter = 0; iter < maxIter; iter++) {
            double sum = 0.0;
            double deriv = 0.0;
            for (double pi : p) {
                if (pi < 1e-15) continue; // 0^alpha = 0, derivative = 0
                double term = Math.pow(pi, alpha);
                sum += term;
                deriv += term * Math.log(pi); // d/dα [p^α] = p^α * log(p)
            }
            double error = sum - targetSum;
            if (Math.abs(error) < tol) {
                return alpha;
            }
            if (Math.abs(deriv) < 1e-12) break; // Derivative too small → switch to bisection

            double alphaNew = alpha - error / deriv;
            if (alphaNew <= 0) break; // Invalid alpha → switch to bisection
            alpha = alphaNew;
        }

        // Fallback to bisection (robust but slower)
        return bisectionSolve(p, targetSum, tol, maxIter);
    }

    private static double bisectionSolve(double[] p, double targetSum, double tol, int maxIter) {
        double alphaLow = 1e-8;   // Near 0 → sum ≈ n
        double alphaHigh = 100.0; // Large → sum ≈ count(p_i == 1)

        // Expand upper bound if needed
        while (computeSum(p, alphaHigh) > targetSum && alphaHigh < 1e8) {
            alphaHigh *= 2;
        }

        for (int iter = 0; iter < maxIter; iter++) {
            double alphaMid = (alphaLow + alphaHigh) / 2.0;
            double sumMid = computeSum(p, alphaMid);
            double error = sumMid - targetSum;

            if (Math.abs(error) < tol) {
                return alphaMid;
            }
            if (error > 0) {
                alphaLow = alphaMid; // Sum too large → increase alpha
            } else {
                alphaHigh = alphaMid; // Sum too small → decrease alpha
            }
        }
        return (alphaLow + alphaHigh) / 2.0; // Best effort
    }

    private static double computeSum(double[] p, double alpha) {
        double sum = 0.0;
        for (double pi : p) {
            if (pi > 1e-15) {
                sum += Math.pow(pi, alpha);
            }
            // pi ≈ 0 contributes 0 for alpha > 0
        }
        return sum;
    }

    
}
