package io.github.ai4ci.util;

import java.io.IOException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import io.github.ai4ci.Import;

/** 
 * A reflection based data repository class with support for foreign keys 
 * for data imported from multiple CSV files. Order of loading CSV files is
 * important. Only many:1 foreign key relations supported (no collections).
 * Integrates with immutables.
 */
public class Repository {

	public Repository(Path baseDirectory) {
		super();
		this.baseDirectory = baseDirectory;
	}

	Map<Class<?>, Map<String, Object>> dataStore = new HashMap<>();
	Path baseDirectory;
	
	public boolean hasType(Class<?> cls) {
		return dataStore.containsKey(cls);
	}
	
	public Repository withType(Class<?> cls) {
		dataStore.computeIfAbsent(cls, c -> new HashMap<>());
		return this;
	}
	
	public <X> X getValue(Class<X> cls, String key) {
		this.withType(cls);
		Object out = dataStore.get(cls).get(key);
		return cls.cast(out);
	}
	
	private <X> void setValue(Class<X> cls, String key, X out) {
		this.withType(cls);
		this.dataStore.get(cls).put(key, out);
	}
	
	public <X> Stream<X> streamValues(Class<X> cls) {
		this.withType(cls);
		Collection<Object> out = dataStore.get(cls).values();
		return out.stream().map(o -> cls.cast(o));
	}
	
	public <X> Repository readCSV(Class<X> cls) throws IOException {
		
		try {
		Method getId = Stream.of(cls.getMethods()).filter(m -> m.isAnnotationPresent(Import.Id.class)).findFirst().orElseThrow();
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
				if (m.isAnnotationPresent(JsonProperty.class)) {
					
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



	public static class CSVReader<X> implements Iterator<Map<String, Object>>, AutoCloseable {

	    private final MappingIterator<Map<String, String>> iterator;
	    private final Map<String, Class<?>> columnTypes;
	    private final Map<String, Boolean> isReferenceField;

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

	            if (value == null || value.trim().isEmpty()) {
	                typedRow.put(colName, null);
	                continue;
	            }

	            Class<?> targetType = columnTypes.getOrDefault(colName, String.class);
	            Boolean ref = isReferenceField.get(colName);
	            if (Boolean.TRUE.equals(ref)) {
	                // Foreign key: always Integer
	                try {
	                    typedRow.put(colName, Integer.parseInt(value.trim()));
	                } catch (NumberFormatException e) {
	                    throw new RuntimeException("Invalid integer value for foreign key column '" + colName + "': " + value);
	                }
	            } else {
	                // Convert to expected type
	                Object converted = convertValue(value.trim(), targetType);
	                typedRow.put(colName, converted);
	            }
	        }

	        return typedRow;
	    }

	    // Basic type conversion (extendable)
	    private Object convertValue(String value, Class<?> type) {
	        if (value == null || value.isEmpty()) return null;

	        if (type == String.class) {
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
	            return Boolean.parseBoolean(value);
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
	                graph.get(returnType).add(cls);
	                inDegree.merge(cls, 1, Integer::sum);
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
	
}
