package io.github.ai4ci.util;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;

@Value.Immutable
public interface TransformedDistribution extends Abstraction.Distribution {

	@Override
	default double getCumulative(double x) {
		return getBaseDistribution().getCumulative(
			getInverseLink().applyAsDouble(x)	
		);
	}
	@Override
	default double getMedian() {
		return getLink().applyAsDouble(
				getBaseDistribution().getMedian());
	}
	@Override
	default double sample(Sampler rng) {
		return getLink().applyAsDouble(
				getBaseDistribution().sample(rng));
	}
	
	public Distribution getBaseDistribution();
	public DoubleUnaryOperator getLink();
	public DoubleUnaryOperator getInverseLink();
	
	default double getCentral() {
		return IntStream.range(0, PRECISION).mapToDouble(
				i -> this.sample()
				).average().getAsDouble();
		
	};
	
}
