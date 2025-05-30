package io.github.ai4ci.config.riskmodel;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.riskmodel.Kernel;

@Value.Immutable
@JsonSerialize(as = ImmutableRiskKernelProvided.class)
@JsonDeserialize(as = ImmutableRiskKernelProvided.class)
public interface RiskKernelProvided extends RiskKernelConfiguration {
	int getOffset();
	double[] getDensity();
	default Kernel kernel() {return Kernel.from(getOffset(), getDensity());}
}