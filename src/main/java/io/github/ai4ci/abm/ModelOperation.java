package io.github.ai4ci.abm;

import java.io.Serializable;
import java.util.function.Predicate;

import io.github.ai4ci.config.SetupConfiguration;
import io.github.ai4ci.util.Sampler;

public class ModelOperation {

	@FunctionalInterface public static interface TriConsumer<A,B,C> extends Serializable {
		void accept(A a,B b,C c);
	}
	
	@FunctionalInterface public static interface TriFunction<A,B,C,R> extends Serializable {
		R apply(A a,B b,C c);
	}
	
	@FunctionalInterface public static interface BiFunction<A,B,R> extends Serializable {
		R apply(A a,B b);
	}
	
	/**
	 * An abstract update stage comprises of a selector which allows you to 
	 * filter the input, using getSelector, then process the input using
	 * getConsumer..
	 * @param <FACTORY> the builder for the next 
	 * @param <INPUT> the type of object to update.
	 */
	public static class UpdateStage<FACTORY,INPUT> {
		Predicate<INPUT> selector;
		TriConsumer<FACTORY,INPUT,Sampler> consumer;
		UpdateStage(Predicate<INPUT> selector, TriConsumer<FACTORY,INPUT,Sampler> consumer) {
			this.selector = selector; 
			this.consumer = consumer;
		}
		public Predicate<INPUT> getSelector() {return selector;};
		public TriConsumer<FACTORY,INPUT,Sampler> getConsumer() {return consumer;}	
	}
	
	
	
