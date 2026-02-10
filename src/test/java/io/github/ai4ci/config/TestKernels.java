package io.github.ai4ci.config;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.abm.riskmodel.ConvolutionFilter;
import io.github.ai4ci.example.Kernels;

public class TestKernels {

	@Test
	public void testKernel() {
		
		System.out.println(Arrays.toString(
			ConvolutionFilter.from(
				Kernels.DEFAULT_CONTACT_KERNEL.kernel()
			).getDensity()
		));
		
		ConvolutionFilter tmp2 = ConvolutionFilter.from(
				Kernels.DEFAULT_SYMPTOM_ONSET_KERNEL.kernel()
			);
		System.out.println(
			Arrays.toString(
				tmp2.getDensity()
			));
		System.out.println(tmp2.getOffset());
		
	}
	
}
