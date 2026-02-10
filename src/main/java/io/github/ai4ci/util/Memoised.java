package io.github.ai4ci.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Memoised is a utility class that implements memoization for a given function.
 * It caches the results of function calls to avoid redundant computations and
 * improve performance for expensive functions.
 * 
 * @param <X> the type of the input to the function
 * @param <Y> the type of the output of the function
 */
public class Memoised<X,Y> implements Function<X,Y> {

	Function<X,Y> fn;
	Map<X,Y> cache = new HashMap<>();
	
	private Memoised(Function<X,Y> fn) {
		this.fn = fn;
	}
	
	@Override
	/**
	 * Applies the memoized function to the given input. If the result for the input
	 * is already cached, it returns the cached value. Otherwise, it computes the
	 * result using the original function, caches it, and returns it.
	 * 
	 * @param t the input to the function
	 * @return the output of the function for the given input
	 */
	public Y apply(X t) {
		return cache.computeIfAbsent(t, fn);
	}
	
	/**
	 * Creates a memoized version of the given function. The returned function will
	 * cache results for previously computed inputs, improving performance for
	 * expensive functions.
	 * 
	 * @param <X> the type of the input to the function
	 * @param <Y> the type of the output of the function
	 * @param fn  the function to be memoized
	 * @return a memoized version of the given function
	 */
	public static <X,Y> Memoised<X,Y> memoise(Function<X,Y> fn) {
		return new Memoised<X,Y>(fn);
	}
}