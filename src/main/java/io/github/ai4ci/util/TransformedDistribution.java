package io.github.ai4ci.util;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import io.github.ai4ci.abm.mechanics.Abstraction;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;

@Deprecated
@Value.Immutable
public interface TransformedDistribution extends Abstraction.Distribution {

	@Override
	default double getCumulative(double x) {
		return getBaseDistribution().getCumulative(
			getInverseTransform().applyAsDouble(x)	
		);
	}
	@Override
	default double getMedian() {
		return getLink().fn(
				getBaseDistribution().getMedian());
	}
	@Override
	default double sample(Sampler rng) {
		return getLink().fn(
				getBaseDistribution().sample(rng));
	}
	
	public Distribution getBaseDistribution();
	public default LinkFunction getLink() {return getBaseDistribution().getLink();}
	public DoubleUnaryOperator getTransform();
	public DoubleUnaryOperator getInverseTransform();
	
	default double getCentral() {
		return IntStream.range(0, PRECISION).mapToDouble(
				i -> this.sample()
				).average().getAsDouble();
		
	};
	
}
