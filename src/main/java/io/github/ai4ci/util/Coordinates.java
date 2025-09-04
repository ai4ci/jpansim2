package io.github.ai4ci.util;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Value.Immutable
public interface Coordinates {
	
	public double[] getX();
	public double[] getY();
	
	public LinkFunction getXLink();
	public LinkFunction getYLink();
	
	@Value.Default default double getXMin() { return getXLink().getMinSupport(); }
	@Value.Default default double getXMax() { return getXLink().getMaxSupport(); }
	@Value.Default default double getYMin() { return getYLink().getMinSupport(); }
	@Value.Default default double getYMax() { return getYLink().getMaxSupport(); }
	
	public boolean isIncreasing();
	@Value.Default default DuplicateResolution resolveDuplicates() {return DuplicateResolution.MEAN;}
	
	public static enum DuplicateResolution {MEAN, MAX, MIN};
	
//	static Map<Coordinates, SortedMap<Double,List<Double>>> CACHE = new HashMap<>();
//	
//	default SortedMap<Double,List<Double>> getMapping() {
//		return CACHE.computeIfAbsent(this, Coordinates::mapped);
//	}
	
	private SortedMap<Double,List<Double>> getMapping() {
		SortedMap<Double,List<Double>> out = new TreeMap<>();
		for (int i=0; i < getX().length; i++) {
			double x = getX()[i];
			double y = getY()[i];
			if (x <= getXMax() && x >= getXMin() && y <= getYMax() && y >= getYMin()) {
				double tx = getXLink().fn(x);
				double ty = getYLink().fn(y);
				out.putIfAbsent(
						tx, new ArrayList<>());
				out.get(tx).add(ty);
			}
		}
		if (isIncreasing()) {
			out.putIfAbsent(
					getXLink().fn(getXMin()), 
					List.of(getYLink().fn(getYMin()))
				);
			out.putIfAbsent(
					getXLink().fn(getXMax()), 
					List.of(getYLink().fn(getYMax()))
				);
		}
		return out;
	}
	
	@Value.Derived
	@JsonIgnore
	public default double[] getHx() {
		return getMapping().keySet().stream().mapToDouble(d -> d).toArray();
	}
	
	@Value.Derived
	@JsonIgnore
	public default double[] getHy() {
		return getMapping().values().stream().mapToDouble(
			list -> { 
				if (resolveDuplicates().equals(DuplicateResolution.MEAN)) {
					return list.stream().mapToDouble(d->d).average().getAsDouble();
				} else if (resolveDuplicates().equals(DuplicateResolution.MAX)) {
					return list.stream().mapToDouble(d->d).max().getAsDouble();
				} else if (resolveDuplicates().equals(DuplicateResolution.MIN)) {
					return list.stream().mapToDouble(d->d).min().getAsDouble();
				} else {
					throw new RuntimeException();
				}
			}).toArray();
	}
	


	
	
}