	public static class PersonUpdater extends UpdateStage<Person, PersonState> {
		PersonUpdater(Predicate<PersonState> selector, TriConsumer<Person, PersonState, Sampler> consumer) {
			super(selector, consumer);
		}
	}
	
//	public static class TestingUpdater extends PersonUpdater {
//		TestingUpdater(
//				Predicate<PersonState> filter,
//				BiFunction<PersonState, Sampler, Pair<List<TestResult>, String>> mapper) {
//			super(filter, 
//				(a,b,c) -> {
//					Pair<List<TestResult>, String> tmp  =mapper.apply(b, c);
//					if (tmp.getKey() != null) a.getNextHistory().get().addAllTodaysTests(tmp.getKey());
//					if (tmp.getValue() != null) {
//						if (tmp.getValue() != "NO_CHANGE") {
//							a.getNextState().get().setTestingStrategy(TestingStrategy.valueOf(TestingStrategy.class, tmp.getValue()));
//						}
//						// Do nothing on no change.
//					} else {
//						a.getNextState().get().setTestingStrategy(b.getEntity().getBaseline().getDefaultTestingStrategy());
//					}
//			});
//		}
//		static TestingUpdater then(BiFunction<PersonState, Sampler, List<TestResult>> mapper, String next) {
//			return new TestingUpdater(a -> true,(a,rng) -> Pair.of(mapper.apply(a, rng), next));
//		}
//		static TestingUpdater thenDefault(BiFunction<PersonState, Sampler, List<TestResult>> mapper) {
//			return new TestingUpdater(a -> true, (a,rng) -> 
//					Pair.of(
//							mapper.apply(a,rng),
//							a.getEntity().getBaseline().getDefaultTestingStrategy().name()));
//		}
//		static TestingUpdater chooseIf(
//				Predicate<PersonState> filter,
//				BiFunction<PersonState, Sampler, Pair<List<TestResult>,String>> mapper) {
//			return new TestingUpdater(filter,mapper);
//		}
//		static TestingUpdater choose(
//				BiFunction<PersonState, Sampler, Pair<List<TestResult>,String>> mapper) {
//			return new TestingUpdater(a->true,mapper);
//		}
//		
//		static Pair<List<TestResult>,String> testThen(TestResult testResult, String nextTestStrategy) {
//			return Pair.of(Collections.singletonList(testResult), nextTestStrategy);
//		}
//		
//		static Pair<List<TestResult>,String> noopThen(String nextTestStrategy) {
//			return Pair.of(Collections.emptyList(), nextTestStrategy);
//		}
//		static Pair<List<TestResult>,String> noop() {
//			return Pair.of(Collections.emptyList(), "NO_CHANGE");
//		}
//		static Pair<List<TestResult>,String> noopThenDefault() {
//			return Pair.of(Collections.emptyList(), null);
//		}
//	}
//	
//	
//	public static class BehaviourUpdater extends PersonStateUpdater {
//		 BehaviourUpdater(
//				Predicate<Person> filter,
//				TriFunction<ImmutablePersonState.Builder, Person, Sampler, String> mapper) {
//			super(filter, 
//				(a,b,c) -> {
//					String tmp  =mapper.apply(a, b, c);
//					if (tmp != null) {
//						if (tmp != "NO_CHANGE") {
//							a.setBehaviourStrategy(BehaviourStrategy.valueOf(BehaviourStrategy.class, tmp));
//						}
//						// Do nothing on no change.
//					} else {
//						a.setBehaviourStrategy(b.getBaseline().getDefaultBehaviourStrategy());
//					}
//			});
//		}
//		
//		static BehaviourUpdater then(TriConsumer<ImmutablePersonState.Builder, Person, Sampler> mapper, String next) {
//			return new BehaviourUpdater(a -> true,
//					(a,b,rng) -> {
//						mapper.accept(a,b,rng); 
//						return next;
//					});
//		}
//		
//		static BehaviourUpdater thenDefault(TriConsumer<ImmutablePersonState.Builder, Person, Sampler> mapper) {
//			return new BehaviourUpdater(a -> true,
//					(a,b,rng) -> {
//						mapper.accept(a,b,rng); 
//						return b.getBaseline().getDefaultBehaviourStrategy().name();
//					});
//		}
//		
//		static BehaviourUpdater chooseIf(Predicate<Person> filter, TriFunction<ImmutablePersonState.Builder, Person, Sampler, String> mapper) {
//			return new BehaviourUpdater(filter,mapper);
//		}
//		
//		static BehaviourUpdater choose(
//				TriFunction<ImmutablePersonState.Builder, Person, Sampler, String> mapper) {
//			return new BehaviourUpdater(a->true,mapper);
//		}
//		
//		static String noop() {return "NO_CHANGE";}
//		static String thenDefault() {return null;}
//	}
//	
	// Generic updaters
	
	public static class OutbreakStateUpdater extends UpdateStage<ImmutableOutbreakState.Builder, Outbreak> {
		OutbreakStateUpdater(Predicate<Outbreak> selector, TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> consumer) {
			super(selector, consumer);
		}}
	
	public static class PersonStateUpdater extends UpdateStage<ImmutablePersonState.Builder, Person> {
		PersonStateUpdater(Predicate<Person> selector, TriConsumer<ImmutablePersonState.Builder, Person, Sampler> consumer) {
			super(selector, consumer);
		}
	}
	
	public static OutbreakStateUpdater updateOutbreakState(Predicate<Outbreak> selector, TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> consumer) {
		return new OutbreakStateUpdater(selector,consumer);
	}
	
	public static PersonStateUpdater updatePersonState(Predicate<Person> selector, TriConsumer<ImmutablePersonState.Builder, Person, Sampler> consumer) {
		return new PersonStateUpdater(selector,consumer);
	}
	
	// Constructor methods ----
	
	public static class OutbreakSetup extends UpdateStage<ModifiableOutbreak, SetupConfiguration> {
		OutbreakSetup(Predicate<SetupConfiguration> selector, TriConsumer<ModifiableOutbreak,  SetupConfiguration, Sampler> consumer) {
			super(selector, consumer);
		}}
	
