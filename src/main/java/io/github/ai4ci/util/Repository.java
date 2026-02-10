package io.github.ai4ci.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.SerializationUtils;
import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import io.github.ai4ci.Import;

/**
 * A reflection based data indexed repository class with support for foreign
 * keys for data imported from multiple CSV files.
 * 
 * <p>
 * The repository is designed to be populated from CSV files specified by
 * classes annotated with @Import, where each class corresponds to a CSV file
 * and its fields correspond to columns in the CSV. The repository supports
 * foreign key references between classes, allowing for efficient lookups based
 * on these references. The foreign key references are specified by the return
 * types of the builder setters in the immutable classes, and the repository
 * resolves these references when loading the data from the CSV
 * files. @Import.Id annotations are used to specify unique keys for each class,
 * which are used for lookups and reference resolution.
 * 
 * <p>
 * The repository uses reflection to read the CSV files and populate the data
 * store, and it builds indexes for efficient lookups based on specified mapping
 * functions. The indexes are lazily populated on first query and cached for
 * subsequent queries.
 * 
 * <p>
 * Only many:1 foreign key relations supported (no collections) in CSV.
 * Relations are lazily resolved on first query and then cached for subsequent
 * queries. This is done by building an index the first time a lookup is
 * performed for a given mapping function, and then using the index for
 * subsequent lookups with the same function. The mapping functions must be
 * expressed in the form `X::getY` for them to work properly as keys for
 * indexing. If they are not, then additional indexes may get created due to
 * differences in serialization of the lambda expressions used as keys.
 * 
 * <p>
 * Integrates with immutables. The CSV reader uses the builder pattern of
 * immutables to construct objects from the CSV data, and it uses reflection to
 * determine the expected types of each column based on the builder setters of
 * the immutable classes. This allows for seamless integration with immutable
 * data classes and supports complex object construction from CSV data,
 * including resolving foreign key references to other immutable classes in the
 * repository.
 * 
 * @author Rob Challen
 */
public class Repository {

	/**
	 * Create a new repository with a base directory for CSV files. The repository
	 * is populated by calling {@link #loadAll(Path, Class...)} with a set of @Import 
	 * annotated classes.
	 * 
	 * @param baseDirectory the base directory for CSV files
	 */
	public Repository(Path baseDirectory) {
		super();
		this.baseDirectory = baseDirectory;
	}

	Map<Class<?>, Map<String, Object>> dataStore = new HashMap<>();
	Path baseDirectory;
	RepositoryIndex index = new RepositoryIndex(this);

	/**
	 * Check if the repository has any data for a given type.
	 * @param cls the type to check
	 * @return true if the repository has data for this type, false otherwise
	 */
	public boolean hasType(Class<?> cls) {
		return dataStore.containsKey(cls);
	}

	/**
	 * Setup a new type
	 * @param cls the type
	 * @return the repository
	 */
	private Repository withType(Class<?> cls) {
		dataStore.computeIfAbsent(cls, c -> new HashMap<>());
		return this;
	}

	/**
	 * Fetch an item by unique id
	 * @param <X> the type of the object
	 * @param cls the type of the object
	 * @param key a unique id for this object
	 * @return the value for the object of this class with this unique key
	 */
	public <X> X getValue(Class<X> cls, String key) {
		this.withType(cls);
		Object out = dataStore.get(cls).get(key);
		return cls.cast(out);
	}

	private <X> void setValue(Class<X> cls, String key, X out) {
		this.withType(cls);
		var tmp = this.dataStore.get(cls).putIfAbsent(key, out);
		if (tmp != null) throw new RuntimeException("Duplicate unique key: "+key+" for class: "+cls.getName());
	}

	/**
	 * Stream all values of a given type.
	 * @param <X> the type of the objects
	 * @param cls the type of the objects
	 * @return a stream of all objects of this type in the repository
	 */
	public <X> Stream<X> streamValues(Class<X> cls) {
		this.withType(cls);
		Collection<Object> out = dataStore.get(cls).values();
		return out.stream().map(o -> cls.cast(o));
	}

