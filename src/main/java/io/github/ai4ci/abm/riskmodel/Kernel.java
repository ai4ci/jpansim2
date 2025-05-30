package io.github.ai4ci.abm.riskmodel;

import java.io.Serializable;
import java.util.Arrays;

import org.immutables.value.Value;

/**
 * A convolution kernel or filter
 */
@Value.Immutable
public interface Kernel extends Serializable {
	
	// N.B. There is overlap between this and delay distributions.
	
	/** Kernel values, offset as required, compared to a convolution this
	 * is in reverse order. The first value in this array is applied to the
	 * most recent data point, which may be in the future (prospective). 
	 */
	double[] getDensity();
	
	/** index of zero point in values. positive values mean that the
	 * kernel is 2 sided. zero means only left sided. negatives don't make particular
	 * sense but should work consistently
	 */
	int getOffset();
	
	/** get the kernel density value relevant to a point offset from now 
	 * positive values of the offset are in the past, negative values relate
	 * to future data the full range of data is from 
	 * [ -prospectiveSize(),retrospectiveSize() ) */
	default double getDensity(int offset) {
		int index = offset+getOffset();
		if (index<0 || index>=getDensity().length) return 0;
		return getDensity()[index];
	}
	
	/** how far does this kernel extend in the future (inclusive)? */
	default int prospectiveSize() {return getOffset();}
	
	/** how far in the past does this kernel extend (exclusive)? */
	default int retrospectiveSize() {return size()-getOffset();}
	
	default int size() {return getDensity().length;}
	
	default Kernel scale(double factor) {
		return ImmutableKernel.builder().from(this)
				.setDensity(Arrays.stream(getDensity()).map(d -> d * factor).toArray())
				.build();
	}
	
	default Kernel normaliseTo(double sum) {
		double total = Arrays.stream(getDensity()).sum();
		return scale(sum/total);
	}
	
	/**
	 * @param offset: index of zero point in values. positive values mean that the
	 * kernel is 2 sided. zero means only left sided. negatives don't make particular
	 * sense but should work consistently
	 * @param values
	 * @return
	 */
	public static Kernel from(int offset, double... values) {
		return ImmutableKernel.builder().setOffset(offset).setDensity(values).build();
	}
	
	public static Kernel empty() {
		return ImmutableKernel.builder().setOffset(0).setDensity(new double[0]).build();
	}
	
	public static Kernel square(int offset, int length) {
		double[] tmp = new double[length];
		Arrays.fill(tmp, 1.0);
		return ImmutableKernel.builder().setOffset(offset).setDensity(tmp).build();
	}
}