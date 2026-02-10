package io.github.ai4ci.functions;

import java.io.Serializable;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.github.ai4ci.util.Sampler;


/**
 * Specify distributions and parameters in a way that makes configuring them
 * in a JSON file is relatively straightforward. All distributions defined by
 * a central and dispersion parameter which in most cases will be Mean and SD.
 */

public interface SimpleDistribution extends Distribution, Serializable {

	@Value.Default @JsonIgnore
	default double getMinSupport() {return getLink().getMinSupport();}
	@Value.Default @JsonIgnore
	default double getMaxSupport() {return getLink().getMaxSupport();}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableBinomialDistribution.class)
	@JsonDeserialize(as = ImmutableBinomialDistribution.class)
	public static interface BinomialDistribution extends SimpleDistribution {
		
		double getMean();
		double getSd();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOGIT;}
		default double sample(Sampler s) {return (double) s.binom(getMean(), getSd()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutablePoissonDistribution.class)
	@JsonDeserialize(as = ImmutablePoissonDistribution.class)
	public static interface PoissonDistribution extends SimpleDistribution {
		
		double getMean();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOG;}
		default double sample(Sampler s) {return (double) s.poisson(getMean()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableNegBinomialDistribution.class)
	@JsonDeserialize(as = ImmutableNegBinomialDistribution.class)
	public static interface NegBinomialDistribution extends SimpleDistribution {
		
		double getMean();
		double getSd();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOG;}
		default double sample(Sampler s) {return (double) s.negBinom(getMean(), getSd()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableGammaDistribution.class)
	@JsonDeserialize(as = ImmutableGammaDistribution.class)
	public static interface GammaDistribution extends SimpleDistribution {
		
		double getMean();
		double getSd();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOG;}
		default double sample(Sampler s) {return (double) s.gamma(getMean(), getSd()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableNormalDistribution.class)
	@JsonDeserialize(as = ImmutableNormalDistribution.class)
	public static interface NormalDistribution extends SimpleDistribution {
		
		double getMean();
		double getSd();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.NONE;}
		default double sample(Sampler s) {return (double) s.normal(getMean(), getSd()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableLogNormalDistribution.class)
	@JsonDeserialize(as = ImmutableLogNormalDistribution.class)
	public static interface LogNormalDistribution extends SimpleDistribution {
		
		double getMean();
		double getSd();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOG;}
		default double sample(Sampler s) {return (double) s.logNormal(getMean(), getSd()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableLogitNormalDistribution.class)
	@JsonDeserialize(as = ImmutableLogitNormalDistribution.class)
	public static interface LogitNormalDistribution extends SimpleDistribution {
		
		double getMedian();
		double getScale();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOGIT;}
		default double sample(Sampler s) {return (double) s.logitNormal(getMedian(), getScale()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableBetaDistribution.class)
	@JsonDeserialize(as = ImmutableBetaDistribution.class)
	public static interface BetaDistribution extends SimpleDistribution {
		
		double getMean();
		double getSd();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOGIT;}
		default double sample(Sampler s) {return (double) s.beta(getMean(), getSd(),false); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableUnimodalBetaDistribution.class)
	@JsonDeserialize(as = ImmutableUnimodalBetaDistribution.class)
	public static interface UnimodalBetaDistribution extends SimpleDistribution {
		
		double getMean();
		double getDispersion();
		
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.LOGIT;}
		default double sample(Sampler s) {return (double) s.beta(getMean(), getDispersion(),true); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutableUniformDistribution.class)
	@JsonDeserialize(as = ImmutableUniformDistribution.class)
	public static interface UniformDistribution extends SimpleDistribution {
		
		@Value.Default default double getMin() {return 0;}
		@Value.Default default double getMax() {return 1;}
		@Value.Default @JsonIgnore
		default double getMean() {return (getMax()-getMin())/2.0 + getMin();}
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.NONE;}
		default double getMinSupport() {return getMin();}
		default double getMaxSupport() {return getMax();}
		
		default double sample(Sampler s) {return (double) s.uniform(getMin(),getMax()); }
	}
	
	@Value.Immutable
	@JsonSerialize(as = ImmutablePointDistribution.class)
	@JsonDeserialize(as = ImmutablePointDistribution.class)
	public static interface PointDistribution extends SimpleDistribution {
		
		double getMean();
		@Value.Default @JsonIgnore
		default LinkFunction getLink() {return LinkFunction.NONE;}
		default double sample(Sampler s) {return (double) getMean(); }
	}
	
	public static BinomialDistribution binom(int n, double p) {
		return ImmutableBinomialDistribution
				.builder().setMean(n*p).setSd(n*p*(1-p))
				.build();
	}
	
	public static BinomialDistribution binom(double mean, double sd) {
		return ImmutableBinomialDistribution
				.builder().setMean(mean).setSd(sd)
				.build();
	}
	
	public static PoissonDistribution pois(double rate) {
		return ImmutablePoissonDistribution
				.builder()
				.setMean(rate)
				.build();
	}
	public static SimpleDistribution negBinom(double mean, double sd) {
		return ImmutableNegBinomialDistribution
				.builder().setMean(mean).setSd(sd)
				.build();
	}
	public static SimpleDistribution gamma(Double mean, Double sd) {
		return ImmutableGammaDistribution
				.builder().setMean(mean).setSd(sd)
				.build();
	}
	public static SimpleDistribution norm(Double mean, Double sd) {
		return ImmutableNormalDistribution
				.builder().setMean(mean).setSd(sd)
				.build();
	}
	public static SimpleDistribution logNorm(Double mean, Double sd) {
		return ImmutableLogNormalDistribution
				.builder().setMean(mean).setSd(sd)
				.build();
	}
	public static SimpleDistribution point(Double mean) {
		return ImmutablePointDistribution
				.builder().setMean(mean)
				.build();
	}
	public static SimpleDistribution uniform0(Double upper) {
		return ImmutableUniformDistribution
				.builder().setMax(upper)
				.build();
	}
	public static SimpleDistribution uniform() {
		return ImmutableUniformDistribution.builder().build();
	}
	public static UnimodalBetaDistribution unimodalBeta(Double mean, Double dispersion) {
		return ImmutableUnimodalBetaDistribution
				.builder().setMean(mean).setDispersion(dispersion)
				.build();
	}
	public static BetaDistribution beta(Double mean, Double sd) {
		return ImmutableBetaDistribution
				.builder().setMean(mean).setSd(sd)
				.build();
	}
	
	
	
	
	
	
}
