package io.github.ai4ci.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.util.FastMath;
import org.junit.jupiter.api.Test;

public class TestEmpiricalFunction {

    private static final int NUM_POINTS = 30; // Number of sample points
    private static final double TOLERANCE_VALUE = 1e-2; // Allow small interpolation error
    private static final double TOLERANCE_DERIVATIVE = 1e-1; // Derivatives are less accurate

    /**
     * Test interpolation of sin(x) over [0, π]
     * Should be well-approximated by cubic spline
     */
    @Test
    public void testSinInterpolation() {
        double minX = 0.0;
        double maxX = FastMath.PI;

        double[] x = linspace(minX, maxX, NUM_POINTS);
        double[] y = Arrays.stream(x).map(Math::sin).toArray();

        EmpiricalFunction func = ImmutableEmpiricalFunction.builder()
                .setX(x)
                .setY(y)
                .build();

        // Evaluate at 100 points
        double[] testX = linspace(minX+0.01, maxX-0.01, 100);
        for (double xi : testX) {
            double interpolated = func.value(xi);
            double actual = Math.sin(xi);
            assertEquals(actual, interpolated, TOLERANCE_VALUE,
                    String.format("sin(%f) should be approximated well", xi));
        }

        // Test derivative: cos(x) = d/dx sin(x)
        for (double xi : testX) {
            double interpolatedDerivative = func.differential(xi);
            double actualDerivative = Math.cos(xi);
            assertEquals(actualDerivative, interpolatedDerivative, TOLERANCE_DERIVATIVE,
                    String.format("d/dx sin(%f) should be ~cos(%f)", xi, xi));
        }
    }

    /**
     * Test exp(x) with LinkFunction.LOG
     * In log-space: log(exp(x)) = x → linear → perfectly splinable
     */
    @Test
    public void testExpWithLogLink() {
        double minX = 0.0;
        double maxX = 2.0;

        double[] x = linspace(minX, maxX, NUM_POINTS);
        double[] y = Arrays.stream(x).map(Math::exp).toArray();

        EmpiricalFunction func = ImmutableEmpiricalFunction.builder()
                .setX(x)
                .setY(y)
                .setLink(LinkFunction.LOG)
                .build();

        // Since log(y) = x, the interpolation in log-space is linear → exact reconstruction
        double[] testX = linspace(minX+0.1, maxX-0.1, 100);
        for (double xi : testX) {
            double interpolated = func.value(xi);
            double actual = Math.exp(xi);
            assertEquals(actual, interpolated, TOLERANCE_VALUE,
                    String.format("exp(%f) with LOG link should be accurate", xi));
        }

        // Derivative: d/dx exp(x) = exp(x)
        for (double xi : testX) {
            double interpolatedDerivative = func.differential(xi);
            double actualDerivative = Math.exp(xi);
            assertEquals(actualDerivative, interpolatedDerivative, TOLERANCE_DERIVATIVE * actualDerivative,
                    String.format("d/dx exp(%f) should be ~exp(%f)", xi, xi));
        }
    }

    /**
     * Test a quadratic polynomial: f(x) = 2x^2 - 3x + 1
     * Cubic splines should represent quadratics exactly with enough points
     */
    @Test
    public void testQuadraticPolynomial() {
        PolynomialFunction poly = new PolynomialFunction(new double[]{1.0, -3.0, 2.0}); // 2x^2 - 3x + 1

        double minX = -2.0;
        double maxX = 2.0;

        double[] x = linspace(minX, maxX, NUM_POINTS);
        double[] y = Arrays.stream(x).map(poly::value).toArray();

        EmpiricalFunction func = ImmutableEmpiricalFunction.builder()
                .setX(x)
                .setY(y)
                .build();

        double[] testX = linspace(minX+0.1, maxX-0.1, 100);
        UnivariateDifferentiableFunction diffPoly = poly; // Polynomial is differentiable

        for (double xi : testX) {
            double interpolated = func.value(xi);
            double actual = poly.value(xi);
            assertEquals(actual, interpolated, TOLERANCE_VALUE,
                    String.format("Polynomial at %f should be accurate", xi));

            // Derivative via analytic vs interpolated
            DerivativeStructure ds = diffPoly.value(new DerivativeStructure(1, 1, 0, xi));
            double actualDerivative = ds.getPartialDerivative(1);
            double interpolatedDerivative = func.differential(xi);
            assertEquals(actualDerivative, interpolatedDerivative, TOLERANCE_DERIVATIVE,
                    String.format("Derivative at %f should be accurate", xi));
        }
    }

    /**
     * Test interpolation of a sigmoid function (logistic) using LinkFunction.LOGIT on Y.
     * 
     * f(x) = 1 / (1 + exp(-k*(x - x0)))  --> maps R → (0,1)
     * Applying LOGIT to Y transforms this to: logit(f(x)) = k*(x - x0) → linear!
     * 
     * Therefore, in link space, the relationship is linear → cubic spline should represent it exactly.
     * We test both value and derivative accuracy.
     * 
     * Evaluation avoids extreme tails (x << 0 or x >> 0) where f(x) ≈ 0 or 1, causing logit instability.
     */
    @Test
    public void testSigmoidWithLogitLink() {
        double k = 2.0;           // Steepness
        double x0 = 1.0;          // Midpoint
        DoubleUnaryOperator sigmoid = x -> 1.0 / (1.0 + FastMath.exp(-k * (x - x0)));

        double xMin = -2.0;
        double xMax = 4.0;
        int numTrainingPoints = 15;

        double[] x = linspace(xMin, xMax, numTrainingPoints);
        double[] y = Arrays.stream(x).map(sigmoid).toArray();

        EmpiricalFunction func = ImmutableEmpiricalFunction.builder()
                .setX(x)
                .setY(y)
                .setLink(LinkFunction.LOGIT)
                .build();

        // Test only in central region: where f(x) ∈ (0.05, 0.95) → avoids LOGIT numerical issues
        double testMin = x0 - 2.0;  // Around center
        double testMax = x0 + 2.0;
        double[] testX = linspace(testMin, testMax, 100);

        for (double xi : testX) {
            double interpolated = func.value(xi);
            double actual = sigmoid.applyAsDouble(xi);

            assertEquals(actual, interpolated, TOLERANCE_VALUE,
                    String.format("Sigmoid value at x=%f should be accurate under LOGIT(Y) link", xi));
        }

        // Test derivative: f'(x) = k * f(x) * (1 - f(x))
        for (double xi : testX) {
            double f = sigmoid.applyAsDouble(xi);
            double actualDerivative = k * f * (1.0 - f);
            double interpolatedDerivative = func.differential(xi);

            assertEquals(actualDerivative, interpolatedDerivative, TOLERANCE_DERIVATIVE,
                    String.format("Derivative of sigmoid at x=%f should be accurate", xi));
        }
    }
    
    /**
     * Helper: Generate linearly spaced array from start to end (inclusive)
     */
    private double[] linspace(double start, double end, int n) {
        return DoubleStream.iterate(start, i -> i + (end - start) / (n - 1))
            .limit(n)
            .toArray();
    }
}