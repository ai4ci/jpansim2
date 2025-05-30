package io.github.ai4ci.config.riskmodel;

import io.github.ai4ci.abm.riskmodel.Kernel;

public enum Kernels {
	
	DEFAULT_SYMPTOM_ONSET_KERNEL (Kernel.from(4, 
			0.1,0.2,0.4,0.8,
			1,1,1,0.8,0.6,0.4,0.2).normaliseTo(5)
	),
	
	DEFAULT_TEST_SAMPLE_KERNEL (Kernel.from(6, 
			0.1,0.2,0.4,0.6,0.8,0.9,
			1,1,1,1,1,1,0.8,0.6,0.4,0.2).normaliseTo(10)
	),
	
	DEFAULT_CONTACT_KERNEL (Kernel.from(0,
			1,1,1,1,0.8,0.6,0.4,0.2).normaliseTo(7.5)
	)
	;
	
	
	Kernel kernel;
	Kernels(Kernel kernel) {this.kernel = kernel;}
}