	private <X> ValueIndex<X> setupIndex(Class<X> cls) {
		return this.index.getOrCreate(cls);
	}

	/**
	 * Find values of type X that link to a specific lookup value or type Y, 
	 * based on a mapping function that maps X to Y. This is a foreign key 
	 * lookup equivalent and returns objects that link to a key. This will 
	 * generate an index the first time it is called and thereafter use the index.
	 * @param <X> the return type
	 * @param <Y> the lookup type (foreign key)
	 * @param lookup the value to lookup
	 * @param xCls The type to return
	 * @param fn must be expressed in the form `X::getY` for it to work properly 
	 *   as a key. If it is not then additional indexes will get created. 
	 * @return a streeam of values whose `fn` methods return the lookup value 
	 */
	public <X,Y> Stream<X> findValues(Y lookup, Class<X> xCls, SerializableFunction<X,Y> fn) {
		return this.index.lookup(lookup, xCls, fn).stream();
	}

	/**
	 * Find a single value of type X that links to a specific lookup value or type Y, 
	 * based on a mapping function that maps X to Y. This is a foreign key 
	 * lookup equivalent and returns the single object that links to a key. This will 
	 * generate an index the first time it is called and thereafter use the index.
	 * @param <X> the return type
	 * @param <Y> the lookup type (foreign key)
	 * @param lookup the value to lookup
	 * @param xCls The type to return
	 * @param fn must be expressed in the form `X::getY` for it to work properly 
	 *   as a key. If it is not then additional indexes will get created. 
	 * @return the single value whose `fn` method returns the lookup value 
	 */
	public <X,Y> X findOne(Y lookup, Class<X> xCls, SerializableFunction<X,Y> fn) {
		var tmp =  this.index.lookup(lookup, xCls, fn);
		if (tmp.size() == 0) throw new IllegalArgumentException("Missing result.");
		if (tmp.size() > 1) throw new IllegalArgumentException("Non unique result.");
		return tmp.iterator().next();
	}

	private <X> Repository readCSV(Class<X> cls) throws IOException {

		try {
			Method getId = Stream.of(cls.getMethods()).filter(m -> m.isAnnotationPresent(Import.Id.class)).findFirst()
					.orElseThrow(() -> new IOException("Class: "+cls.getName()+" did not have and @Import.Id annotation."));
			String filename = cls.getAnnotation(Import.class).value();
			// Read a CSV into an immutable using a builder and resolving references
			Class<?> immClz = ReflectionUtils.immutable(cls);
			Class<?> builderClz = immClz.getMethod("builder").getReturnType();
			Object builder = immClz.getMethod("builder").invoke(null);

			try (CSVReader<X> r = new CSVReader<X>(filename, cls, baseDirectory)) {

				while (r.hasNext()) {
					// QWEN: I need the following to be read from a CSV file
					Map<String,Object> record = r.next();

					for (Method m: builderClz.getMethods()) {
						if (
								m.getParameterCount() == 1 &&
								//m.getName().equals("setIndex") &
								m.getParameterTypes()[0].equals(ValueIndex.class)
								) {
							m.invoke(builder, this.setupIndex(cls));
						} else if (m.isAnnotationPresent(JsonProperty.class)) {

							String csvCol = m.getAnnotation(JsonProperty.class).value();
							boolean byRef = m.getParameterTypes()[0].isAnnotationPresent(Import.class);
							Object value;
							if (byRef) {
								String key = record.get(csvCol).toString();
								value = this.getValue(m.getParameterTypes()[0], key);
							} else {
								value = record.get(csvCol);
							}

							// Set the builder
							m.invoke(builder, value);

						}
					}

					Method builderBuild = builder.getClass().getMethod("build");
					X out = cls.cast(builderBuild.invoke(builder));
					String key = getId.invoke(out).toString();

					this.setValue(cls, key, out);
				}
			}

			return this;
		} catch (
				NoSuchMethodException| SecurityException| IllegalAccessException| IllegalArgumentException| InvocationTargetException e
				) {
			throw new IOException("CSV repository exception", e);
		}

	}

