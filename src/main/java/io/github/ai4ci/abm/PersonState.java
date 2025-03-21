package io.github.ai4ci.abm;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.immutables.value.Value;

import io.github.ai4ci.abm.TestResult.Result;
import io.github.ai4ci.abm.TestResult.Type;
import io.github.ai4ci.util.Conversions;
import io.github.ai4ci.util.ModelNav;
import io.github.ai4ci.util.Sampler;

@Value.Immutable
public interface PersonState extends PersonTemporalState {
	
	// Optional<Integer> getLastTestedTime(); 
	// Optional<Integer> getLastInfectedTime();
	// @Value.Default default Double getInfectionRisk() {return 0D;}
	// @Value.Default default Double getKnownInfectionRisk() {return 0D;}
	// @Value.Default default Boolean isScreened() {return false;}
	
	@Value.Default default Double getTransmissibilityModifier() {return 1.0D;}
	@Value.Default default Double getMobilityModifier() {return 1.0D;}
	@Value.Default default Double getComplianceModifier() {return 1.0D;}
	@Value.Default default Double getContactDetectedProbability() {return 1.0D;}
	
	@Value.Default default Double getImmuneModifier() {return 1.0D;}
	@Value.Default default Double getScreeningInterval() {return 7D;}
	@Value.Default default Double getImportationExposure() {return 0D;}
	
	/**
	 * An immunisation dose is a fraction of dormant immune cells that are 
	 * activated by any immunising exposure in the previous day. This is scaled
	 * to the total remaining immune immune capacity. Setting this to something
	 * non zero implies that the person was immunised at this time point. 
	 * @return
	 */
	// TODO: need to think about this as a protocol or strategy like testing
	@Value.Default default Double getImmunisationDose() {return 0D;}
	
//	@Value.Default default TestingStrategy getTestingStrategy() {
//		return this.getEntity().getBaseline().getDefaultTestingStrategy();
//	}
//	@Value.Default default BehaviourStrategy getBehaviourStrategy() {
//		return this.getEntity().getBaseline().getDefaultBehaviourStrategy();
//	}
	
//	@Value.Default default StateMachine getStateMachine() {
//		return StateMachine.from(getEntity());
//	}
	
	@Value.Default default ViralLoadState getViralLoad() {
		// TODO: is initialising this a good idea? 
		return ViralLoadState.initialise(this.getEntity());
	}
	
	/**
	 * A normalised severity index where 1 is symptomatic.
	 */
	default Double getNormalisedSeverity() {
		return getViralLoad().getNormalisedSeverity();
	}
	
	/**
	 * A normalised viral load index where 1 is infectious.
	 */
	default Double getNormalisedViralLoad() {
		return getViralLoad().getNormalisedViralLoad();
	}
	
	/**
	 * Is the persons internal infected targets above the threshold for 
	 * exhibiting symptoms.
	 */
	@Value.Derived default boolean isSymptomatic() {
		double adjSev = TestParameters.applyNoise(
				getNormalisedSeverity(), 
				this.getEntity().getBaseline().getSymptomSensitivity(),
				this.getEntity().getBaseline().getSymptomSpecificity(),
				Sampler.getSampler()
			);
		return adjSev > 1;}
	
	@Value.Derived default boolean isReportedSymptomatic() {
		// assume user is too lazy to lie about symptoms
		if (!isSymptomatic()) return false;
		// TODO: maybe this should be generic for all tests.
		// some form of probablility of reporting LFT and symptoms.
		// However this is a very specific problem for self administered tests
		// unless you consider that the app may not have access to the canonical
		// test results.... unlikely.
		// reporting of tests is an individual behaviour so this needs to be
		// done independent of tests. It is however result specific
		// for LFTs and reporting of negatives was not done.  If this is the 
		// case 
		double p = ModelNav.baseline(this).getAppUseProbability();
		return Sampler.getSampler().bern(p);
	}
	
	default  boolean isSymptomaticConsecutively(int days) {
		return ModelNav.history(this, days)
			.takeWhile(h -> h.isSymptomatic())
			.count() >= days;
	}
	
	/**
	 * Probability of contact given a fully mobile partner. The joint probability
	 * will define the likelihood of a contact. (Plus some randomness) 
	 * @return
	 */
	default Double getAdjustedMobility() {
		double tmp = 
			Conversions.scaleProbability(
				this.getEntity().getBaseline().getMobilityBaseline(),
				this.getMobilityModifier()
			);
		return tmp;
	};
	
	/**
	 * Probability of transmission given a contact with a completely
	 * unprotected partner. A lower value means less likely transmission.
	 * This is is going to be a function of compliance
	 * @return
	 */
	default Double getAdjustedTransmissibility() {
		double tmp = 
			Conversions.scaleProbability(
				this.getEntity().getBaseline().getTransmissibilityBaseline(),
				this.getTransmissibilityModifier()
			);
		return tmp;
	};
	
