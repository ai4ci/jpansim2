package io.github.ai4ci.util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class SparseDecomposition {

    public static class SparseMatrix {
    	public final int dim;
    	public final int[] rowIndices;      // row indices
        public final int[] colIndices;  // column indices
        public final double[] values;   // matrix values

        public SparseMatrix(int[] rowIndices, int[] colIndices, double[] values) {
        	this(
        			Math.max(
	        			Arrays.stream(rowIndices).max().orElse(0),
	        			Arrays.stream(colIndices).max().orElse(0))+1,
        			rowIndices, 
        			colIndices,
        			values
        	);
        }
        
        public SparseMatrix(int dim, int[] rowIndices, int[] colIndices, double[] values) {
        	if (rowIndices.length != colIndices.length || rowIndices.length != values.length) 
        		throw new RuntimeException("Mistamtched sizes");
            this.rowIndices = rowIndices;
            this.colIndices = colIndices;
            this.values = values;
            this.dim = dim; 
        }
        
        public static SparseMatrixBuilder builder() {
        	return new SparseMatrixBuilder();
        }

        // Multiply matrix by vector: y = A * x
        public AtomicDoubleArray multiply(AtomicDoubleArray x) {
        	if (x.length() != dim) throw new RuntimeException("Mismatched shapes");
        	AtomicDoubleArray out = new AtomicDoubleArray(x.length());
            IntStream.range(0, values.length).parallel().forEach(k -> {
            	int j = colIndices[k];
            	// multiplies across columns
                double Axi = x.get(j) * values[k];
                int i = rowIndices[k];
                // sums accross rows
                out.getAndAdd(i, Axi);
            });
            return out;
        }
    }

    // Compute Euclidean norm of a vector
    private static double norm2(AtomicDoubleArray v) {
        return Math.sqrt(IntStream.range(0, v.length())
                .parallel()
                .mapToDouble(i -> v.get(i) * v.get(i))
                .sum());
    }

    // Normalize vector in-place
    private static void normalize(AtomicDoubleArray v) {
        double n = norm2(v);
        if (n > 0) {
            final double invNorm = 1.0 / n;
            IntStream.range(0, v.length()).parallel().forEach(i -> {
            	v.getAndApply(i, invNorm, (old,x) -> old*x);
            });
        }
    }

    /**
     * Perform power iteration to estimate the largest eigenvalue and eigenvector.
     *
     * @param A        The sparse matrix in triple format
     * @param maxIter  Maximum number of iterations
     * @param tol      Convergence tolerance
     * @return         An array containing [lambda_max, ... eigenvector ...]
     */
    public static double[] powerIteration(SparseMatrix A, int maxIter, double tol) {
        int n = A.dim;

        // Initialize random guess vector
        AtomicDoubleArray v = new AtomicDoubleArray(n);
        for (int i = 0; i < n; i++) {
            v.set(i, Math.random() - 0.5);  // Small random initial vector
        }
        normalize(v);

        double lambdaOld = 0.0;
        
        boolean converged = false;
        for (int iter = 0; iter < maxIter; iter++) {
        	AtomicDoubleArray Av = A.multiply(v);  // Av = A * v
        	AtomicDoubleArray v2 = v;

            // New eigenvalue estimate via Rayleigh quotient
            double lambda = IntStream.range(0, n)
                    .parallel()
                    .mapToDouble(i -> v2.get(i) * Av.get(i))
                    .sum();

            // Update eigenvector approximation
            v = Av;
            normalize(v);

            // Check convergence
            if (Math.abs(lambda - lambdaOld) < tol) {
                lambdaOld = lambda;
                converged = true;
                break;
            }

            lambdaOld = lambda;
        }
        if (!converged) throw new RuntimeException("Eigenvalue decomposition did not converge.");
        
        // normalise signs otherwise we get variable output
        if (v.get(0)<0) for (int i =0; i<n; i++) v.getAndApply(i, -1, (x,y) -> x*y);

        // Return [lambda, v0, v1, ..., vn-1]
        double[] result = new double[n + 1];
        result[0] = lambdaOld;
        for (int i =0; i<n; i++) result[i+1] = v.get(i);
        return result;
    }


    public static class SparseMatrixBuilder {
        public int dim = -1;
        public List<Integer> rowIndices = new ArrayList<>();      // rowPtr[i] to rowPtr[i+1] gives col indices and values for row i
        public List<Integer> colIndices = new ArrayList<>();  // column indices
        public List<Double> values = new ArrayList<>();
        
        public void insertSymmetric(Integer row, Integer column, Double value) {
        	insert(row,column,value);
        	insert(column,row,value);
        }
        
        public void insert(Integer row, Integer column, Double value) {
        	if (value < 1e-8) return;
        	if (dim < row) dim = row;
        	if (dim < column) dim = column;
        	rowIndices.add(row);
        	colIndices.add(column);
        	values.add(value);
        }
        
        public SparseMatrix build() {
        	return new SparseMatrix(
        		dim+1,
    			rowIndices.stream().mapToInt(i->i).toArray(),
    			colIndices.stream().mapToInt(i->i).toArray(),
    			values.stream().mapToDouble(i->i).toArray()
        	); 
        }
    }
    
    
}

