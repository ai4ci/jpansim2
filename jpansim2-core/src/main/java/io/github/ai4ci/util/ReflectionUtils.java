package io.github.ai4ci.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.immutables.datatype.Datatype;

import io.github.ai4ci.abm.PersonDemographic;
import io.github.ai4ci.config.Modification;
import io.github.ai4ci.config.Scale;
import io.github.ai4ci.config.execution.PartialDemographicAdjustment;
import io.github.ai4ci.functions.Distribution;
import io.github.ai4ci.functions.SimpleDistribution;
import io.github.ai4ci.functions.SimpleFunction;

/**
 * Many of these utilities could be switched for immutable datatype approaches
 * which would use code generation rather than reflection.
 */
public class ReflectionUtils {

	private static Object adjust(
			Object object, PersonDemographic demog, SimpleFunction ageAdjustment,
			Scale.ScaleType scale
	) {
		if (demog.getAge() == Double.NaN) { return object; }
		var factor = ageAdjustment.value(demog.getAge());
		if (object instanceof Distribution) {
			var base = ((Distribution) object).sample();
			return SimpleDistribution
				.point(Scale.ScaleType.scale(base, factor, scale));
		}
		if (object instanceof Double) {
			return Scale.ScaleType.scale((Double) object, factor, scale);
		}
		throw new RuntimeException(
				"Attempt to scale something not a number or distribution"
		);
	}

	private static String capitalize(String s, String prefix) {
		return prefix + s.substring(0, 1)
			.toUpperCase() + s.substring(1);
	}

	/**
	 * Find the datatype mirror class associated with an interface annotated
	 * with @Datatype. This is used to find the mirror class for an interface
	 * that is implemented by an immutable or modifiable class.
	 *
	 * @param <X> the interface tyoe
	 * @param clz the class of the interface (or the immutable or modifiable
	 *            implementation)
	 * @return the datatype mirror class associated with the interface
	 */
	@SuppressWarnings("unchecked")
	public static <X> Datatype<X> datatype(Class<X> clz) {
		var tmp = clz.getPackageName() + ".Datatype_" + clz.getSimpleName();
		try {
			return (Datatype<X>) Class.forName(tmp)
				.getConstructor()
				.newInstance();
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException("No datatype found: " + tmp);
		}
	}

	/**
	 * Get a getter method by field name
	 *
	 * @param <X> the type of the field
	 * @param o   any object
	 * @param s   the name of the field
	 * @return an Optional containing the value of the field if it exists, or an
	 *         empty Optional if the getter method does not exist or is not
	 *         accessible.
	 */
	@SuppressWarnings("unchecked")
	private static <X> Optional<X> get(Object o, String s) {
		try {
			return Optional.ofNullable((X) getter(s, o.getClass()).invoke(o));
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			return Optional.empty();
		}
	}

	private static Method getter(String s, Class<?> clz)
			throws NoSuchMethodException {
		return clz.getMethod(capitalize(s.replaceFirst("^(get|set)", ""), "get"));
	}

	private static <X> Class<?> iface(Class<X> clz) {
		if (!clz.getSimpleName()
			.startsWith("Immutable")) { return clz; }
		var tmp = clz.getSimpleName()
			.replaceFirst("Immutable", "");
		for (Class<?> iclz : clz.getInterfaces()) {
			if (iclz.getSimpleName()
				.endsWith(tmp)) { return iclz; }
		}
		return clz.getSuperclass();
	}

	/**
	 * Find the immutable (or modifiable) implementation version of an interface
	 *
	 * @param <X> the interface
	 * @param clz the class of the interface (or the immutable or modifiable
	 *            implementation)
	 * @return the immutable implementation of the interface
	 */
	public static <X> Class<?> immutable(Class<X> clz) {
		if (clz.getSimpleName()
			.startsWith("Immutable")
				|| clz.getSimpleName()
					.startsWith("Modifiable")) {
			return clz;
		}
		Class<?> immClz;
		var tmp = clz.getPackageName() + ".Immutable" + clz.getSimpleName();
		try {
			immClz = Class.forName(tmp);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("No immutable found: " + tmp);
		}
		return immClz;
	}

	/**
	 * Initialise a class using a constructor and parameters
	 *
	 * @param <X>    the type returned.
	 * @param clz    the class
	 * @param params the parameters
	 * @return an instance of type X
	 */
	@SuppressWarnings("unchecked")
	public static <X> X initialise(Class<X> clz, Object... params) {
		var paramTypes = Arrays.stream(params)
			.map(o -> o.getClass())
			.toArray(i -> new Class<?>[i]);
		Throwable err = null;
		for (Method m : clz.getMethods()) {
			if (clz.isAssignableFrom(m.getReturnType())
					&& Modifier.isStatic(m.getModifiers())
					&& Arrays.equals(m.getParameterTypes(), paramTypes)) {
				try {
					return (X) m.invoke(null, params);
				} catch (Exception e) {
					if (e instanceof InvocationTargetException) {
						e.getCause()
							.printStackTrace();
						err = e.getCause();
					}
					// didn;t work
				}
			}
		}
		if (err == null) {
			throw new RuntimeException(
					"Cannot initialise " + clz.getCanonicalName()
							+ ": no static factory method found"
			);
		}
		throw new RuntimeException(
				"Cannot initialise " + clz.getCanonicalName()
						+ ": factory threw an exception: "
						+ err.getLocalizedMessage(),
				err
		);
	}

