package io.github.ai4ci.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.lang3.SerializationUtils;
import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.util.Factor;


public class FunctionalKeyExperiment {

	
	
	public static class KeyFunction<X,Y> {
		SerializableFunction<X,Y> fn;
		byte[] ser;
		int hash;
		
		public static interface SerializableFunction<X,Y> extends Serializable, Function<X,Y> {} 
		
		public KeyFunction(SerializableFunction<X, Y> lambda) {
			fn = lambda;
			ser = SerializationUtils.serialize(lambda);
			hash = Arrays.hashCode(ser);
		}
		
		public Y apply(X x) {
			return fn.apply(x);
		};
		public int hashCode() {
			return hash;
		}
		public boolean equals(Object o) {
			if (this.hashCode() != o.hashCode()) return false;
			if (!(o instanceof KeyFunction)) return false;
			return Arrays.equals(this.ser, ((KeyFunction<?,?>) o).ser);
		}
		
		public static <X,Y> KeyFunction<X,Y> create(SerializableFunction<X,Y> lambda) {
			return new KeyFunction<X,Y>(lambda);
		}
	}
	
	@Value.Immutable
	public static interface TestImmutable {
		Integer getInt();
		String getString();
		
		@Value.Default default KeyFunction<TestImmutable,Integer> kf() {
			return KeyFunction.create(TestImmutable::getInt);
		}
	}
	
	
	
	
	
	public static enum ExampleEnum implements Factor {
		@Level("test_one") ONE,
		@Level("test_two") TWO,
		THREE
	}
	
	@Test
	void testLabelled() {
		
		assertEquals(ExampleEnum.ONE.getLabel(), "test_one");
		assertEquals(ExampleEnum.TWO.getLabel(), "test_two");
		assertEquals(ExampleEnum.THREE.getLabel(), "THREE");
		
		assertEquals(Factor.fromLabel(ExampleEnum.class, "test_one"), ExampleEnum.ONE);
		assertEquals(Factor.fromLabel(ExampleEnum.class, "THREE"), ExampleEnum.THREE);
	}
	
	@Test
	void testExperiment() {
		
		var instance = ImmutableTestImmutable.builder().setInt(1).setString("hello").build();
		var instance2 = ImmutableTestImmutable.builder().setInt(1).setString("hello").build();
		
		var k1 = // KeyFunction.create(TestImmutable::getInt);
				instance2.kf();
		var k2 = instance.kf();
		var k3 = KeyFunction.create(TestImmutable::getString);
		
//		System.out.println(k1.fn.getClass().hashCode());
//		System.out.println(k2.fn.getClass().hashCode());
//		System.out.println(k3.fn.getClass().hashCode());
		
		System.out.println(k1.hashCode());
		System.out.println(k2.hashCode());
		System.out.println(k3.hashCode());
		assertEquals(k1, k2);
		assertNotEquals(k1, k3);
	}
	
}
