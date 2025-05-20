package io.github.ai4ci.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.abm.mechanics.Abstraction.Distribution;
import io.github.ai4ci.abm.mechanics.Abstraction.Modification;
import io.github.ai4ci.abm.mechanics.Abstraction.SimpleFunction;
import io.github.ai4ci.config.DemographicAdjustment.Scale;
import io.github.ai4ci.config.DemographicAdjustment.ScaleType;
import io.github.ai4ci.config.PartialDemographicAdjustment;

public class ReflectionUtils {

	private static <X> Class<?> immutable(Class<X> clz) {
		if (clz.getSimpleName().startsWith("Immutable")) return clz;
		if (clz.getSimpleName().startsWith("Modifiable")) return clz;
		Class<?> immClz;
		String tmp = clz.getPackageName()+".Immutable"+clz.getSimpleName();
		 try {
			immClz = Class.forName(tmp);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("No immutable found: "+tmp);
		}
		return immClz;
	}
	
	private static <X> Class<?> iface(Class<X> clz) {
		if (clz.getSimpleName().startsWith("Immutable")) {
			String tmp = clz.getSimpleName().replaceFirst("Immutable", "");
			for (Class<?> iclz: clz.getInterfaces())
				if (iclz.getSimpleName().endsWith(tmp)) return iclz;
			return clz.getSuperclass();
		} else {
			return clz;
		}
	}
	
	/** 
	 * Reflection based configuration merging due to complexities working
	 * with mapstruct for nested immutables. 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> X merge(X base, Modification<? extends X> modifier) {
		try {
			Class<?> immClz = immutable(base.getClass());
			Class<?> builderClz = immClz.getMethod("builder").getReturnType();
			Object builder = immClz.getMethod("builder").invoke(null);
			builderClz.getMethod("from", iface(base.getClass())).invoke(builder, base);
			
			for (Method m: builderClz.getMethods()) {
				if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
					Optional<?> value1 = get(modifier, m.getName());
					if (value1.isPresent()) {
						if (value1.get() instanceof Modification) {
							Optional<?> baseValue = get(base, m.getName());
							m.invoke(builder, merge( baseValue.get(), (Modification) value1.get()));
						} else if (
							value1.get() instanceof Collection
						) {
							Collection<Object> tmp = new ArrayList<>();
							get(base,m.getName()).map(o -> (Collection<Object>) o).ifPresent(tmp::addAll);
							
							//TODO: Is adding to collections is a good idea e.g. if we are overriding tests
							
							tmp.addAll((Collection<Object>) value1.get());
							m.invoke(builder, tmp);
						}
						else {
							m.invoke(builder, value1.get());
						}
					}
				}
			}
			
			Method builderBuild = builder.getClass().getMethod("build");
			Object out = builderBuild.invoke(builder);
			return (X) out;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static <X> X modify(X base, PartialDemographicAdjustment modifiers, PersonDemographic demog) {
		try {
			Class<?> immClz = immutable(base.getClass());
			Class<?> builderClz = immClz.getMethod("builder").getReturnType();
			Object builder = immClz.getMethod("builder").invoke(null);
			builderClz.getMethod("from", iface(base.getClass())).invoke(builder, base);
			
			for (Method m: modifiers.getClass().getMethods()) {
				if (SimpleFunction.class.isAssignableFrom(m.getReturnType())) {
				
					Optional<?> value = get(base, m.getName());
					if (value.isPresent()) {
						SimpleFunction ageAdjustment = (SimpleFunction) m.invoke(modifiers);
						if (ageAdjustment != null) {
							ScaleType scale = m.getAnnotation(Scale.class).value();
							Object adjusted = adjust(value.get(), demog, ageAdjustment, scale);
							set(builder, m.getName(), adjusted);
						}
					}
				}
			}
			return (X) builder.getClass().getMethod("build").invoke(builder);
		} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Object adjust(Object object, PersonDemographic demog, SimpleFunction ageAdjustment, ScaleType scale) {
		if (demog.getAge() == Double.NaN) return object; 
		double factor = ageAdjustment.value(demog.getAge());
		if (object instanceof Distribution) {
			double base = ((Distribution) object).sample();
			return SimpleDistribution.point(ScaleType.scale(base,factor, scale));
		} else if (object instanceof Double) {
			return ScaleType.scale((Double) object, factor, scale);
		}
		throw new RuntimeException("Attempt to scale something not a number or distribution");
	};
	
	
	
	@SuppressWarnings({ "unused", "unchecked" })
	private void copy(Object from, Object to, String s) throws NoSuchMethodException {
		Optional<?> value1 = get(from, s);
		if (value1.isEmpty()) return;
		if (
				value1.get() instanceof Collection
		) {
			Collection<Object> tmp = new ArrayList<>();
			tmp.addAll((Collection<Object>) value1.get());
			get(to,s).map(o -> (Collection<Object>) o).ifPresent(tmp::addAll);
			set(to, s, (Iterable<?>) tmp);
			return;
		}
		set(to, s, value1);
	}
	
	@SuppressWarnings("unchecked")
	private static <X> Optional<X> get(Object o, String s) {
		try {
			return Optional.ofNullable((X) getter(s,o.getClass()).invoke(o));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}
	
	private static void set(Object o, String s, Object value) throws NoSuchMethodException {
		try {
			setter(s, o.getClass(), value.getClass()).invoke(o, value);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Method getter(String s, Class<?> clz) throws NoSuchMethodException {
		return 
			clz.getMethod(
				capitalize(s.replaceFirst("^(get|set)", ""), "get")
			);
	}
	
	private static Method setter(String s, Class<?> clz, Class<?> type) throws NoSuchMethodException {
		String sn = capitalize(s.replaceFirst("^(get|set)", ""), "set");
		for (Method m : clz.getMethods()) {
			if (m.getName().equals(sn) && m.getParameterCount() ==1 
					&& m.getParameters()[0].getType().isAssignableFrom(type)
				) {
				return m;
			}
		}
		throw new NoSuchMethodException("No match for "+clz.getCanonicalName()+"("+type.getCanonicalName()+")");
	}
	
	private static String capitalize(String s, String prefix) {
        return prefix + s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