	/** 
	 * Construct a repository class from a set of @Import annotated classes.
	 * @param baseDir the directory of the CSV files 
	 * @param clzs a set of @Import annotated classes with 
	 * @return a repository containing the data from the CSV files for the given classes, with references resolved
	 * @throws IOException
	 */
	public static Repository loadAll(Path baseDir, Class<?>... clzs) throws IOException {
		Repository repo = new Repository(baseDir);

		List<Class<?>> validClasses = Arrays.stream(clzs)
				.filter(cls -> cls.isAnnotationPresent(Import.class))
				.collect(Collectors.toList());

		if (validClasses.isEmpty()) {
			throw new IllegalArgumentException("No @Import-annotated classes provided.");
		}

		// Ensure no duplicates
		Set<Class<?>> classSet = new HashSet<>(validClasses);
		if (classSet.size() != validClasses.size()) {
			throw new IllegalArgumentException("Duplicate classes detected.");
		}

		// Graph: dependency → dependents (edges go from prerequisite to dependent)
		Map<Class<?>, Set<Class<?>>> graph = new HashMap<>();
		Map<Class<?>, Integer> inDegree = new HashMap<>();

		// Initialize
		for (Class<?> cls : validClasses) {
			graph.put(cls, new HashSet<>());
			inDegree.put(cls, 0);
		}

		// Build graph: if B.getXXX() returns A, then A → B (A must be loaded before B)
		for (Class<?> cls : validClasses) {
			for (Method method : cls.getMethods()) {
				Class<?> returnType = method.getReturnType();
				if (classSet.contains(returnType)) {
					if (!graph.get(returnType).contains(cls)) inDegree.merge(cls, 1, Integer::sum);
					graph.get(returnType).add(cls);
				}
			}
		}

		// Kahn's algorithm
		Queue<Class<?>> queue = new LinkedList<>();
		for (Class<?> cls : validClasses) {
			if (inDegree.get(cls) == 0) {
				queue.offer(cls);
			}
		}

		List<Class<?>> ordered = new ArrayList<>();
		while (!queue.isEmpty()) {
			Class<?> cls = queue.poll();
			ordered.add(cls);

			// Traverse dependents
			for (Class<?> dependent : graph.get(cls)) {
				inDegree.merge(dependent, -1, Integer::sum);
				if (inDegree.get(dependent) == 0) {
					queue.offer(dependent);
				}
			}
		}

		// Cycle detection
		if (ordered.size() != validClasses.size()) {
			throw new IllegalArgumentException(
					"Cyclic dependency detected in @Import classes: " + validClasses);
		}

		// Now load in order
		for (Class<?> cls : ordered) {
			repo.readCSV(cls);
		}

		return repo;
	}

	/**
	 * String representation of the repository showing the number of items for each type and the indexes.
	 * @return a string representation of the repository
	 */
	public String toString() {
		return (
				"Data repository:\n"+
				this.dataStore.entrySet().stream().map(kv -> "- "+kv.getKey().getSimpleName()+": "+kv.getValue().size()+" items.").collect(Collectors.joining("\n"))+
				"\nIndexes: \n"+
				this.index.indices.entrySet().stream()
					.flatMap(kv -> {
						return kv.getValue().cache.entrySet().stream().map(kv2 -> {
							return "- "+kv.getKey().getSimpleName()+"->"+
							kv2.getValue().output.getSimpleName().replace("Immutable", "");
						});
					})
					.collect(Collectors.joining("\n"))
				);

	}

	/**
	 * A CSV reader that reads a CSV file into a map of column name to value,
	 * converting to the expected type based on the builder setters of the class. It
	 * uses Jackson CSV for parsing and supports type conversion for basic types and
	 * enums. It also supports foreign key references by looking up values in the
	 * repository. The reader is designed to be used in a try-with-resources block
	 * to ensure proper resource management.
	 *
	 * @param <X> the type of the objects being read (the entity type)
	 */
	public static class CSVReader<X> implements Iterator<Map<String, Object>>, AutoCloseable {

