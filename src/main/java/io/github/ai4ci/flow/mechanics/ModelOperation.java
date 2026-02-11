package io.github.ai4ci.flow.mechanics;

import java.io.Serializable;
import java.util.function.Predicate;

import io.github.ai4ci.abm.ImmutableOutbreakState;
import io.github.ai4ci.abm.ImmutablePersonState;
import io.github.ai4ci.abm.Outbreak;
import io.github.ai4ci.abm.Person;
import io.github.ai4ci.util.Sampler;

/**
 * Utility class for defining custom model operations that can be used in the
 * {@link io.github.ai4ci.flow.mechanics.Updater} to create custom update stages
 * for outbreaks and people.
 *
 */
public class ModelOperation {

	/**
	 * BiFunction is not used in the current implementation but is provided for
	 * potential future use cases where a two-parameter function might be needed.
	 * It follows the same pattern as TriFunction, ensuring it is serializable
	 * and can be used in similar contexts.
	 *
	 * @param <A> the type of the first argument
	 * @param <B> the type of the second argument
	 * @param <R> the type of the return value
	 *
	 */
	@FunctionalInterface
	public static interface BiFunction<A, B, R> extends Serializable {
		/**
		 * Applies this function to the given arguments.
		 *
		 * @param a the first function argument
		 * @param b the second function argument
		 * @return the function result
		 */
		R apply(A a, B b);
	}

	/**
	 * An Update stage processor for outbreaks.
	 *
	 * @see ModelOperation#updateOutbreakState(Predicate, TriConsumer)
	 */
	public static class OutbreakStateUpdater
			extends UpdateStage<ImmutableOutbreakState.Builder, Outbreak> {
		OutbreakStateUpdater(
				Predicate<Outbreak> selector,
				TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> consumer
		) {
			super(selector, consumer);
		}
	}

	/**
	 * An Update stage processor for people.
	 *
	 * @see ModelOperation#updatePersonState(Predicate, TriConsumer)
	 */
	public static class PersonStateUpdater
			extends UpdateStage<ImmutablePersonState.Builder, Person> {
		PersonStateUpdater(
				Predicate<Person> selector,
				TriConsumer<ImmutablePersonState.Builder, Person, Sampler> consumer
		) {
			super(selector, consumer);
		}
	}

	/**
	 * Functional interfaces for the selector and consumer functions used in
	 * update stages. These are defined here to avoid dependencies on external
	 * libraries and to ensure they are serializable for use in distributed
	 * contexts.
	 *
	 * @param <A> the type of the first argument
	 * @param <B> the type of the second argument
	 * @param <C> the type of the third argument
	 */
	@FunctionalInterface
	public static interface TriConsumer<A, B, C> extends Serializable {
		/**
		 * Performs this operation on the given arguments.
		 *
		 * @param a the first input argument
		 * @param b the second input argument
		 * @param c the third input argument
		 */
		void accept(A a, B b, C c);
	}

	// Generic updaters these are used in the updater
	// and see ModelUpdate for where this infrastructure is used.

	/**
	 * TriFunction is defined for potential future use cases where a
	 * three-parameter function might be needed. It follows the same pattern as
	 * TriConsumer, ensuring it is serializable and can be used in similar
	 * contexts where a return value is required.
	 *
	 * @param <A> the type of the first argument
	 * @param <B> the type of the second argument
	 * @param <C> the type of the third argument
	 * @param <R> the type of the return value
	 */
	@FunctionalInterface
	public static interface TriFunction<A, B, C, R> extends Serializable {
		/**
		 * Applies this function to the given arguments.
		 *
		 * @param a the first function argument
		 * @param b the second function argument
		 * @param c the third function argument
		 * @return the function result
		 */
		R apply(A a, B b, C c);
	}

	/**
	 * An abstract update stage comprises of a selector which allows you to
	 * filter the input, using getSelector, then process the input using
	 * getConsumer..
	 *
	 * @param <FACTORY> the builder for the next
	 * @param <INPUT>   the type of object to update.
	 */
	public static class UpdateStage<FACTORY, INPUT> {
		Predicate<INPUT> selector;
		TriConsumer<FACTORY, INPUT, Sampler> consumer;

		private UpdateStage(
				Predicate<INPUT> selector,
				TriConsumer<FACTORY, INPUT, Sampler> consumer
		) {
			this.selector = selector;
			this.consumer = consumer;
		}

		/**
		 * Getters for the selector and consumer functions. These allow the update
		 * stages to be used in the Updater without exposing the internal
		 * implementation details.
		 *
		 * @return the consumer function for this update stage
		 */
		public TriConsumer<FACTORY, INPUT, Sampler> getConsumer() {
			return this.consumer;
		}

		/**
		 * Getters for the selector and consumer functions. These allow the update
		 * stages to be used in the Updater without exposing the internal
		 * implementation details.
		 *
		 * @return the selector function for this update stage
		 */
		public Predicate<INPUT> getSelector() { return this.selector; }
	}

	/**
	 * Creates a custom updating function that runs against an outbreak when the
	 * selector is matched. The updating function can change anything about the
	 * future state of the outbreak.
	 *
	 * @param selector selects whether to run this updater for a given Outbreak
	 * @param consumer a three parameter function taking the builder, current
	 *                 outbreak, and a RNG.
	 * @return An updater suitable for
	 *         {@link io.github.ai4ci.flow.mechanics.Updater#withOutbreakProcessors(OutbreakStateUpdater...)}
	 */
	public static OutbreakStateUpdater updateOutbreakState(
			Predicate<Outbreak> selector,
			TriConsumer<ImmutableOutbreakState.Builder, Outbreak, Sampler> consumer
	) {
		return new OutbreakStateUpdater(selector, consumer);
	}

	/**
	 * Creates a custom updating function that runs against an person when the
	 * selector is matched. The updating function can change anything about the
	 * future state of the person.
	 *
	 * @param selector selects whether to run this updater for a given Person
	 * @param consumer a three parameter function taking the builder, current
	 *                 outbreak, and a RNG.
	 * @return An updater suitable for
	 *         {@link io.github.ai4ci.flow.mechanics.Updater#withPersonProcessors(PersonStateUpdater...)}
	 */
	public static PersonStateUpdater updatePersonState(
			Predicate<Person> selector,
			TriConsumer<ImmutablePersonState.Builder, Person, Sampler> consumer
	) {
		return new PersonStateUpdater(selector, consumer);
	}

}