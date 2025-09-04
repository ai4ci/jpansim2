package io.github.ai4ci.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.TestUtils;
import io.github.ai4ci.abm.mechanics.ModelOperation.BiFunction;
import io.github.ai4ci.config.setup.BarabasiAlbertConfiguration;
import io.github.ai4ci.config.setup.PartialSetupConfiguration;
import io.github.ai4ci.util.SparseDecomposition.SparseMatrix;
import io.github.ai4ci.util.SparseDecomposition.SparseMatrixBuilder;

public class TestEigenValue {
    /**
     * Example usage with small sparse matrix:
     *
     * 3x3 symmetric matrix:
     * [ 0.0  2.0  3.0 ]
     * [ 2.0  0.0  4.0 ]
     * [ 3.0  4.0  0.0 ]
     */
	static int[] rowPtr = {0, 2, 4, 6};
	static int[] rowIndices = {0, 0, 1, 1, 2, 2};
	static int[] colIndices = {1, 2, 0, 2, 0, 1};
	static double[] values = {2.0, 3.0, 2.0, 4.0, 3.0, 4.0};

	static double eigenValue = 6.07467;
	static double[] eigenVector = {0.797043, 0.920887, 1};
	
	@Test
	void testEigen() {
        // CSR representation of the above symmetric matrix
        int n = rowPtr.length-1;
        SparseMatrix A = new SparseMatrix(3, rowIndices, colIndices, values);

        int maxIter = 100;
        double tol = 1e-5;

        double[] result = SparseDecomposition.powerIteration(A, maxIter, tol);
        double lambdaMax = result[0];
        double[] v = new double[n];
        System.arraycopy(result, 1, v, 0, n);
        
        assertThat(lambdaMax).isCloseTo(eigenValue, byLessThan(tol));

        System.out.println("Largest eigenvalue (lambda_max): " + lambdaMax);
        System.out.print("Eigenvector: ");
        for (int i = 0; i<n; i++) {
        	assertThat(v[i]/v[n-1]).isCloseTo(eigenVector[i], byLessThan(Math.sqrt(tol)));
            System.out.printf("%.4f (%.4f), ", v[i], v[i]/v[n-1]);
        }
        
        System.out.println();
    }
    
    @Test
	void testCSRBuild() {
		
		SparseMatrixBuilder tmp = SparseMatrix.builder();
		
		//tmp.insert(2, 2, 0.5);
		
//		tmp.insert(0,0,1.0);
//		tmp.insert(1,1,1.0);
//		tmp.insert(1,2,2.0);
//		tmp.insert(2,2,1.0);
//		
		tmp.insertSymmetric(1,2,4.0);
		tmp.insertSymmetric(0,1,2.0);
		tmp.insertSymmetric(0,2,3.0);
		
		SparseMatrix m = tmp.build();
		
		System.out.println(Arrays.toString(m.rowIndices));
		System.out.println(Arrays.toString(m.colIndices));
		System.out.println(Arrays.toString(m.values));
		
		//assertArrayEquals(m.rowPtr, rowPtr);
		// assertArrayEquals(m.rowIndices, rowIndices);
		// assertArrayEquals(m.colIndices, colIndices);
		// assertArrayEquals(m.values, values);
		
	}
    