	/**
	 * Probability of transmission given a contact with a completely
	 * unprotected partner. A lower value means less likely transmission.
	 * This is is going to be a function of compliance
	 * @return
	 */
	default Double getAdjustedCompliance() {
		return 
			Conversions.scaleProbability(
				this.getEntity().getBaseline().getComplianceBaseline(),
				this.getComplianceModifier()
			);
	};
	
	/**
	 * TODO: this needs review. It determines if the most recent test regardless of whether
	 * there is a result or it is still pending. This is how it needs to be for
	 * determining whether or not to do another test and is used in teh testing 
	 * strategies. It is a bad choice for anything else.
	 * @param r
	 * @return
	 */
	default boolean isLastTestExactly(Result r) {
		// This is looking at the day behind for test results 
		// because at the point this is run in the update cycle the 
		// history has not yet been updated.
		Optional<Result> o = this
				.getLastTest()
				.map(tr -> tr.resultOnDay( this.getTime()));
		return o.isPresent() && o.get().equals(r);
	}
	
	default boolean isCompliant(Sampler rng) {
		return rng.uniform() < this.getAdjustedCompliance();
	}
	
	@Value.Derived default boolean isCompliant() {
		Sampler rng = Sampler.getSampler();
		return isCompliant(rng);
	}
	
	@Value.Lazy default String getBehaviour() {
		return this.getEntity().getStateMachine().getState().getName();
	}
	
	/**
	 * A normalised viral load dose, this is converted into virion numbers later
	 * and it is the sum of all the exposures over the course of a day. This is
	 * always going to work one day behind as the viral load model can only pick 
	 * up the change for the next day. This is derived form the contact network
	 * when that is initialised.
	 * 
	 * N.B. the dose maxes out at 20 times the infectious viral load. This 
	 * prevents ridiculously large initial viral doses when uncontrolled 
	 * exponential growth. I could have done this in the getEposure() method of 
	 * contact to control the maximum amount one individual can expose another by.
	 * 
	 * @return
	 */
	@Value.Lazy default double getVirionExposure() {
		return Math.min(20, ModelNav.history(this).stream()
				.flatMap(ph -> ph.getTodaysContacts().stream())
				.mapToDouble(p -> p.getExposure())
				.sum());
	}
	
	/** 
	 * Looks at whether there are any tests of specified type within the last
	 * x days.
	 * @param type
	 * @return
	 */
	default boolean isRecentlyTested(Type type) {
		return getTests().filter(t-> type.params().getTestName().equals(t.getTestParams().getTestName()))
				.findFirst().isPresent();
	};
	
	/**
	 * Testing indicated if the person has been symptomatic for a set number
	 * of days and has not had a test within a set number of days (regardless of
	 * result)
	 * @param symptomDays
	 * @param noRepeatTestDays
	 * @return
	 */
	default boolean isSymptomaticTestingIndicatedToday(int symptomDays) {
		return this.isSymptomaticConsecutively(symptomDays) && !this.isRecentlyTested(Type.PCR);
	}
	
	private int limit() {
		return ModelNav.modelState(this).getPresumedInfectiousPeriod();
	}
	
	
	/**
	 * Estimate the local prevalence for an individual. This is a sum of all the
	 * persons contacts presumed risk. I.e. if a contact has a positive test 
	 * result their risk will be higher. 
	 * @return
	 */
	@Value.Lazy default double getPresumedLocalPrevalence() {
		// double prev2 = ModelNav.modelState(this).getPresumedTestPositivePrevalence();
		double prev =  getContacts()
			.filter(c -> c.isDetected())
			.flatMap(c -> c.getParticipant().getCurrent(this.getEntity().getOutbreak()).stream())
			.mapToDouble(ph -> ph.getProbabilityInfectiousToday())
			.average()
			.orElse(0D);
		// The local prevalence
		return prev != 0 ? prev :
			// prev2 !=0 ? prev2 :
				0.0001;
	}
	
	/**
	 * Looks at the previous tests and identifies if there was a positive 
	 * test result within the infectious period. This assumes a prior 
	 * probability of disease = local prevalence and the likelihood of 
	 * each observed test outcome.
	 * @return
	 */
	@Value.Lazy default double getProbabilityInfectiousToday() {
		
		double logOdds = 
				Conversions.logit(this.getPresumedLocalPrevalence()) +
				//Conversions.logit(ModelNav.modelState(this).getPresumedTestPositivePrevalence()) +
//				Conversions.logit((
//						this.getPresumedLocalPrevalence()+
//						ModelNav.modelState(this).getPresumedTestPositivePrevalence())/2.0) +
				getDirectLogLikelihood();
		
		return Conversions.expit(logOdds);
	};
	