		private final MappingIterator<Map<String, String>> iterator;
		private final Map<String, Class<?>> columnTypes;
		private final Map<String, Boolean> isReferenceField;

		/**
		 * Create a new CSV reader for a given class and base directory. The reader
		 * will read the CSV file specified by the @Import annotation on the class,
		 * and will use reflection to determine the expected types of each column based
		 * on the builder setters of the class. It will also determine which columns
		 * are foreign key references based on the presence of @Import annotations on
		 * the setter parameter types.
		 *
		 * @param filename the name of the CSV file to read (relative to baseDirectory)
		 * @param cls the class representing the type of objects being read (must be annotated with @Import)
		 * @param baseDirectory the base directory for CSV files
		 * @throws IOException if there is an error reading the CSV file or initializing the reader
		 */
		public CSVReader(String filename, Class<X> cls, Path baseDirectory) throws IOException {
			Path filePath = baseDirectory.resolve(filename);
			if (!Files.exists(filePath)) {
				throw new IllegalArgumentException("CSV file not found: " + filePath);
			}

			// Initialize Jackson CSV mapper
			CsvMapper csvMapper = new CsvMapper();
			CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnReordering(true);

			// ObjectMapper mapper = new ObjectMapper(); // For type handling

			// Build schema and type map from the class setters
			this.columnTypes = new HashMap<>();
			this.isReferenceField = new HashMap<>();

			// Inspect builder setters via reflection (matching your Repository logic)
			try {
				Class<?> immClz = ReflectionUtils.immutable(cls);
				Class<?> builderClz = immClz.getMethod("builder").getReturnType();

				// Temporarily create an empty instance to infer types via setter parameters
				// We use reflection to simulate what the builder would accept
				for (Method method : builderClz.getMethods()) {
					if (method.isAnnotationPresent(JsonProperty.class)) {
						JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
						String csvCol = jsonProperty.value();

						Class<?> paramType = method.getParameterTypes()[0];
						boolean isRef = paramType.isAnnotationPresent(Import.class);

						isReferenceField.put(csvCol, isRef);

						// If it's a reference, we expect Integer (for key)
						if (isRef) {
							columnTypes.put(csvCol, Integer.class);
						} else {
							// Otherwise use actual type
							columnTypes.put(csvCol, paramType);
						}
					}
				}

				// Open CSV and start reading
				this.iterator = csvMapper.readerFor(Map.class)
						.with(schema)
						.readValues(Files.newBufferedReader(filePath));

			} catch (Exception e) {
				throw new IOException("Failed to initialize CSV reader for " + cls.getSimpleName(), e);
			}
		}

		@Override
		public boolean hasNext() {
			try {
				return iterator.hasNext();
			} catch (UncheckedIOException e) {
				throw new RuntimeException("IO error during CSV read", e.getCause());
			}
		}

		@Override
		public Map<String, Object> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			Map<String, String> stringRow;
			try {
				stringRow = iterator.next();
			} catch (UncheckedIOException e) {
				throw new RuntimeException("IO error while reading CSV row", e.getCause());
			}

			Map<String, Object> typedRow = new HashMap<>();
			for (Map.Entry<String, String> entry : stringRow.entrySet()) {
				String colName = entry.getKey();
				String value = entry.getValue();

				if (value == null || value.trim().isEmpty() || value.equals("NA")) {
					typedRow.put(colName, null);
					continue;
				}

				Class<?> targetType = columnTypes.get(colName);
				if (targetType == null) {throw new RuntimeException("No matching get method for column: "+colName); }
				Boolean ref = isReferenceField.get(colName);
				if (Boolean.TRUE.equals(ref)) {
					// Foreign key: always String
					typedRow.put(colName, value.toString());
				} else {
					// Convert to expected type
					Object converted = convertValue(value.trim(), targetType);
					typedRow.put(colName, converted);
				}
			}

