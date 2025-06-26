package io.github.ai4ci.util;

import io.github.ai4ci.util.SparsePowerIterationParallel.SparseMatrixCSR;

public class TestEigenValue {
    /**
     * Example usage with small sparse matrix:
     *
     * 3x3 symmetric matrix:
     * [ 0.0  2.0  3.0 ]
     * [ 2.0  0.0  4.0 ]
     * [ 3.0  4.0  0.0 ]
     */
    public static void main(String[] args) {
        // CSR representation of the above symmetric matrix
        int n = 3;
        int[] rowPtr = {0, 2, 4, 6};
        int[] colIndices = {1, 2, 0, 2, 0, 1};
        double[] values = {2.0, 3.0, 2.0, 4.0, 3.0, 4.0};

        SparseMatrixCSR A = new SparseMatrixCSR(n, rowPtr, colIndices, values);

        int maxIter = 100;
        double tol = 1e-3;

        double[] result = SparsePowerIterationParallel.powerIteration(A, maxIter, tol);
        double lambdaMax = result[0];
        double[] v = new double[n];
        System.arraycopy(result, 1, v, 0, n);

        System.out.println("Largest eigenvalue (lambda_max): " + lambdaMax);
        System.out.print("Eigenvector: ");
        for (double val : v) {
            System.out.printf("%.4f ", val);
        }
        System.out.println();
    }
}
