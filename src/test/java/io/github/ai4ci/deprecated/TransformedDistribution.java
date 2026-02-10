//package io.github.ai4ci.functions;
//
//import java.util.function.DoubleUnaryOperator;
//import java.util.stream.IntStream;
//
//import org.immutables.value.Value;
//
//import io.github.ai4ci.abm.mechanics.Abstraction;
//import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
//import io.github.ai4ci.util.Sampler;
//
//@Deprecated
//@Value.Immutable
//public interface TransformedDistribution extends Abstraction.Distribution {
//
//	default double getCumulative(double x) {
//		return getBaseDistribution().getCumulative(
//			getInverseTransform().applyAsDouble(x)	
//		);
//	}
//	default double getMedian() {
//		return getLink().fn(
//				getBaseDistribution().getMedian());
//	}
//	@Override
//	default double sample(Sampler rng) {
//		return getLink().fn(
//				getBaseDistribution().sample(rng));
//	}
//	
//	public Distribution getBaseDistribution();
//	public default LinkFunction getLink() {return getBaseDistribution().getLink();}
//	public DoubleUnaryOperator getTransform();
//	public DoubleUnaryOperator getInverseTransform();
//	
//	default double getMean() {
//		return IntStream.range(0, PRECISION).mapToDouble(
//				i -> this.sample()
//				).average().getAsDouble();
//		
//	};
//	
//}