			return typedRow;
		}

		// Basic type conversion (extendable)
		@SuppressWarnings({"unchecked"})
		private <T extends Enum<T>> Object convertValue(String value, Class<?> type) {
			if (value == null || value.isEmpty() || value.equals("NA")) return null;
			if (Factor.class.isAssignableFrom(type)) {
				return Factor.fromLabel((Class<T>) type, value);
			} else if (type.isEnum()) {
				try {
					return Enum.valueOf((Class<T>) type, value);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("Invalid value for enum " + type.getSimpleName() + ": '" + value + "'. ", e);
				}
			} else if (type == String.class) {
				return value;
			} else if (type == Integer.class || type == int.class) {
				return Integer.parseInt(value);
			} else if (type == Long.class || type == long.class) {
				return Long.parseLong(value);
			} else if (type == Double.class || type == double.class) {
				return Double.parseDouble(value);
			} else if (type == Float.class || type == float.class) {
				return Float.parseFloat(value);
			} else if (type == Boolean.class || type == boolean.class) {
				return Boolean.parseBoolean(value.toLowerCase());
			} else if (type == Short.class || type == short.class) {
				return Short.parseShort(value);
			} else if (type == Byte.class || type == byte.class) {
				return Byte.parseByte(value);
			} else {
				// Default to string for unknown types
				return value;
			}
		}

