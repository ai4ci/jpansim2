package io.github.ai4ci.util;

import java.util.ArrayList;

/**
 * Used to prevent immutables deep cloning for lists. A shallow list will always
 * be copied in situ to a new immutable.  
 * @param <X>
 */
public class ShallowList<X> extends ArrayList<X> {

	public ShallowList<X> combine(ShallowList<X> other) {
		if (other != null)
			this.addAll(other);
		return this;
	}

}