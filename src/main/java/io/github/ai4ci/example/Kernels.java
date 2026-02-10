package io.github.ai4ci.example;

import io.github.ai4ci.functions.KernelFunction;
import io.github.ai4ci.functions.ImmutableGaussianKernel;

/**
 * A collection of kernels used in example code and tests.
 *
 * These kernels are used in the risk model to produce current risk estimates
 * based on the timing of events such as symptom onset, test sampling and contact events. They are
 * defined as simple Gaussian kernels with parameters chosen to produce reasonable shapes for the
 * respective events.
 * 
 * @author Rob Challen
 */
public enum Kernels {
	
	/** A kernel for symptom onset that peaks around 2 days after infection. */
	DEFAULT_SYMPTOM_ONSET_KERNEL (
			ImmutableGaussianKernel.builder()
			.setMinimum(-3)
			.setMaximum(7)
			.setMu(2)
			.setSigma(3)
			.setSum(5)
			.build()
	),
	/** A kernel for sampling test results that peaks around 2 days after infection. */
	DEFAULT_TEST_SAMPLE_KERNEL (
			ImmutableGaussianKernel.builder()
			.setMinimum(-5)
			.setMaximum(11)
			.setMu(2)
			.setSigma(3)
			.setSum(10)
			.build()
	),
	/** A kernel for sampling contact events that peaks around the time of infection. */
	DEFAULT_CONTACT_KERNEL (
			ImmutableGaussianKernel.builder()
			.setMinimum(0)
			.setMaximum(10)
			.setMu(0)
			.setSigma(3)
			.setSum(7.5)
			.build()
		
	)
	;
	
	/** Get the kernel function associated with this enum instance. 
	 * @return a {@link io.github.ai4ci.functions.KernelFunction} used to compute risk contributions from events such as symptom onset, test sampling and contact events. 
	 */
	public KernelFunction kernel() {return kernel;}
	private KernelFunction kernel;
	private Kernels(KernelFunction kernel) {this.kernel = kernel;}
}