		@Override
		public void close() throws IOException {
			iterator.close();
		}
	}

	/**
	 * A serializable function interface that extends both Serializable and Function.
	 * This is used to allow lambda expressions to be serialized and used as keys for indexing.
	 *
	 * @param <X> the input type of the function (usually the entity type)
	 * @param <Y> the return type of the function (the indexed key type)
	 */
	public static interface SerializableFunction<X,Y> extends Serializable, Function<X,Y> {} 

	/**
	 * A key generated from the serialization of a lambda function.
	 *
	 * <p>This class serialises a {@link SerializableFunction} and uses the
	 * resulting byte sequence as the basis for equality and hashCode. It is
	 * used internally to memoise indexes built from lambda expressions.
	 * 
	 * <p>Note that the lambda must be expressed in the form `X::getY` for it to 
	 * work properly as a key. If it is not then additional indexes will get 
	 * created, as the serialization of the lambda will differ.
	 *
	 * @param <X> the input type of the function (usually the entity type)
	 * @param <Y> the return type of the function (the indexed key type)
	 */
	public static class FunctionKey<X,Y> {
		byte[] ser;
		int hash;


		/**
		 * Create a new FunctionKey by serializing the given lambda function.
		 *
		 * @param lambda the lambda function to serialize and use as a key
		 */
		public FunctionKey(SerializableFunction<X, Y> lambda) {
			ser = SerializationUtils.serialize(lambda);
			hash = Arrays.hashCode(ser);
		}

		@Override
		public int hashCode() {
			return hash;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this.hashCode() != o.hashCode()) return false;
			if (!(o instanceof FunctionKey)) return false;
			return Arrays.equals(this.ser, ((FunctionKey<?,?>) o).ser);
		}

		/**
		 * Factory method to create a FunctionKey from a SerializableFunction.
		 *
		 * @param <X> the input type of the function (usually the entity type)
		 * @param <Y> the return type of the function (the indexed key type)
		 * @param lambda the lambda function to serialize and use as a key as
		 *  an method reference (e.g. `X::getY`). If the lambda is not a method 
		 *  reference, then additional indexes may be created due to differences 
		 *  in serialization.
		 * @return a new FunctionKey instance representing the serialized lambda
		 */
		public static <X,Y> FunctionKey<X,Y> create(SerializableFunction<X,Y> lambda) {
			return new FunctionKey<X,Y>(lambda);
		}
	}

	private static Function<SerializableFunction<?,?>, FunctionKey<?,?>> getKey = 
			Memoised.memoise(FunctionKey::create);

	/**
	 * Index for a given type.
	 *
	 * <p>Holds cached inversion indexes for a particular key type. The generic
	 * parameter {@code Y} is the type of the indexed key (for example an id
	 * or simple lookup value) returned by the functions used to build the
	 * index.
	 *
	 * @param <Y> the indexed key type
	 */
	public static class ValueIndex<Y> {

		Repository repo;
		Map<FunctionKey<?,Y>, Inversion<Y,?>> cache = new HashMap<>();
		Class<Y> indexCls;

		/**
		 * Create a new ValueIndex for a given key type and repository.
		 *
		 * @param indexType the class of the indexed key type (for example Integer.class for an id)
		 * @param repo the repository to use for looking up values when building the index
		 */
		public ValueIndex(Class<Y> indexType, Repository repo) {
			this.indexCls = indexType;
			this.repo = repo;
		}

		/**
		 * Get the class of the indexed key type for this index.
		 *
		 * @return the class of the indexed key type (for example Integer.class for an id)
		 */
		public Class<Y> getType() {
			return indexCls;
		}

		/**
		 * Lookup values of type X that link to a specific indexed key value of type Y, 
		 * based on a mapping function that maps X to Y. This is a foreign key lookup 
		 * equivalent and returns objects that link to a key. This will generate an index 
		 * the first time it is called and thereafter use the index.
		 *
		 * @param <X> the return type (the entity type being looked up)
		 * @param indexed the value of the indexed key to lookup (for example an id)
		 * @param type the class of the return type X
		 * @param fn must be expressed in the form `X::getY` for it to work properly 
		 *   as a key. If it is not then additional indexes will get created. 
		 * @return a set of values of type X whose `fn` methods return the indexed value
		 */
		public <X> Set<X> lookup(Y indexed, Class<X> type, SerializableFunction<X, Y> fn) {
			@SuppressWarnings("unchecked")
			FunctionKey<X,Y> fk = (FunctionKey<X, Y>) getKey.apply(fn);
			return cache
					// Lazily populate index when first queried
					.computeIfAbsent(fk, v -> Inversion.cache(fn, repo.streamValues(type)))
					.apply(indexed, type);
		}

		/**
		 * Cache an index for a given mapping function. This allows pre-populating the index for a function, 
		 * which can be useful if you know you will be querying it multiple times and want to avoid the overhead of lazy population on first query.
		 *
		 * @param <X> the return type (the entity type being indexed)
		 * @param xCls the class of the return type X
		 * @param fn must be expressed in the form `X::getY` for it to work properly 
		 *   as a key. If it is not then additional indexes will get created. 
		 */
		public <X> void cache(Class<X> xCls, SerializableFunction<X,Y> fn) {
			@SuppressWarnings("unchecked")
			FunctionKey<X,Y> fk = (FunctionKey<X, Y>) getKey.apply(fn);
			cache
			// Lazily populate index on construction
			.computeIfAbsent(fk, v -> Inversion.cache(fn, repo.streamValues(xCls)));
		}
	}

	/**
	 * Repository index that holds ValueIndex instances for different key types.
	 * This allows the repository to maintain multiple indexes for different lookup
	 * functions and key types.
	 */
	public static class RepositoryIndex {

		Repository repo;
		Map<Class<?>, ValueIndex<?>> indices = new HashMap<>();

		/**
		 * Create a new RepositoryIndex for a given repository.
		 *
		 * @param repo the repository to use for looking up values when building indexes
		 */
		public RepositoryIndex(Repository repo) {
			this.repo = repo;
		}

		/**
		 * Get an existing ValueIndex for a given key type, or create a new one if it does not exist.
		 *
		 * @param <X> the indexed key type
		 * @param cls the class of the indexed key type (for example Integer.class for an id)
		 * @return the existing ValueIndex for this key type, or a new one if it does not exist
		 */
		@SuppressWarnings({ "unchecked"})
		public <X> ValueIndex<X> getOrCreate(Class<X> cls) {
			return (ValueIndex<X>) indices.computeIfAbsent(cls, c -> new ValueIndex<X>((Class<X>) c,repo));
		}

		/**
		 * Lookup values of type X that link to a specific lookup value of type Y, 
		 * based on a mapping function that maps X to Y. This is a foreign key lookup 
		 * equivalent and returns objects that link to a key. This will generate an index 
		 * the first time it is called and thereafter use the index.
		 *
		 * @param <X> the return type (the entity type being looked up)
		 * @param <Y> the lookup type (the indexed key type)
		 * @param value the value of the indexed key to lookup (for example an id)
		 * @param clsX the class of the return type X
		 * @param fn must be expressed in the form `X::getY` for it to work properly 
		 *   as a key. If it is not then additional indexes will get created. 
		 * @return a set of values of type X whose `fn` methods return the lookup value
		 */
		public <X,Y> Set<X> lookup(Y value, Class<X> clsX, SerializableFunction<X,Y> fn) {
			@SuppressWarnings("unchecked")
			ValueIndex<Y> valIdx = (ValueIndex<Y>) indices
			.computeIfAbsent(value.getClass(), cls -> new ValueIndex<Y>((Class<Y>) cls, repo));
			return valIdx.lookup(value, clsX, fn);
		}

		/**
		 * Add a ValueIndex to the repository index. This allows manually adding an index for a specific key type, which can be useful for pre-populating indexes or adding custom indexes.
		 *
		 * @param index the ValueIndex to add to the repository index
		 */
		public void add(ValueIndex<?> index) {
			this.indices.put(index.getType(), index);
		}

		protected Map<Class<?>, ValueIndex<?>> getIndices() {
			return indices;
		}

	}

	/**
	 * An interface for entities that can be indexed in the repository. This
	 * interface requires implementing classes to provide a ValueIndex for their
	 * type, which allows them to perform indexed lookups using the repository's
	 * indexing mechanism.
	 *
	 * @param <Y> the type of the entity that implements this interface (used for
	 *            self-referential typing)
	 */
	public static interface Indexed<Y extends Indexed<Y>> {
		
		/**
		 * Get the ValueIndex for this entity's type. This index is used to perform
		 * indexed lookups for this entity type in the repository.
		 * 
		 * @return the ValueIndex for this entity's type
		 */
		@Value.Redacted @JsonIgnore ValueIndex<Y> getIndex();
		
		/**
		 * Find values of type X that link to this entity based on a mapping function
		 * that maps X to Y. This is a foreign key lookup equivalent and returns objects
		 * that link to this entity. This will generate an index the first time it is
		 * called and thereafter use the index.
		 *
		 * @param <X>  the return type (the entity type being looked up)
		 * @param type the class of the return type X
		 * @param fn   must be expressed in the form `X::getY` for it to work properly
		 *             as a key. If it is not then additional indexes will get created.
		 * @return a set of values of type X whose `fn` methods return this entity
		 */
		@SuppressWarnings("unchecked")
		default <X> Set<X> find(Class<X> type, SerializableFunction<X,Y> fn) {
			return this.getIndex().lookup((Y) this, type, fn);
		}
		
//		@SuppressWarnings("unchecked")
//		default <X> X findOne(Class<X> type, SerializableFunction<X,Y> fn) {
//			var tmp = this.getIndex().lookup((Y) this, type, fn);
//			if (tmp.size() == 0) throw new RuntimeException("No "+type.getClass().getSimpleName()+" found for "+this.toString());
//			if (tmp.size() > 1) throw new RuntimeException("Multiple "+type.getClass().getSimpleName()+" found for "+this.toString());
//			return tmp.iterator().next();
//		}
//		
//		@SuppressWarnings("unchecked")
//		default <X> Optional<X> findZeroOrOne(Class<X> type, SerializableFunction<X,Y> fn) {
//			var tmp = this.getIndex().lookup((Y) this, type, fn);
//			if (tmp.size() == 0) return Optional.empty();
//			if (tmp.size() > 1) throw new RuntimeException("Multiple "+type.getClass().getSimpleName()+" found for "+this.toString());
//			return tmp.stream().findAny();
//		}
	}



}
