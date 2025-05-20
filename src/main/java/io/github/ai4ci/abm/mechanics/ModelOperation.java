package io.github.ai4ci.abm.mechanics;

import java.io.Serializable;
import java.util.function.Predicate;

import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
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
	
	
	// Generic updaters these are used in the updater
	// and see ModelUpdate for where this infrastructure is used.
	
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
	

}