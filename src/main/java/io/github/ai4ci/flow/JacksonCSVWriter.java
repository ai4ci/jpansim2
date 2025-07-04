//package io.github.ai4ci.flow;
//
//import java.io.BufferedOutputStream;
//import java.io.Closeable;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.stream.Stream;
//
//import com.fasterxml.jackson.databind.MapperFeature;
//import com.fasterxml.jackson.databind.SequenceWriter;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.dataformat.csv.CsvMapper;
//import com.fasterxml.jackson.dataformat.csv.CsvSchema;
//
//import io.github.ai4ci.flow.CSVWriter.Writeable;
//
//public class JacksonCSVWriter<X extends CSVWriter.Writeable> implements Closeable {
//	
//	File file;
//	CsvMapper cm;
//	CsvSchema sch;
//	Class<X> type;
//	QueueWriter<X> queueWriter;
//	
//	public static class QueueWriter<X extends CSVWriter.Writeable> extends Thread {
//		
//		ConcurrentLinkedQueue<X> queue;
//		SequenceWriter seqW;
//		volatile boolean stop = false;
//		
//		QueueWriter(SequenceWriter seqW) {
//			this.seqW = seqW;
//			this.queue = new ConcurrentLinkedQueue<>();
//		}
//		
//		public void queue(X item) {
//			this.queue.add(item);
//		}
//		
//		public void halt() {
//			this.stop = true;
//		}
//		
//		public void run() {
//			try {
//				
//				while (!stop) {
//					while (!this.queue.isEmpty()) {
//						seqW.write(queue.poll());
//					}
//					Thread.yield();
//				}
//				
//				while (!this.queue.isEmpty()) {
//					seqW.write(queue.poll());
//				}
//				this.seqW.close();
//				
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		}
//		
//	}
//	
//	public void export(Stream<X> supply) {
//		supply.forEach(this::export);
//	}
//	
//	public static <X extends Writeable> JacksonCSVWriter<X> of(Class<X> type, File file) throws IOException {
//		return new JacksonCSVWriter<X>(type, file);
//	}
//	
//	private JacksonCSVWriter(Class<X> type, File file) throws IOException {
//		this.type = type;
//		
//		cm = CsvMapper.builder()
//				.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
//				.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE)
//				.build();
//		CsvSchema sch = cm.schemaFor(type)
//				.withHeader()
//				.withColumnReordering(false);
//		// FileWriter strW = new FileWriter(file));
//		BufferedOutputStream strW = new BufferedOutputStream(new FileOutputStream(file), 256*1024*1024);
//		// FastWriteOnlyOutputStream strW = new FastWriteOnlyOutputStream(file.toPath(), 256);
//		queueWriter = new QueueWriter<X>(cm.writer(sch).writeValues(strW));
//		//queueWriter.setDaemon(true);
//		queueWriter.setName(type.getSimpleName()+" writer");
//		queueWriter.start();
//		
//	}
//	
//	public void export(X single) {
//		queueWriter.queue(single);
//	}
//
//	@Override
//	public void close() {
//		queueWriter.halt();
//	}
//	
//}