	public static OutbreakSetup setupOutbreak(TriConsumer<ModifiableOutbreak,  SetupConfiguration, Sampler> consumer) {
		return new OutbreakSetup(
				x -> {
					if (x != null) return true;
					throw new RuntimeException("Outbreak is not configured correctly for setup.");
				},
				consumer);
	}
	
	public static class PersonBaseliner extends UpdateStage<ImmutablePersonBaseline.Builder, Person> {
		PersonBaseliner(Predicate<Person> selector, TriConsumer<ImmutablePersonBaseline.Builder, Person, Sampler> consumer) {
			super(selector, consumer);
		}}
	
	public static PersonBaseliner baselinePerson(TriConsumer<ImmutablePersonBaseline.Builder, Person, Sampler> consumer) {
		return new PersonBaseliner(
				p -> {
					// Filter only to people that have been fully initialised
					// TODO: realistically this should throw exceptions
					// rather that skip the setup if it is not ready
					if (p.getOutbreak() instanceof ModifiableOutbreak m) {
						if (m.initialisedSetupConfiguration() &&
								m.initialisedExecutionConfiguration() &&
								m.initialisedSocialNetwork()
						) return true;
					} 
					throw new RuntimeException("Person is not configured correctly for baselining.");
				},
				consumer); 
	}
	
	public static class OutbreakBaseliner extends UpdateStage<ImmutableOutbreakBaseline.Builder, Outbreak> {
		OutbreakBaseliner(Predicate<Outbreak> selector, TriConsumer<ImmutableOutbreakBaseline.Builder, Outbreak, Sampler> consumer) {
			super(selector, consumer);
		}}
	
	public static OutbreakBaseliner baselineOutbreak(TriConsumer<ImmutableOutbreakBaseline.Builder, Outbreak, Sampler> consumer) {
		return new OutbreakBaseliner(
				o -> {
					if (o instanceof ModifiableOutbreak m) {
						if(
								!m.getPeople().isEmpty() &&
								m.initialisedSetupConfiguration() &&
								m.initialisedExecutionConfiguration() &&
								m.initialisedSocialNetwork()
							) return true;
					} 
					throw new RuntimeException("Outbreak is not configured correctly for baselining.");
				},
				consumer); 
	}
	
	public static class PersonInitialiser extends UpdateStage<ImmutablePersonState.Builder, Person> {
		PersonInitialiser(Predicate<Person> selector, TriConsumer<ImmutablePersonState.Builder, Person, Sampler> consumer) {
			super(selector, consumer);
		}}
	
	public static PersonInitialiser initialisePerson(Predicate<Person> selector, TriConsumer<ImmutablePersonState.Builder, Person, Sampler> consumer) {
		return new PersonInitialiser(
				p -> {
					if (!selector.test(p)) return false;
					if (p instanceof ModifiablePerson mp) {
						if (p.getOutbreak() instanceof ModifiableOutbreak m) {
							if (
									mp.initialisedBaseline() &&
									m.initialisedSetupConfiguration() &&
									m.initialisedExecutionConfiguration() &&
									m.initialisedSocialNetwork()
								) return true;
						} 
					}
					throw new RuntimeException("Person is not configured correctly for initialisation.");
				},
				consumer); 
	}
	
	public static class OutbreakInitialiser extends UpdateStage<ImmutableOutbreakState.Builder, Outbreak> {
		OutbreakInitialiser(Predicate<Outbreak> selector, TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> consumer) {
			super(selector, consumer);
		}}
	
	public static OutbreakInitialiser initialiseOutbreak(TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> consumer) {
		return new OutbreakInitialiser(
				o -> {
						if (o instanceof ModifiableOutbreak m) {
							if ( 
									m.initialisedBaseline() &&
									m.initialisedSetupConfiguration() &&
									m.initialisedExecutionConfiguration() &&
									m.initialisedSocialNetwork()
							) return true;
						} 
						throw new RuntimeException("Outbreak is not configured correctly for initialisation.");
				},
				consumer); 
	}
}