	/** 
	 * the log likelihood of disease given the history of tests. 
	 * The value of this will change in the future as more 
	 * test results become available. We can't pre-calculate this, and there is 
	 * little point anyway as it only is used directly for this individual at
	 * one point in time.
	 * @return
	 */
	default double getTestLogLikelihood() {
		return this.getTests()
			.mapToDouble(t -> t.logLikelihoodRatio(this.getTime(), limit()) )
			.sum();
	}
	
	/**
	 * The log likelihood of disease given the history of symptoms.
	 * Each day of symptoms in the presumed infectious period increases the 
	 * chance the patient is identified as symptomatic. Adjusted for recency of
	 * symptoms. This quantity doesn't change over time so we can precalculate
	 * it
	 * @return
	 */
	@Value.Derived default double getSymptomLogLikelihood() {
		int samples = 5;
		//return 0; 
		return ModelNav
			.history(this, samples)
			.mapToDouble(ph -> 
//				Conversions.wanLogOdds(
//						sympLogLik(this, ph.isReportedSymptomatic()), 
//						this.getTime() - ph.getTime(), 
//						samples)
				symptomLogLik(this, ph.isReportedSymptomatic())
			)
			.average().orElse(0);
			//.sum();
	}
	
	private static double symptomLogLik(PersonState pers, boolean isKnownSymptomatic) {
		double adoption = ModelNav.modelParam(pers).getAppUseProbability().getCentral();
		double sens = ModelNav.modelState(pers).getPresumedSymptomSensitivity() * adoption;
		double spec = ModelNav.modelState(pers).getPresumedSymptomSpecificity();
		// Assume that the app only knows about positive symptoms, and not all
		// positive symptoms reported.
		return	isKnownSymptomatic ?
					// Value of a symptom
					Math.log(sens / (1-spec)) : 
						// Value of a not reported symptom: 
						Math.log((1-sens) / (spec))
				;
	}
	
	/** 
	 * The log likelihood of disease given exposure to contacts, adjusted for 
	 * the proximity of the contact.     
	 * @return
	 */
	default double logLikelihoodContacts() {
//		Pair<Double,Double> out = 
//			this.getContacts()
//				.filter(c -> c.isDetected())
//				.map(c -> c.getProximityDuration() *
//						c.getParticipant(this.getEntity().getOutbreak())
//							.getDirectLogLikelihood(
//								this.getTime(), 
//								limit()))
//				.collect(Collectors.teeing(
//						// Collectors.filtering(d -> d>0, Collectors.averagingDouble(d -> (double) d)),
//						Collectors.filtering(d -> d>0, Collectors.summingDouble(d -> (double) d)),
////						Collectors.filtering(d -> d>0,
////								Collectors.collectingAndThen(
////										Collectors.maxBy(Double::compareTo),
////										o -> o.orElse(0D)
////								)),
//						Collectors.filtering(d -> d<0, Collectors.averagingDouble(d -> (double) d)), 
//						Pair::of));
//		if (out.getLeft() > 0) return out.getLeft();
//		return out.getRight();
		
		return 
				this.getContacts()
					.filter(c -> c.isDetected())
					.mapToDouble(c -> 
							c.getProximityDuration() *
							c.getParticipant(this.getEntity().getOutbreak())
								.getDirectLogLikelihood(
									this.getTime(), 
									limit()))
					.max().orElse(0);
	}
	
	@Value.Lazy default double getTotalLogLikelihood() {
		double tests = getTestLogLikelihood();
		double symptoms = getSymptomLogLikelihood();
		double contacts = logLikelihoodContacts();
		return tests + symptoms + contacts; 
	}
	
	@Value.Lazy default double getDirectLogLikelihood() {
		double tests = getTestLogLikelihood();
		double symptoms = getSymptomLogLikelihood();
		return tests + symptoms; 
	}
	
	/**
	 * All the tests done in the last presumed infectious period
	 * @return
	 */
	default Stream<TestResult> getTests() {
		return ModelNav.history(this, limit())
			.flatMap(ph -> ph.getTodaysTests().stream());
	}
	
	/**
	 * Most recent test whether or not there is a result
	 * @return
	 */
	@Value.Lazy default Optional<TestResult> getLastTest() {
		return getTests().findFirst();
	}
	
	/**
	 * Most recent test with a result
	 * @return
	 */
	@Value.Lazy default Optional<TestResult> getLastResult() {
		return getTests()
				.filter(t -> t.isResultAvailable(this.getTime()))
				.findFirst();
	}
	
	/**
	 * The collection of test results for an individual that generate a result
	 * today. 
	 * @param time
	 * @return
	 */
	@Value.Lazy default List<TestResult> getResults() {
		return this.getTests()
			.filter(r -> r.isResultToday(this.getTime()))
			.collect(Collectors.toList());
	};
	
	/**
	 * Reassemble the weighted contacts from the PersonHistory
	 * graph within the infectious period.  
	 * @return
	 */
	default Stream<Contact> getContacts() {
		return ModelNav.history(this, limit())
			.flatMap(ph -> ph.getTodaysContacts().stream());
	}
}