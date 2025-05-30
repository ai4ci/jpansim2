package io.github.ai4ci.config.riskmodel;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.github.ai4ci.abm.riskmodel.Kernel;

@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes( {
	@Type(value = ImmutableRiskKernelInBuilt.class), 
	@Type(value = ImmutableRiskKernelProvided.class) 
} )
public interface RiskKernelConfiguration extends Serializable {
	
	@JsonIgnore
	@Value.NonAttribute Kernel kernel();

	static RiskKernelConfiguration from(Kernels kernel) {
		return ImmutableRiskKernelInBuilt.builder().setKernel(kernel).build();
	}
	
	static RiskKernelConfiguration from(Kernel kernel) {
		return ImmutableRiskKernelProvided.builder()
				.setDensity(kernel.getDensity())
				.setOffset(kernel.getOffset())
				.build();
	} 
	
}
