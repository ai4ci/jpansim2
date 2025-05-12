package io.github.ai4ci.abm;

import io.github.ai4ci.abm.policy.ReactiveLockdown;

public class ClassNameTest {

	public static interface Test1 {
		String name();
		default String getName() {
			return this.getClass().getCanonicalName()+"."+name();
		}
	}
	
	public static enum TestImplementation implements Test1 {
		TEST
	}
	
	public static void main(String[] args) {
		System.out.println(TestImplementation.TEST.getName());
		System.out.println(ReactiveLockdown.class.getSimpleName());
		System.out.println(ReactiveLockdown.LOCKDOWN.getName());
	}

}
