package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Optional;

/**
 * An optional value that is serializable. It is used to temporarily hold the
 * next state of a Person or Outbreak during the update cycle, and is cleared
 * after the cycle. It can be cloned and serialized.
 *
 * @param <T> the type of the value being held
 */
public class Ephemeral<T> implements Serializable {

	/**
	 * Creates a new Ephemeral instance with no value (null).
	 *
	 * @param <T2> the type of the value to hold
	 * @return a new Ephemeral instance containing null
	 */
	public static <T2> Ephemeral<T2> empty() {
		return new Ephemeral<>(null);
	}

	/**
	 * Creates a new Ephemeral instance holding the specified value.
	 *
	 * @param <T2>  the type of the value to hold
	 * @param value the value to hold; can be null
	 * @return a new Ephemeral instance containing the given value
	 */
	public static <T2> Ephemeral<T2> of(T2 value) {
		return new Ephemeral<>(value);
	}

	transient T value;

	/**
	 * Constructs an Ephemeral instance with the given value.
	 *
	 * @param value the initial value to hold; can be null
	 */
	public Ephemeral(T value) {
		this.value = value;
	}

	/**
	 * Clears the held value by setting it to null.
	 *
	 * @return this Ephemeral instance with the value cleared
	 */
	public Ephemeral<T> clear() {
		this.value = null;
		return this;
	}

	/**
	 * Retrieves the held value.
	 *
	 * @return the held value; may be null if no value is present
	 * @throws java.util.NoSuchElementException if no value is present (i.e., if
	 *                                          the value is null)
	 */
	public T get() {
		return this.toOptional().get();
	}

	/**
	 * Checks if a value is present.
	 *
	 * @return true if a value is present (i.e., the held value is not null),
	 *         false otherwise
	 */
	public boolean isPresent() { return this.toOptional().isPresent(); }

	/**
	 * Converts the held value to an Optional.
	 *
	 * @return an Optional containing the held value, or empty if the value is
	 *         null
	 */
	public Optional<T> toOptional() {
		return Optional.ofNullable(this.value);
	}

}