//public class SparseDecomposition {
//
//    // Sparse matrix in Compressed Sparse Row (CSR) format
//    public static class SparseMatrix {
//        public final int numRows;
//        public final int[] rowPtr;      // rowPtr[i] to rowPtr[i+1] gives col indices and values for row i
//        public final int[] colIndices;  // column indices
//        public final double[] values;   // matrix values
//
//        public SparseMatrix(int[] rowPtr, int[] colIndices, double[] values) {
//            this.numRows = rowPtr.length-1;
//            this.rowPtr = rowPtr;
//            this.colIndices = colIndices;
//            this.values = values;
//        }
//        
//        public static SparseMatrixBuilder builder() {
//        	return new SparseMatrixBuilder();
//        }
//
//        // Multiply matrix by vector: y = A * x
//        public void multiply(double[] x, double[] y) {
//            IntStream.range(0, numRows).parallel().forEach(i -> {
//                double sum = 0.0;
//                int start = rowPtr[i];
//                int end = rowPtr[i + 1];
//                for (int j = start; j < end; j++) {
//                    sum += values[j] * x[colIndices[j]];
//                }
//                y[i] = sum;
//            });
//        }
//    }
//
//    // Compute Euclidean norm of a vector
//    private static double norm2(double[] v) {
//        return Math.sqrt(IntStream.range(0, v.length)
//                .parallel()
//                .mapToDouble(i -> v[i] * v[i])
//                .sum());
//    }
//
//    // Normalize vector in-place
//    private static void normalize(double[] v) {
//        double n = norm2(v);
//        if (n > 0) {
//            final double invNorm = 1.0 / n;
//            IntStream.range(0, v.length).parallel().forEach(i -> {
//                v[i] *= invNorm;
//            });
//        }
//    }
//
//    /**
//     * Perform power iteration to estimate the largest eigenvalue and eigenvector.
//     *
//     * @param A        The sparse matrix in CSR format
//     * @param maxIter  Maximum number of iterations
//     * @param tol      Convergence tolerance
//     * @return         An array containing [lambda_max, ... eigenvector ...]
//     */
//    public static double[] powerIteration(SparseMatrix A, int maxIter, double tol) {
//        int n = A.numRows;
//
//        // Initialize random guess vector
//        double[] v = new double[n];
//        for (int i = 0; i < n; i++) {
//            v[i] = Math.random() - 0.5;  // Small random initial vector
//        }
//        normalize(v);
//
//        double lambdaOld = 0.0;
//        double[] Av = new double[n];
//        
//        boolean converged = false;
//        for (int iter = 0; iter < maxIter; iter++) {
//            A.multiply(v, Av);  // Av = A * v
//
//            // New eigenvalue estimate via Rayleigh quotient
//            double lambda = IntStream.range(0, n)
//                    .parallel()
//                    .mapToDouble(i -> v[i] * Av[i])
//                    .sum();
//
//            // Update eigenvector approximation
//            System.arraycopy(Av, 0, v, 0, n);
//            normalize(v);
//
//            // Check convergence
//            if (Math.abs(lambda - lambdaOld) < tol) {
//                lambdaOld = lambda;
//                converged = true;
//                break;
//            }
//
//            lambdaOld = lambda;
//        }
//        if (!converged) throw new RuntimeException("Eigenvalue decomposition did not converge.");
//        
//        // normalise signs otherwise we get variable output
//        if (v[0]<0) for (int i =0; i<n; i++) v[i] *= -1;
//
//        // Return [lambda, v0, v1, ..., vn-1]
//        double[] result = new double[n + 1];
//        result[0] = lambdaOld;
//        System.arraycopy(v, 0, result, 1, n);
//        return result;
//    }
//
//
//    public static class SparseMatrixBuilder {
//        public int numRows;
//        public List<Integer> rowPtr = new ArrayList<>();      // rowPtr[i] to rowPtr[i+1] gives col indices and values for row i
//        public List<Integer> colIndices = new ArrayList<>();  // column indices
//        public List<Double> values = new ArrayList<>();
//        
//        {
//        	rowPtr.add(0);
//        }
//        
//        public void insertSymmetric(Integer row, Integer column, Double value) {
//        	insert(row,column,value);
//        	insert(column,row,value);
//        }
//        
//        public void insert(Integer row, Integer column, Double value) {
//        	if (value == 0) return;
//        	// pad row pointers with zeros 
//        	int initRows = rowPtr.size()-1; 
//        	if (initRows <= row) for (int i = 0; i<=row-initRows; i++) rowPtr.add(rowPtr.get(initRows));
//        	int colStart = rowPtr.get(row);
//        	int colEnd = rowPtr.get(row+1);
//        	boolean found = false;
//        	for (int j = colStart; j<colEnd; j++) {
//        		if (column == colIndices.get(j)) {
//        			// Redefined. This could be an error.
//        			colIndices.set(j, column);
//        			values.set(j, value);
//        			found = true;
//        			break;
//        		} else if (column < colIndices.get(j)) {
//        			colIndices.add(j, column);
//        			values.add(j, value);
//        			found = true;
//        			break;
//        		}
//        	}
//        	if (!found) {
//        		colIndices.add(colEnd, column);
//        		values.add(colEnd, value);
//        	}
//        	for (int i = row+1; i < rowPtr.size(); i++) rowPtr.set(i,rowPtr.get(i)+1);
//        }
//        
//        public SparseMatrix build() {
//        	return new SparseMatrix(
//    			rowPtr.stream().mapToInt(i->i).toArray(),
//    			colIndices.stream().mapToInt(i->i).toArray(),
//    			values.stream().mapToDouble(i->i).toArray()
//        	); 
//        }
//    }
//    
//    public static <X> SparseMatrix csrFromEdges(
//    		Stream<X> input,
//    		Function<X,Integer> sourceId, 
//    		Function<X,Integer> targetId, 
//    		Function<X,Double> mapper,
//    		boolean symmetric
//    	) {
//		
//    	SparseMatrixBuilder build = SparseMatrix.builder();
//    	
//    	input.forEach(x -> {
//    		
//    		if (symmetric) 
//    			build.insertSymmetric(targetId.apply(x), sourceId.apply(x), mapper.apply(x));
//    		else 
//    			build.insert(sourceId.apply(x), targetId.apply(x), mapper.apply(x));
//    	});
//    	
//		return build.build();
//	}
// }