package io.github.ai4ci.util;

import java.util.ArrayList;

public class Group<X> extends ArrayList<X> {

	public Group<X> combine(Group<X> other) {
		if (other != null)
			this.addAll(other);
		return this;
	}

}