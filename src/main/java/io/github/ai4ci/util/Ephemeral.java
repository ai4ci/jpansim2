package io.github.ai4ci.util;

import java.io.Serializable;
import java.util.Optional;

/** 
 * An optional value that is not serialised. It is used to 
 * temporarily hold the next state of a Person or Outbreak during the update 
 * cycle, and is cleared after the cycle. 
 * @param <T>
 */
public class Ephemeral<T> implements Serializable {
	
	transient T value;
	
	public Ephemeral(T value) {
		this.value = value;
	}
	
	public Ephemeral<T> clear() {
		value = null;
		return this;
	}
	
	public static <T2> Ephemeral<T2> of(T2 value) {
		return new Ephemeral<T2>(value);
	}
	
	public static <T2> Ephemeral<T2> empty() {
		return new Ephemeral<T2>(null);
	}
	
	public Optional<T> toOptional() {
		return Optional.ofNullable(value);
	}

	public T get() {
		return toOptional().get();
	}
	
	public boolean isPresent() {
		return toOptional().isPresent();
	}

}
