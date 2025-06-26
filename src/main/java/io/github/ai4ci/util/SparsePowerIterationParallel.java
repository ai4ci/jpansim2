package io.github.ai4ci.util;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

public class SparsePowerIterationParallel {

    // Sparse matrix in Compressed Sparse Row (CSR) format
    public static class SparseMatrixCSR {
        public final int numRows;
        public final int[] rowPtr;      // rowPtr[i] to rowPtr[i+1] gives col indices and values for row i
        public final int[] colIndices;  // column indices
        public final double[] values;   // matrix values

        public SparseMatrixCSR(int numRows, int[] rowPtr, int[] colIndices, double[] values) {
            this.numRows = numRows;
            this.rowPtr = rowPtr;
            this.colIndices = colIndices;
            this.values = values;
        }

        // Multiply matrix by vector: y = A * x
        public void multiply(double[] x, double[] y) {
            IntStream.range(0, numRows).parallel().forEach(i -> {
                double sum = 0.0;
                int start = rowPtr[i];
                int end = rowPtr[i + 1];
                for (int j = start; j < end; j++) {
                    sum += values[j] * x[colIndices[j]];
                }
                y[i] = sum;
            });
        }
    }

    // Compute Euclidean norm of a vector
    private static double norm2(double[] v) {
        return Math.sqrt(IntStream.range(0, v.length)
                .parallel()
                .mapToDouble(i -> v[i] * v[i])
                .sum());
    }

    // Normalize vector in-place
    private static void normalize(double[] v) {
        double n = norm2(v);
        if (n > 0) {
            final double invNorm = 1.0 / n;
            IntStream.range(0, v.length).parallel().forEach(i -> {
                v[i] *= invNorm;
            });
        }
    }

    /**
     * Perform power iteration to estimate the largest eigenvalue and eigenvector.
     *
     * @param A        The sparse matrix in CSR format
     * @param maxIter  Maximum number of iterations
     * @param tol      Convergence tolerance
     * @return         An array containing [lambda_max, ... eigenvector ...]
     */
    public static double[] powerIteration(SparseMatrixCSR A, int maxIter, double tol) {
        int n = A.numRows;

        // Initialize random guess vector
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = Math.random() - 0.5;  // Small random initial vector
        }
        normalize(v);

        double lambdaOld = 0.0;
        double[] Av = new double[n];

        for (int iter = 0; iter < maxIter; iter++) {
            A.multiply(v, Av);  // Av = A * v

            // New eigenvalue estimate via Rayleigh quotient
            double lambda = IntStream.range(0, n)
                    .parallel()
                    .mapToDouble(i -> v[i] * Av[i])
                    .sum();

            // Update eigenvector approximation
            System.arraycopy(Av, 0, v, 0, n);
            normalize(v);

            // Check convergence
            if (Math.abs(lambda - lambdaOld) < tol) {
                System.out.println("Converged after " + (iter + 1) + " iterations.");
                break;
            }

            lambdaOld = lambda;
        }

        // Return [lambda, v0, v1, ..., vn-1]
        double[] result = new double[n + 1];
        result[0] = lambdaOld;
        System.arraycopy(v, 0, result, 1, n);
        return result;
    }


    
    
    public static <X> SparseMatrixCSR contactsCSR(X[][] contacts, BiFunction<X,Integer,Integer> other, Function<X,Double> mapper) {
		// Convert to CSR format
		int n = contacts.length;
		int totalEntries = 0;
		int[] rowCounts = new int[n];

		// First pass: count non-zero entries per row
		for (int i = 0; i < n; i++) {
		    rowCounts[i] = contacts[i].length;
		    totalEntries += rowCounts[i];
		}

		// Second pass: fill arrays
		int[] rowPtr = new int[n + 1];
		int[] colIndices = new int[totalEntries];
		double[] values = new double[totalEntries];

		int idx = 0;
		rowPtr[0] = 0;
		for (int i = 0; i < n; i++) {
		    X[] neighbors = contacts[i];
		    for (X entry : neighbors) {
		        colIndices[idx] = other.apply(entry, i);
		        values[idx] = mapper.apply(entry);
		        idx++;
		    }
		    rowPtr[i + 1] = idx;
		}

		SparseMatrixCSR A = new SparseMatrixCSR(n, rowPtr, colIndices, values);
		return A;
	}
}