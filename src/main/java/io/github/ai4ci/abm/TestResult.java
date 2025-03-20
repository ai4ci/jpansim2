package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import io.github.ai4ci.config.ExecutionConfiguration;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.Sampler;


/**
 * Models a testing process
 */
@Value.Immutable
public interface TestResult extends Serializable {
	
	public static enum Type { 
		LFT (
			ImmutableTestParameters.builder()
				.setSensitivity(0.9)
				.setSpecificity(0.98)
				.setMeanTestDelay(0.0)
				.setSdTestDelay(0.0)
			), 
		
		PCR (ImmutableTestParameters.builder()
				.setSensitivity(0.8)
				.setSpecificity(0.995)
				.setMeanTestDelay(3.0)
				.setSdTestDelay(1.0)
			)
		;
		private TestParameters params;
		public TestParameters params() {return params;}
		public ImmutableTestParameters.Builder modify() {
			return ImmutableTestParameters.builder().from(params);
		} 
		private Type(ImmutableTestParameters.Builder params) {
			this.params = params
					.setTestName(this.name()).build();
		}
		
	}
	
	static List<TestParameters> defaultTypes() {
		return Arrays.asList(Type.values()).stream().map(t -> t.params())
				.collect(Collectors.toList());
	}
	
	public enum Result {PENDING,POSITIVE,NEGATIVE};
	
	// Viral load is normalised so that a value of 1 is infected and should be
	// detected at the sensitivity and specificity of the tests. This is to do
	// with the ease of separating signal from noise. The noise distribution 
	// upper quantile at a test cut off must be equal to specificity.
	// at the same test cut off the lower quantile of the signal distribution must
	// be equal to sensitivity. If we define a viral load of 1 as an infection 
	// that a perfectly sensitive test would pick up, then the sensitivity cut-off
	// will be greater than 1.
	// Problem is we don;t know anything about the signal distribution. and it 
	// is somewhat arbitrary because it is a circular definition, if we impose
	// a cut-off. There is some degree of non zero viral load that is essentially
	// irrelevant, and would be regarded as a false positive. This comes down to
	// how we normalise viral load. A cut off of 1 has to be designed that 
	// noise distribution quantile (spec)% is 1, and 1+noise distribution quantile
	// is (1-sens)% at 1
	// An alternative is that the 
	
	public double getViralLoadTruth();
	public long getTime();
	public TestParameters getTestParams();
	
	@Value.Derived public default double getViralLoadSample() {
		Sampler rng = Sampler.getSampler();
		return getTestParams().applyNoise(this.getViralLoadTruth(), rng);
	}
	
	@Value.Derived public default long getDelay() {
		Sampler rng = Sampler.getSampler();
		return Math.round(rng.logNormal(
				getTestParams().getMeanTestDelay(),
				getTestParams().getSdTestDelay()
		));
	}
	
	private static TestResult build(double viralLoad, long time, TestParameters testParams) {
		return ImmutableTestResult.builder()
			.setViralLoadTruth(viralLoad)
			.setTestParams(testParams)
			.setTime(time)
			.build();
	}
	
	default public boolean isResultToday(int day) {
		return day == getTime()+getDelay();
	}
	
	default public boolean isResultAvailable(int day) {
		return day >= getTime()+getDelay();
	}
	
	default public Result resultOnDay(int day) {
		if (!isResultAvailable(day)) return Result.PENDING;
		if (getFinalObservedResult()) return Result.POSITIVE;
		return Result.NEGATIVE;
	}
	
//	default public boolean confirmedPositive(int day) {
//		return (resultOnDay(day) == Result.POSITIVE);
//	}
//	
//	default public boolean confirmedNegative(int day) {
//		return (resultOnDay(day) == Result.NEGATIVE);
//	}
	
//	@Value.Derived default public Result getTrueResult() {
//		if (getViralLoadTruth() > 1) return Result.POSITIVE;
//		return Result.NEGATIVE;
//	}
	
	@Value.Derived default public boolean getFinalObservedResult() {
		return (getViralLoadSample() > 1);
	}
	
//	// what is the probability given a particular result that the person
//	// has disease? 
//	// If the disease is uncommon the positive test is still less likely
//	// to be due to disease. 
//	default double postTestProbPositive(int day, double prevalence) {
//		if (resultOnDay(day).equals(Result.POSITIVE)) {
//			return this.getTestParams().postPositiveTestProbability(prevalence);
//		} else if (resultOnDay(day).equals(Result.NEGATIVE)) {
//			return this.getTestParams().postNegativeTestProbability(prevalence);
//		} else {
//			return prevalence;
//		}
//	}
//	
//	default double postTestProbNegative(int day, double prevalence) {
//		return 1-postTestProbPositive(day, prevalence);
//	}
	
	@Value.Derived default double trueLogLikelihoodRatio() {
		return getFinalObservedResult() ?
			Math.log(this.getTestParams().positiveLikelihoodRatio()) :
			Math.log(this.getTestParams().negativeLikelihoodRatio()) ;	
	}
	
	default double logLikelihoodRatio(int day, int limit) {
		if (isResultCurrent(day, limit)) {
			return trueLogLikelihoodRatio();
		} else {
			return 0;
		}
	}
	
	
	
	default double adjustedLogLikelihoodRatio(int day, int limit) {
		// Test result not known yet
		if (day < getTime()+getDelay()) return 0;
		return Conversions.waneLogOdds(
				logLikelihoodRatio(day, limit), 
				day - getTime(), 
				limit);
		// Test has become irrelevant 
	}
	
	default public boolean isResultCurrent(long day, long recoveryTime) {
		return day >= getTime() && day < getTime() + recoveryTime;
	}
	
//	default public Optional<Result> publishedResult(int day) {
//		if (day != getTime()+getDelay()) return Optional.empty();
//		return Optional.of(this.resultOnDay(day));
//	}
		
	public static Optional<TestResult> resultFrom(PersonTemporalState testee, Type type) {
		return resultFrom(
				testee.getEntity().getOutbreak().getExecutionConfiguration(),
				testee.getNormalisedViralLoad(),
				testee.getTime(),
				type.name());
	}
	
	private static Optional<TestResult> resultFrom(ExecutionConfiguration cfg, double viralLoad, int time, String type) {
		
		return cfg.getAvailableTests()
				.stream()
				.filter(tp -> tp.getTestName().equalsIgnoreCase(type))
				.findFirst()
				.map(params -> {
					return TestResult.build(
						viralLoad, // true test status as test is testing infectiousness 
						time, // test date
						params
					);
				});
	}
	
}
