package io.github.ai4ci.util;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.immutables.value.Value;
import org.junit.jupiter.api.Test;

import io.github.ai4ci.Data.Partial;
import io.github.ai4ci.abm.ImmutablePersonDemographic;
import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
import io.github.ai4ci.abm.mechanics.Abstraction.Modification;
import io.github.ai4ci.config.PartialDemographicAdjustment;

public class TestImmutableModifier {

	@Value.Immutable
	public static interface TestClass {
		Double getDouble();
		String getString();
		List<Integer> getSimpleList();
	}
	
	@Value.Immutable
	public static interface TestComplex {
		Double getDouble();
		String getString();
		TestClass getSubTest();
		List<TestClass> getComplexList();
	}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	public static interface _PartialTestClass extends TestClass, Modification<TestClass> {
		default _PartialTestClass self() {return this;}
	}
	
	@Partial @Value.Immutable @SuppressWarnings("immutables")
	public static interface _PartialTestComplex extends TestComplex, Modification<TestComplex> {
		default _PartialTestComplex self() {return this;}
	}
	
	@Value.Immutable
	public static interface TestSetting {
		Double getDouble();
		Distribution getAppUseProbability();
	}
	
	@Test 
	void testModify() {
		TestSetting tmp = ImmutableTestSetting.builder()
				.setAppUseProbability(SimpleDistribution.point(0.2))
				.setDouble(2.0).build();
		
		PartialDemographicAdjustment mod = 
				PartialDemographicAdjustment.builder()
					.setAppUseProbability(FixedValueFunction.of(2))
					.build();
		
		PersonDemographic test = ImmutablePersonDemographic.builder()
				.setAge(10)
				.build();
		
		TestSetting tmp2 = ReflectionUtils.modify(tmp, mod, test);
		System.out.println(tmp2);
		System.out.println(tmp2.getAppUseProbability().sample());
	}
	
	@Test
	void testMerge() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		TestClass source = ImmutableTestClass.builder()
				.setDouble(1D)
				.setString("hello world")
				.addSimpleList(1,2,3)
				.build();
		 
//		
		PartialTestClass mod = PartialTestClass.builder()
				.setDouble(2D)
				.addSimpleList(4,5)
				.build();
		System.out.println(ReflectionUtils.merge(source,mod ));
		
		TestComplex comp = ImmutableTestComplex.builder()
				.setDouble(1D)
				.setString("hello world")
				.setSubTest(source)
				.addComplexList(source,source)
				.build();
		
		PartialTestComplex mod2 = PartialTestComplex.builder()
				.setSubTest(mod)
				.build();
		
		System.out.println(ReflectionUtils.merge(comp,mod2 ));
	}
}
