package io.github.ai4ci.config.riskmodel;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.riskmodel.Kernel;

@Value.Immutable
@JsonSerialize(as = ImmutableRiskKernelInBuilt.class)
@JsonDeserialize(as = ImmutableRiskKernelInBuilt.class)
public interface RiskKernelInBuilt extends RiskKernelConfiguration {
	Kernels getKernel();
	default Kernel kernel() {return getKernel().kernel;}
}