	/**
	 * Initialise an object based on class name, package and parameters
	 *
	 * @param <X>          the type of the object being initialised
	 * @param className    the name of the class to initialise. This can be a
	 *                     simple name (e.g. "MarkovStateModel") or a fully
	 *                     qualified name (e.g.
	 *                     "io.github.ai4ci.config.inhost.MarkovStateModel"). If
	 *                     a simple name is provided, it will be resolved
	 *                     relative to the package of clzInPackage.
	 * @param clzInPackage a class that is in the same package as the class to be
	 *                     initialised. This is used to resolve the package for
	 *                     simple class names.
	 * @param params       optional parameters to pass to the initialisation
	 *                     method. The method will look for a static factory
	 *                     method in the target class that matches the parameter
	 *                     types and returns an instance of the target class. If
	 *                     no such method is found, or if the factory method
	 *                     throws an exception, a RuntimeException will be
	 *                     thrown.
	 * @return an instance of type X initialized by the factory method with the
	 *         provided parameters.
	 */
	public static <X> X initialise(
			String className, Class<?> clzInPackage, Object... params
	) {
		var base = clzInPackage.getPackageName();
		if (!className.startsWith(base)) { className = base + "." + className; }
		try {
			@SuppressWarnings(
				"unchecked"
			) var riskModelClass = (Class<X>) Class.forName(className);
			return ReflectionUtils.initialise(riskModelClass, params);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reflection based configuration merging due to complexities working with
	 * mapstruct for nested immutables. The problem here is that nested
	 * immutables need to be merged by constructing new immutables but mapstruct
	 * assumes they can be merged directly using javabeans. By providing this
	 * method we can merge nested immutables by recursively merging them using
	 * builders, but we can still use mapstruct to merge the top level objects
	 * using javabeans. This could be substituted by a mirror based approach with
	 * immutables datatype.
	 *
	 * <p>
	 * Merging means overwriting the base values with those from the modifier
	 *
	 * @param <X>      the type of the object being merged
	 * @param base     the base object to merge into
	 * @param modifier the modifications to apply to the base object. Any
	 *                 non-null fields will be merged into the base object. If a
	 *                 field is itself an immutable object and the modifier
	 *                 provides a modification for it, then the merge will be
	 *                 applied recursively to that field. For collections, the
	 *                 modifier's collection will be added to the base collection
	 *                 (if present) rather than replacing it.
	 * @return a new object of type X with the modifications applied.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <X> X merge(X base, Modification<? extends X> modifier) {
		try {
			Class<?> immClz = immutable(base.getClass());
			Class<?> builderClz = immClz.getMethod("builder")
				.getReturnType();
			var builder = immClz.getMethod("builder")
				.invoke(null);
			builderClz.getMethod("from", iface(base.getClass()))
				.invoke(builder, base);

			for (Method m : builderClz.getMethods()) {
				if (m.getName()
					.startsWith("set") && m.getParameterCount() == 1) {
					Optional<?> value1 = get(modifier, m.getName());
					if (value1.isPresent()) {
						if (value1.get() instanceof Modification) {
							Optional<?> baseValue = get(base, m.getName());
							m.invoke(
								builder,
								merge(baseValue.get(), (Modification) value1.get())
							);
						} else if (value1.get() instanceof Collection) {
							Collection<Object> tmp = new ArrayList<>();
							get(base, m.getName()).map(o -> (Collection<Object>) o)
								.ifPresent(tmp::addAll);

							// TODO: Is adding to collections is a good idea e.g. if we
							// are overriding tests

							tmp.addAll((Collection<Object>) value1.get());
							m.invoke(builder, tmp);
						} else {
							m.invoke(builder, value1.get());
						}
					}
				}
			}

			var builderBuild = builder.getClass()
				.getMethod("build");
			var out = builderBuild.invoke(builder);
			return (X) out;
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Apply demographic adjustments to a configuration object. This is similar
	 * to merge but instead of replacing values with the modifier's values, it
	 * applies a scaling factor to the base value based on the modifier and the
	 * person's demographic. The modifier provides {@link SimpleFunction}s that
	 * take the person's age and return a scaling factor. The base value is then
	 * scaled by this factor according to the specified {@link Scale.ScaleType}
	 * (e.g. multiplicative, additive). This allows us to apply age-based
	 * adjustments to parameters like incubation period or case fatality rate
	 * based on the person's age.
	 *
	 * <p>
	 * Modifying means scaling the base with the modifier either as an odds ratio
	 * or as a multiplicative factor (depending on the ScaleType annotation)
	 *
	 * @param <X>       the type of the object being modified
	 * @param base      the base object to modify
	 * @param modifiers the demographic adjustments to apply. This should be an
	 *                  object that provides SimpleFunctions for each parameter
	 *                  that needs to be adjusted, annotated with @Scale to
	 *                  specify how the scaling should be applied.
	 * @param demog     the person's demographic information, which is used to
	 *                  compute the scaling
	 * @return a new object of type X with the demographic adjustments applied.
	 */
	@SuppressWarnings("unchecked")
	public static <X> X modify(
			X base, PartialDemographicAdjustment modifiers, PersonDemographic demog
	) {
		try {
			Class<?> immClz = immutable(base.getClass());
			Class<?> builderClz = immClz.getMethod("builder")
				.getReturnType();
			var builder = immClz.getMethod("builder")
				.invoke(null);
			builderClz.getMethod("from", iface(base.getClass()))
				.invoke(builder, base);

			for (Method m : modifiers.getClass()
				.getMethods()) {
				if (SimpleFunction.class.isAssignableFrom(m.getReturnType())) {

					Optional<?> value = get(base, m.getName());
					if (value.isPresent()) {
						var ageAdjustment = (SimpleFunction) m
							.invoke(modifiers);
						if (ageAdjustment != null) {
							var scale = m.getAnnotation(Scale.class)
								.value();
							var adjusted = adjust(
								value.get(),
								demog,
								ageAdjustment,
								scale
							);
							set(builder, m.getName(), adjusted);
						}
					}
				}
			}
			return (X) builder.getClass()
				.getMethod("build")
				.invoke(builder);
		} catch (NoSuchMethodException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Create a proxy for some interface that returns null or default value for
	 * any invocation.
	 *
	 * This is useful for testing and for creating stubs when we don't care about
	 * the actual implementation. For example, we can create a null proxy for a
	 * configuration interface when we want to test code that uses the
	 * configuration but we don't want to provide a real implementation of the
	 * configuration. The null proxy will return null for any method that returns
	 * an object, and will return default values for primitive return types (e.g.
	 * false for boolean, 0 for int, etc.). This allows us to focus on testing
	 * the code that uses the configuration without worrying about the details of
	 * the configuration itself.
	 *
	 * @param <X> the type of the interface to proxy
	 * @param clz the class of the interface to proxy
	 * @return a proxy instance of the interface that returns null or default
	 *         values for any method
	 *
	 */
	@SuppressWarnings("unchecked")
	public static <X> X nullProxy(Class<X> clz) {
		return (X) Proxy.newProxyInstance(
			clz.getClassLoader(),
			new Class<?>[] { clz },
			(InvocationHandler) (proxy, method, args) -> {
				if (method.getReturnType()
					.isPrimitive()) {
					if (method.getReturnType()
						.equals(Boolean.TYPE)) { return false; }
					if (method.getReturnType()
						.equals(Integer.TYPE)) { return 0; }
					if (method.getReturnType()
						.equals(Long.TYPE)) { return 0L; }
					if (method.getReturnType()
						.equals(Double.TYPE)) {
						return Double.NaN;
					}
					if (method.getReturnType()
						.equals(Float.TYPE)) { return Float.NaN; }
				}
				return null;
			}
		);
	}

	/**
	 * Set a value by field name
	 *
	 * @param <X>   the type of the field
	 * @param o     any object
	 * @param s     the name of the field
	 * @param value the value to set
	 */
	private static void set(Object o, String s, Object value)
			throws NoSuchMethodException {
		try {
			setter(s, o.getClass(), value.getClass()).invoke(o, value);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private static Method setter(String s, Class<?> clz, Class<?> type)
			throws NoSuchMethodException {
		var sn = capitalize(s.replaceFirst("^(get|set)", ""), "set");
		for (Method m : clz.getMethods()) {
			if (m.getName()
				.equals(sn) && m.getParameterCount() == 1
					&& m.getParameters()[0].getType()
						.isAssignableFrom(type)) {
				return m;
			}
		}
		throw new NoSuchMethodException(
				"No match for " + clz.getCanonicalName() + "("
						+ type.getCanonicalName() + ")"
		);
	}

	@SuppressWarnings({ "unused", "unchecked" })
	private void copy(Object from, Object to, String s)
			throws NoSuchMethodException {
		Optional<?> value1 = get(from, s);
		if (value1.isEmpty()) { return; }
		if (value1.get() instanceof Collection) {
			Collection<Object> tmp = new ArrayList<>(
					(Collection<Object>) value1.get()
			);
			get(to, s).map(o -> (Collection<Object>) o)
				.ifPresent(tmp::addAll);
			set(to, s, tmp);
			return;
		}
		set(to, s, value1);
	}

}
