//package io.github.ai4ci.example;
//
//import java.lang.reflect.Method;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import io.github.ai4ci.config.ExperimentConfiguration;
//import io.github.ai4ci.util.ReflectionUtils;
//
//public class ImmutablesJacksonInspector {
//
//	public static void main(String[] args) {
//
//		Class<?> root = ExperimentConfiguration.class;
//		var insp = new ImmutablesJacksonInspector();
//
//	}
//
//	Map<Class<?>, List<Method>> repo;
//
//	public ImmutablesJacksonInspector() {
//		this.repo = new HashMap<>();
//
//	}
//
//	private void traverse(Class<?> cls) {
//
//		var immCls = ReflectionUtils.immutable(cls);
//
//		immCls.getAnnotation(null)
//
//	}
//}