    /**
     * Constructs a dense n x n matrix where each element is computed as f(i, j)
     *
     * @param n        Size of the matrix (n x n)
     * @param function Function that computes value at (i, j)
     * @return         2D double array representing the dense matrix
     */
    public static double[][] buildDenseMatrix(int n, BiFunction<Integer, Integer, Double> function) {
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = function.apply(i, j);
            }
        }
        return matrix;
    }

    /**
     * Builds a sparse matrix using MatrixBuilder from a dense matrix
     * Only non-zero entries are inserted
     *
     * @param dense    Dense matrix as 2D array
     * @param builder  MatrixBuilder implementation
     * @return         SparseMatrix built via builder
     */
    public static SparseMatrix denseToSparse(double[][] dense, SparseMatrixBuilder builder) {
        int n = dense.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < dense[i].length; j++) {
                if (Math.abs(dense[i][j]) > 1e-12) {
                    builder.insert(i, j, dense[i][j]);
                }
            }
        }
        return builder.build();
    }
    
    @Test
    public void testPowerIterationWithGaussianKernelMatrix() {
        
    	int n = 10;
        // Example 1: Gaussian kernel-like matrix: A[i][j] = exp(-(i-j)^2 / sigma^2)
        double sigma = 1.5;
        double[][] dense = buildDenseMatrix(n, (i, j) -> {
            double diff = i - j;
            return Math.exp(-diff * diff / (sigma * sigma));
        });
        
        doMatrixTest(dense);
    }
    
    @Test
    public void testPowerIterationWithLinearRowColMatrix() {
        int n = 4;
        // Example 2: A[i][j] = i + j (symmetric)
        double[][] dense = buildDenseMatrix(n, (i, j) -> (double)(i + j));
        doMatrixTest(dense);
    }
    
    private void doMatrixTest(double[][] denseMatrix) {
        
    	int n = denseMatrix.length;

        // Use MatrixBuilder to create sparse matrix from dense
        SparseMatrix A_sparse = denseToSparse(denseMatrix, SparseMatrix.builder());

        // Create EJML dense matrix for comparison
        SimpleMatrix A_dense = new SimpleMatrix(denseMatrix);

        // Find dominant real eigenvalue (largest in magnitude)
        double lambdaMaxDense = 0.0;
        SimpleMatrix dominantEigenvector = null;
        double maxAbs = -1.0;

        for (int i = 0; i < n; i++) {
            var eigenvalue = A_dense.eig().getEigenvalue(i);
            if (Math.abs(eigenvalue.imaginary) > 1e-10) continue; // Skip complex

            double realPart = eigenvalue.real;
            if (Math.abs(realPart) > maxAbs) {
                maxAbs = Math.abs(realPart);
                lambdaMaxDense = realPart;
                dominantEigenvector = A_dense.eig().getEigenVector(i).copy();
            }
        }

        if (dominantEigenvector!=null) {
        	// No real dominant eigenvalue found;
        
	        dominantEigenvector = normalize(dominantEigenvector);	
	        // Run power iteration on the sparse matrix
	        int maxIter = 1000;
	        double tol = 1e-8;
	        double[] result = SparseDecomposition.powerIteration(A_sparse, maxIter, tol);
	
	        double lambdaMaxSparse = result[0];
	        double[] vSparseData = new double[n];
	        System.arraycopy(result, 1, vSparseData, 0, n);
	        SimpleMatrix vSparse = new SimpleMatrix(n, 1, true, vSparseData);
	        vSparse = normalize(vSparse);
	
	        // Compare eigenvalues
	        assertEquals(lambdaMaxDense, lambdaMaxSparse, 1e-3, "Largest eigenvalue does not match");
	
	        // Compare eigenvectors (account for sign flip)
	        double dot = vSparse.transpose().mult(dominantEigenvector).get(0, 0);
	        assertTrue(Math.abs(dot) > 0.9999,
	                "Eigenvectors are not aligned (|dot product| = " + Math.abs(dot) + ")");
        }
    }

    private SimpleMatrix normalize(SimpleMatrix v) {
        double norm = v.normF(); // Frobenius norm (for vector, same as L2)
        return v.divide(norm);
    }
    
    @Test
    void testSocialNetworkEigen() {
    	Outbreak out2 = TestUtils.builder
				.setSetupTweak(
					PartialSetupConfiguration.builder()
						.setNetwork(BarabasiAlbertConfiguration.DEFAULT
								.withNetworkSize(10000)
								.withNetworkDegree(100)
						)
						.build())
				.build().getOutbreak(); 
    	
    	var build = SparseMatrix.builder();
    	out2.getSocialNetwork().stream().forEach(sr -> {
    		build.insertSymmetric(sr.getPeopleIds().keyInt(), sr.getPeopleIds().valueInt(), sr.getRelationshipStrength());
    	});
    	var m = build.build();
    	
    	int maxIter = 1000;
        double tol = 1e-8;
        double[] result = SparseDecomposition.powerIteration(m, maxIter, tol);
        
        System.out.println("EigenValue: "+result[0]);
        System.out.println("Beta (for R0=1): "+1.0/result[0]);
    }
}
