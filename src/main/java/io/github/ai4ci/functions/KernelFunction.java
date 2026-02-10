package io.github.ai4ci.functions;

import java.util.stream.IntStream;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.util.ProbabilityScaler;

/**
 * Interface for kernel functions that operate on discrete integer ranges.
 * 
 * <p>Kernel functions are typically used for convolution operations and probability
 * distributions with discrete support. Supports multiple implementations including
 * empirical kernels, discrete functions, and Gaussian kernels.
 */
@JsonTypeInfo(use = Id.NAME, requireTypeIdForSubtypes = OptBoolean.TRUE)
@JsonSubTypes( {
	@Type(value=ImmutableEmpiricalKernel.class, name="empirical"), 
	@Type(value=ImmutableDiscreteFunction.class, name="function"),
	@Type(value=ImmutableGaussianKernel.class, name="gaussian"),
})
public interface KernelFunction {
	
	/** 
	 * The kernel function is defined over a discrete integer range from minimum to maximum.
	 * @return the maximum value in the kernel's support range */
	int getMaximum();
	
	/**
	 *  The kernel function is defined over a discrete integer range from minimum to maximum.
	 *  @return the minimum value in the kernel's support range */
	int getMinimum();
	
	/** 
	 *  The kernel function values are scaled to sum to this target value.
	 * @return the target sum for the scaled kernel values */
	double getSum();
	
	/**
	 * Returns the raw kernel value at position t before scaling.
	 * 
	 * @param t the position index
	 * @return the raw kernel value at position t
	 */
	double rawValue(int t);
	
	/**
	 * Returns the scaled kernel value at position t.
	 * 
	 * @param t the position index
	 * @return the scaled kernel value at position t
	 */
	default double value(int t) {
		return values()[t-getMinimum()];
	};
	
	/**
	 * Returns the normalized kernel values scaled to the target sum.
	 * 
	 * @return array of normalized kernel values
	 */
	@JsonIgnore
	@Value.Derived default double[] values() {
		int min = (int) getMinimum();
		int max = (int) getMaximum();
		double[] raw =  IntStream.rangeClosed(min, max)
				.mapToDouble(this::rawValue)
				.toArray();
		return ProbabilityScaler.scaleToTargetSum(raw, getSum());
	};
	
}