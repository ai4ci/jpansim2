package io.github.ai4ci.util;

import java.util.ArrayList;

/**
 * Used to prevent immutables deep cloning for lists. A shallow list will always
 * be copied in situ to a new immutable. Its basically an arraylist hidden
 * so deep cloning cannot happen. Its particularly used to expose one set of 
 * test types to the test results class, without creating an army of clones.
 * @param <X> the list type
 */
public class ShallowList<X> extends ArrayList<X> {

	/**
	 * Combine this list with another, returning this list. This is a mutating operation, but allows chaining.
	 * @param other the other list to combine with this one
	 * @return this list, after combining with the other list
	 */
	public ShallowList<X> combine(ShallowList<X> other) {
		if (other != null)
			this.addAll(other);
		return this;
	}

}