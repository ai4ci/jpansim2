package io.github.ai4ci.abm.riskmodel;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;

import io.github.ai4ci.functions.KernelFunction;

/**
 * A convolution kernel or filter. This is an integer time based filter.
 */
@Value.Immutable
public interface ConvolutionFilter extends Serializable {

	// N.B. There is overlap between this and delay distributions.

	/**
	 * Kernel values, offset as required, compared to a convolution this is in
	 * reverse order. The first value in this array is applied to the most recent
	 * data point, which may be in the future (prospective).
	 * 
	 * @return the kernel density values, ordered from most recent (potentially
	 *         future) to oldest (past) data point
	 */
	double[] getDensity();

	/**
	 * index of zero point in values. positive values mean that the kernel is 2
	 * sided. zero means only left sided. negatives don't make particular sense but
	 * should work consistently
	 * 
	 * @return the index of the zero point in the kernel density values array, where
	 *         positive values indicate a two-sided kernel, zero indicates a
	 *         left-sided kernel, and negative values are allowed but may not make
	 *         particular sense
	 */
	int getOffset();

	/**
	 * get the kernel density value relevant to a point offset from now positive
	 * values of the offset are in the past, negative values relate to future data
	 * the full range of data is from [ -prospectiveSize(),retrospectiveSize() )
	 * 
	 * @param offset the offset from the current point in time for which to retrieve the kernel
	 * @return the kernel density value relevant to a point offset from now, but
	 *         return zero if the offset is in the future
	 */
	default double getDensity(int offset) {
		int index = offset+getOffset();
		if (index<0 || index>=getDensity().length) return 0;
		return getDensity()[index];
	}

	/**
	 * get the kernel density value relevant to a point offset from now, but return
	 * zero if the offset is in the future
	 * 
	 * @return how far does this kernel extend in the future (inclusive)?
	 */
	default int prospectiveSize() {return getOffset();}

	/**
	 * get the kernel density value relevant to a point offset from now, but return
	 * zero if the offset is in the future
	 * 
	 * @return how far in the past does this kernel extend (exclusive)?
	 */
	default int retrospectiveSize() {return size()-getOffset();}

	/**
	 * get the total size of the kernel density array. The kernel density array is
	 * ordered from most recent (potentially future) to oldest (past) data point,
	 * and the offset indicates the index of the zero point in this array. The full
	 * range of data is from [ -prospectiveSize(), retrospectiveSize() ).
	 * 
	 * @return the total size of the kernel density array
	 */
	default int size() {return getDensity().length;}

	/**
	 * Scale the kernel density values by a given factor, returning a new
	 * ConvolutionFilter instance with the same offset and scaled density values.
	 * 
	 * @param factor the factor by which to scale the kernel density values
	 * @return a new ConvolutionFilter instance with the same offset and scaled
	 *         density values
	 */
	default ConvolutionFilter scale(double factor) {
		return ImmutableConvolutionFilter.builder().from(this)
				.setDensity(Arrays.stream(getDensity()).map(d -> d * factor).toArray())
				.build();
	}

	/**
	 * Create a ConvolutionFilter instance from the given offset and kernel density
	 * values. The offset indicates the index of the zero point in the values array,
	 * where positive values mean that the kernel is two-sided, zero means only
	 * left-sided, and negative values are allowed but may not make particular
	 * sense. The kernel density values are ordered from most recent (potentially
	 * future) to oldest (past) data point.
	 * 
	 * @param offset: index of zero point in values. positive values mean that the
	 *                kernel is 2 sided. zero means only left sided. negatives don't
	 *                make particular sense but should work consistently
	 * @param values: kernel density values, ordered from most recent (potentially future) to
	 * 			  oldest (past) data point
	 * @return a new ConvolutionFilter instance with the specified offset and kernel
	 *         density values
	 */
	public static ConvolutionFilter from(int offset, double... values) {
		return ImmutableConvolutionFilter.builder().setOffset(offset).setDensity(values).build();
	}

	/**
	 * Create an empty ConvolutionFilter instance with zero offset and an empty
	 * kernel density array. This filter will effectively have no effect when
	 * applied in a convolution operation, as it contains no density values to
	 * contribute to the output.
	 * 
	 * @return a new ConvolutionFilter instance with zero offset and an empty kernel
	 *         density array
	 */
	public static ConvolutionFilter empty() {
		return ImmutableConvolutionFilter.builder().setOffset(0).setDensity(new double[0]).build();
	}

	/**
	 * Create a ConvolutionFilter instance representing a square kernel with the
	 * specified offset and length. The kernel density values will be set to 1.0 for
	 * all entries, indicating equal weighting for each data point within the
	 * kernel. The offset indicates the index of the zero point in the values array,
	 * where positive values mean that the kernel is two-sided, zero means only
	 * left-sided, and negative values are allowed but may not make particular
	 * sense. The kernel density values are ordered from most recent (potentially
	 * future) to oldest (past) data point.
	 * 
	 * @param offset index of zero point in values. positive values mean that the
	 * @param length the length of the square kernel, which determines the number of
	 *               data points that will be included in the kernel density array.
	 *               The kernel density values will be set to 1.0 for all entries,
	 *               indicating equal weighting for each data point within the
	 *               kernel.
	 * @return a new ConvolutionFilter instance representing a square kernel with
	 *         the specified offset and length, where the kernel density values are
	 *         set to 1.0 for all entries
	 */
	public static ConvolutionFilter square(int offset, int length) {
		double[] tmp = new double[length];
		Arrays.fill(tmp, 1.0);
		return ImmutableConvolutionFilter.builder().setOffset(offset).setDensity(tmp).build();
	}

	/**
	 * Create a ConvolutionFilter instance from the given KernelFunction. The offset
	 * will be set to the negative of the floor of the minimum value of the
	 * KernelFunction, and the kernel density values will be obtained from the
	 * values of the KernelFunction. The kernel density values are ordered from most
	 * recent (potentially future) to oldest (past) data point, and the offset
	 * indicates the index of the zero point in this array, where positive values
	 * mean that the kernel is two-sided, zero means only left-sided, and negative
	 * values are allowed but may not make particular sense.
	 * 
	 * @param kfn the KernelFunction from which to create the ConvolutionFilter
	 *            instance. The offset will be set to the negative of the floor of
	 *            the minimum value of the KernelFunction, and the kernel density
	 *            values will be obtained from the values of the KernelFunction.
	 * @return a new ConvolutionFilter instance created from the given
	 *         KernelFunction, where the offset is set to the negative of the floor
	 *         of the minimum value of the KernelFunction and the kernel density
	 *         values are obtained from the values of the KernelFunction
	 */
	static ConvolutionFilter from(KernelFunction kfn) {
		return ImmutableConvolutionFilter.builder()
				.setOffset(- (int) Math.floor(kfn.getMinimum()))
				.setDensity(kfn.values()).build();
	}
}