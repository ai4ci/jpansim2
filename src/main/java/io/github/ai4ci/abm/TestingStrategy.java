//package io.github.ai4ci.abm;
//
//import static io.github.ai4ci.abm.ModelOperation.TestingUpdater.*;
//
//import java.util.Collections;
//import java.util.Optional;
//
//import io.github.ai4ci.abm.ModelOperation.TestingUpdater;
//import io.github.ai4ci.abm.TestResult.Result;
//import io.github.ai4ci.abm.TestResult.Type;
//import io.github.ai4ci.util.Conversions;
//
///**
// * Called during an update cycle before any changes have been made
// * This means any references to state refers to current state, but 
// * any references to history refer to the previous state.
// */
//@Deprecated
//public enum TestingStrategy {
//	
//	
//	/**
//	 * No test and return to baseline behaviour.
//	 */
//	NONE (thenDefault(
//		(a,rng) -> Collections.emptyList()
//	)),
//	
//	/**
//	 * Patient will probably test if they have symptoms, then wait for the
//	 * result.
//	 */
//	ON_SYMPTOMS (chooseIf(
//		a -> a.isSymptomatic(),
//		(a,rng) -> {
//			if (rng.uniform() > a.getComplianceProbability()) return noop();
//			return testThen(
//				TestResult.resultFrom(a, Type.PCR).get(),
//				"AWAIT_RESULT_PCR"
//			);
//		}
//	)),
//	
//	AWAIT_RESULT_LFT (chooseIf(
//		a -> !a.isLastTestExactly(Result.PENDING),
//		(a,rng) -> {
//			if (a.isLastTestExactly(Result.POSITIVE )) return testThen(
//					TestResult.resultFrom(a, Type.PCR).get(),
//					"AWAIT_RESULT_PCR"
//				);
//			// missing or Negative 
//			return noopThenDefault();
//		} 
//	)),
//	
//	AWAIT_RESULT_PCR (chooseIf(
//			a -> !a.isLastTestExactly(Result.PENDING),
//			(a,rng) -> {
//				if (a.isLastTestExactly(Result.POSITIVE)) return noopThen("DONT_REPEAT");
//				// missing or negative 
//				return noopThenDefault(); 
//			}
//	)),
//	
//	/**
//	 * Will stay in this state randomly for average of 5 days then will return
//	 * to the baseline strategy
//	 * TODO: parameterise this maybe?
//	 */
//	DONT_REPEAT (choose(
//			(a,rng) -> {
//				if (rng.uniform() < Conversions.probabilityFromPeriod(5)) return noopThenDefault();
//				return noop();
//			}
//	)),
//	
//	/** 
//	 * Randomly will conduct a LFT depending on individual screening interval
//	 * Then will wait for the result, and follow up with a PCR
//	 */
//	SCREEN_LFT (choose(
//		(a,rng) -> {
//			if (rng.uniform() > a.getComplianceProbability()) return noop();
//			double p = Conversions.probabilityFromPeriod(a.getScreeningInterval());
//			if (rng.uniform() > p) return noop();
//			return testThen(
//					TestResult.resultFrom(a,Type.LFT).get(),
//					"AWAIT_RESULT_LFT"
//				);
//		}
//	))
//	
//	;
//	// IMPLEMENTATION DETAILS
//	
//	private TestingUpdater fn;
//	private TestingStrategy(TestingUpdater update) {
//		this.fn = update;
//	}
//	public TestingUpdater fn() {return fn;}
//	
//	
//	
//}
