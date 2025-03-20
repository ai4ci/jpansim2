package io.github.ai4ci.output;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class CSVWriter<X> implements Closeable {
	
	File file;
	CsvMapper cm;
	CsvSchema sch;
	Class<X> type;
	SequenceWriter seqW;
	
	public void export(Stream<X> supply) {
		supply.forEach(this::export);
	}
	
	public static <X> CSVWriter<X> of(Class<X> type, File file) throws IOException {
		return new CSVWriter<X>(type, file);
	}
	
	private CSVWriter(Class<X> type, File file) throws IOException {
		this.type = type;
		cm = CsvMapper.builder()
				.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
				.build();
		CsvSchema sch = cm.schemaFor(type)
				.withHeader()
				.withColumnReordering(false);
		FileWriter strW = new FileWriter(file);
		seqW = cm.writer(sch).writeValues(strW);
	}
	
	public void export(X single) {
		try {
			seqW.write(single);
		} catch (IOException e) {
			// TODO: Handle more gracefully
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		seqW.close();
	}
	
}
