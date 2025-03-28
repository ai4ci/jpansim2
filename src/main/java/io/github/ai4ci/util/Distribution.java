package io.github.ai4ci.util;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.abm.ModelOperation.BiFunction;
import io.github.ai4ci.abm.ModelOperation.TriFunction;
import io.reactivex.rxjava3.annotations.Nullable;


@Value.Immutable
@JsonSerialize(as = ImmutableDistribution.class)
@JsonDeserialize(as = ImmutableDistribution.class)
public interface Distribution extends Serializable {

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
		UNIFORM0 ((s,mean) -> s.uniform()*mean*2);
		
		Type(TriFunction<Sampler,Double,Double,Double> fn) {this.fn2 = fn; this.params=2;}
		Type(BiFunction<Sampler,Double,Double> fn) {this.fn1 = fn; this.params=1;}
		TriFunction<Sampler,Double,Double,Double> fn2;
		BiFunction<Sampler,Double,Double> fn1;
		int params;
		int params() {return params;}
		TriFunction<Sampler,Double,Double,Double> fn2() {return fn2;}
		BiFunction<Sampler,Double,Double> fn1() {return fn1;}
	}
	
	Type getType();
	Double getCentral();
	@Nullable @Value.Default default Double getDispersion() {return null;}
	
	private static Distribution of(Type type, Double mean, Double sd) {
		return ImmutableDistribution.builder().setType(type)
				.setDispersion(sd).setCentral(mean).build();
	}
	private static Distribution of(Type type, Double mean) {
		return of(type,mean,null);
	}
	
	public static Distribution binom(int n,Double p) {
		return of(Type.BINOM, n*p, n*p*(1-p));
	}
	public static Distribution pois(Double rate) {
		return of(Type.POIS, rate);
	}
	public static Distribution negBinom(Double mean, Double sd) {
		return of(Type.NEG_BINOM, mean, sd);
	}
	public static Distribution gamma(Double mean, Double sd) {
		return of(Type.GAMMA, mean, sd);
	}
	public static Distribution norm(Double mean, Double sd) {
		return of(Type.NORM, mean, sd);
	}
	public static Distribution logNorm(Double mean, Double sd) {
		return of(Type.LOG_NORM, mean, sd);
	}
	public static Distribution point(Double mean) {
		return of(Type.POINT, mean);
	}
	public static Distribution uniform0(Double upper) {
		return of(Type.UNIFORM0, upper/2);
	}
	public static Distribution unimodalBeta(Double mean, Double sd) {
		return of(Type.UNIMODAL_BETA, mean, sd);
	}
	public static Distribution beta(Double mean, Double sd) {
		return of(Type.BETA, mean, sd);
	}
	
	public default Double sample() {
		Sampler rng = Sampler.getSampler();
		return sample(rng);
	}
	
	public default Double sample(Sampler rng) {
		
		if (getType().params() == 1) {
			return getType().fn1().apply(rng, getCentral());
		}
		if (getType().params() == 2) {
			return getType().fn2().apply(rng, getCentral(),getDispersion());
		}
		throw new RuntimeException("Poorly defined distribution");
	}
}
