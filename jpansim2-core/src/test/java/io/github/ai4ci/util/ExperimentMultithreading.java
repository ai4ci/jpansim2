package io.github.ai4ci.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.LongStream;

public class ExperimentMultithreading {

	public static void main(String... args) {
		
		Long n = (long) 10e7;
		
		
//		{
//			ConcurrentLinkedQueue<Long> clq = new ConcurrentLinkedQueue<>();
//			
//			long start = System.currentTimeMillis();
//			LongStream.range(1, n).parallel().forEach(l ->
//					clq.add(l % 100)
//			);
//			long[] out = clq.stream().unordered().distinct().mapToLong(l->l).toArray(); 
//			long duration = System.currentTimeMillis() - start;
//			System.out.print(out.length+" "+duration+"\n");
//		}
		
		{
			ConcurrentHashMap<Long,Long> cs = new ConcurrentHashMap<Long,Long>();
		
		
			long start = System.currentTimeMillis();
			LongStream.range(1, n).parallel().forEach(l ->
					cs.put(l % 100, l)
			);
			long[] out = cs.values().stream().mapToLong(l->l).toArray(); 
			long duration = System.currentTimeMillis() - start;
			System.out.print(out.length+" "+duration+"\n");
		}
		
		{
			ConcurrentSkipListMap<Long,Long> cs = new ConcurrentSkipListMap<Long,Long>();
		
		
			long start = System.currentTimeMillis();
			LongStream.range(1, n).parallel().forEach(l ->
					cs.put(l % 100, l)
			);
			long[] out = cs.values().stream().mapToLong(l->l).toArray(); 
			long duration = System.currentTimeMillis() - start;
			System.out.print(out.length+" "+duration+"\n");
		}
	}
	
}
