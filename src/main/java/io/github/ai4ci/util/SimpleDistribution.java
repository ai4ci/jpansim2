package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.ai4ci.abm.Abstraction;
import io.github.ai4ci.abm.ModelOperation.BiFunction;
import io.github.ai4ci.abm.ModelOperation.TriFunction;
import io.reactivex.rxjava3.annotations.Nullable;


@Value.Immutable
public interface SimpleDistribution extends Abstraction.Distribution, Serializable {

	public static enum Type {
		BINOM ((s,mean,sd) -> (double) s.binom(mean, sd)), 
		POIS ((s,mean) -> (double) s.poisson(mean)), 
		NEG_BINOM ((s,mean,sd) -> (double) s.negBinom(mean,sd)),
		GAMMA ((s,mean,sd) -> s.gamma(mean,sd)), 
		NORM  ((s,mean,sd) -> s.normal(mean,sd)), 
		LOG_NORM ((s,mean,sd) -> s.logNormal(mean,sd)),
		LOGIT_NORM ((s,median,scale) -> s.logitNormal(median,scale)),
		UNIMODAL_BETA ((s,mean,sd) -> s.beta(mean,sd,true)),
		BETA ((s,mean,sd) -> s.beta(mean,sd,false)),
		POINT ((s,mean) -> mean), 
		UNIFORM0 ((s,mean) -> s.uniform()*mean*2),
		RESAMPLE ();
		
		Type(TriFunction<Sampler,Double,Double,Double> fn) {this.fn2 = fn; this.params=2;}
		Type(BiFunction<Sampler,Double,Double> fn) {this.fn1 = fn; this.params=1;}
		Type() {this.params=0;}
		TriFunction<Sampler,Double,Double,Double> fn2;
		BiFunction<Sampler,Double,Double> fn1;
		int params;
		int params() {return params;}
		TriFunction<Sampler,Double,Double,Double> fn2() {return fn2;}
		BiFunction<Sampler,Double,Double> fn1() {return fn1;}
	}
	
	Type getType();
	double getCentral();
	@Nullable @Value.Default default Double getDispersion() {return null;}
	
	private static SimpleDistribution of(Type type, Double mean, Double sd) {
		return ImmutableSimpleDistribution.builder().setType(type)
				.setDispersion(sd).setCentral(mean).build();
	}
	private static SimpleDistribution of(Type type, Double mean) {
		return of(type,mean,null);
	}
	
	public static SimpleDistribution binom(int n,Double p) {
		return of(Type.BINOM, n*p, n*p*(1-p));
	}
	public static SimpleDistribution pois(Double rate) {
		return of(Type.POIS, rate);
	}
	public static SimpleDistribution negBinom(Double mean, Double sd) {
		return of(Type.NEG_BINOM, mean, sd);
	}
	public static SimpleDistribution gamma(Double mean, Double sd) {
		return of(Type.GAMMA, mean, sd);
	}
	public static SimpleDistribution norm(Double mean, Double sd) {
		return of(Type.NORM, mean, sd);
	}
	public static SimpleDistribution logNorm(Double mean, Double sd) {
		return of(Type.LOG_NORM, mean, sd);
	}
	public static SimpleDistribution point(Double mean) {
		return of(Type.POINT, mean);
	}
	public static SimpleDistribution uniform0(Double upper) {
		return of(Type.UNIFORM0, upper/2);
	}
	public static SimpleDistribution unimodalBeta(Double mean, Double sd) {
		return of(Type.UNIMODAL_BETA, mean, sd);
	}
	public static SimpleDistribution beta(Double mean, Double sd) {
		return of(Type.BETA, mean, sd);
	}
	
	public default double sample() {
		Sampler rng = Sampler.getSampler();
		return sample(rng);
	}
	
	public default double sample(Sampler rng) {
		
		if (getType().params() == 1) {
			return getType().fn1().apply(rng, getCentral());
		}
		if (getType().params() == 2) {
			return getType().fn2().apply(rng, getCentral(),getDispersion());
		}
		throw new RuntimeException("Poorly defined distribution");
	}
	
	@JsonIgnore
	@Value.Derived public default double[] getSamples() {
		return IntStream.range(0,PRECISION)
				.mapToDouble(i -> sample())
				.toArray();
	}
	
	default double pLessThan(double x) {
		return Arrays.stream(getSamples())
			.filter(d -> d < x )
			.count() / PRECISION;
	}
	
	@Value.Derived default double getMedian() {
		double[] tmp = getSamples();
		Arrays.sort(tmp);
		return tmp[PRECISION / 2];
	}
}
