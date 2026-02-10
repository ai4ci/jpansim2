package io.github.ai4ci.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Given a stream of objects of type {@code X}, and a mapping that extracts objects of
 * type {@code Y}, this class creates an index of type {@code Y} and acts as a function for
 * retrieving the set of {@code X} which map to {@code Y}. This is basically the same as a
 * foreign key lookup.
 *
 * @param <Y> a key-like object
 * @param <X> a value-like object
 */
public class Inversion<Y,X> implements Function<Y,Set<X>> {

	Function<X,Y> fn;
	Map<Y,Set<X>> cache = new HashMap<>();
	Class<Y> input = null;
	Class<X> output = null;
	
	@SuppressWarnings("unchecked")
	private Inversion(Function<X,Y> fn, Stream<X> data) {
		this.fn = fn;
		data.forEach(x -> {
			var y = fn.apply(x);
			if (input == null) input = (Class<Y>) y.getClass();
			if (output == null) output = (Class<X>) x.getClass();
			cache.computeIfAbsent(y, v -> new HashSet<>()).add(x);
		});
	}
	
	/**
	 * Apply the inversion function to retrieve the set of X that map to Y.
	 *
	 * @param t the key-like object for which to retrieve the corresponding set of value-like objects
	 * @return a set of X objects that map to the given Y object; returns an empty set if no mappings exist
	 */
	@Override
	public Set<X> apply(Y t) {
		return cache.getOrDefault(t, Collections.emptySet());
	}
	
	/**
	 * Apply the inversion function with type checking to retrieve the set of X that map to Y.
	 *
	 * @param <Y1> the type of the key-like object for which to retrieve the corresponding set of value-like objects; should be compatible with Y
	 * @param <X1> the expected type of the value-like objects in the output
	 * @param t the key-like object for which to retrieve the corresponding set of value-like objects
	 * @param as the class object representing the expected output type; used for type checking
	 * @return a set of X objects that map to the given Y object; returns an empty set if no mappings exist
	 * @throws IllegalArgumentException if the input type of t does not match the expected input type or if the output type does not match the expected output type
	 */
	public <Y1,X1> Set<X1> apply(Y1 t, Class<X1> as) {
		if (!input.isAssignableFrom(t.getClass())) throw new 
			IllegalArgumentException("Incorrect input type: "+t.getClass().getName()+" should be a "+input.getName());
		if (!as.isAssignableFrom(output)) throw new 
		IllegalArgumentException("Incorrect output type: "+as.getName()+" should be a "+output.getName());
		Y y = input.cast(t);
		return cache.getOrDefault(y, Collections.emptySet()).stream()
				.map(x -> as.cast(x)).collect(Collectors.toSet());
	}
	
	/**
	 * Apply the inversion function without type checking to retrieve the set of X that map to Y.
	 *
	 * @param <Y1> the type of the key-like object for which to retrieve the corresponding set of value-like objects; should be compatible with Y
	 * @param t the key-like object for which to retrieve the corresponding set of value-like objects
	 * @return a set of X objects that map to the given Y object; returns an empty set if no mappings exist
	 * @throws IllegalArgumentException if the input type of t does not match the expected input type
	 */
	public <Y1> Set<X> applyUnsafe(Y1 t) {
		if (!input.isAssignableFrom(t.getClass())) throw new 
			IllegalArgumentException("Incorrect input type: "+t.getClass().getName()+" should be a "+input.getName());
		Y y = input.cast(t);
		return cache.getOrDefault(y, Collections.emptySet());
	}
	
	/**
	 * Get the input type of the inversion function.
	 *
	 * @return the Class object representing the input type Y
	 */
	public Class<X> outputType() {
		return output;
	}
	
	/**
	 * cache the inversion of a function over a stream of data.
	 * 
	 * @param <Y> the type of the key-like objects to retrieve
	 * @param <X> the type of the value-like objects to retrieve
	 * @param fn the function to invert; should be a mapping from X to Y
	 * @param data the stream of X objects to process and cache
	 * @return an Inversion object that can be used to retrieve sets of X for given Y values
	 */
	public static <Y,X> Inversion<Y,X> cache(Function<X,Y> fn, Stream<X> data) {
		return new Inversion<Y,X>(fn, data);
	}
}