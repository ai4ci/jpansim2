//package io.github.ai4ci.util;
//
//import java.io.Serializable;
//import java.util.Collection;
//import java.util.Set;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.CountDownLatch;
//import java.util.function.Supplier;
//
//import org.jgrapht.Graph;
//import org.jgrapht.GraphType;
//import org.jgrapht.alg.util.Pair;
//import org.jgrapht.alg.util.Triple;
//
//public class NetworkBuilder<V, E> implements Graph<V,E>, Serializable {
//
//	transient CountDownLatch latch;
//	volatile Graph<V,E> graph;
//	transient GraphWriter graphWriter;
//	
//	public static <V2,E2> NetworkBuilder<V2,E2> from(Graph<V2,E2> graph) {
//		return new NetworkBuilder<V2,E2>(graph);
//	}
//	
//	private CountDownLatch latch() {
//		if (latch == null) latch = new CountDownLatch(0);
//		return latch;
//	}
//	
//	private NetworkBuilder(Graph<V,E> graph) {
//		this.graph = graph;
//	}
//	
//	private class GraphWriter extends Thread {
//		ConcurrentLinkedQueue<V> vertexQueue = new ConcurrentLinkedQueue<>();
//		ConcurrentLinkedQueue<Triple<V,V,E>> edgeQueue= new ConcurrentLinkedQueue<>();
//		ConcurrentLinkedQueue<Pair<E,Double>> edgeWeightQueue = new ConcurrentLinkedQueue<>();
//		volatile boolean stop = false;
//		public void queueEdge(V vertex,V vertex2, E edge) {
//			edgeQueue.add(Triple.of(vertex, vertex2, edge));
//		}
//		public void queueVertex(V vertex) {
//			vertexQueue.add(vertex);
//		}
//		public void queueEdgeWeight(E edge, Double weight) {
//			edgeWeightQueue.add(Pair.of(edge, weight));
//		}
//		public void run() {
//			while (!stop) {
//				while (!this.vertexQueue.isEmpty()) {
//					graph.addVertex(vertexQueue.poll());
//				}
//				while (!this.edgeQueue.isEmpty()) {
//					Triple<V,V,E> tmp = edgeQueue.poll();
//					graph.addEdge(tmp.getFirst(), tmp.getSecond(), tmp.getThird());
//				}
//				while (!this.edgeWeightQueue.isEmpty()) {
//					Pair<E,Double> tmp = edgeWeightQueue.poll();
//					graph.setEdgeWeight(tmp.getFirst(), tmp.getSecond());
//				}
//				try {
//					Thread.sleep(1);
//				} catch (InterruptedException e) {
//					stop = true;
//				}
//			}
//			latch().countDown();
//		}
//		
//		public void finishWriting() {
//			this.stop = true;
//		}
//	}
//	
//	private void await() {
//		try {
//			latch().await();		
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//	}
//	
//	// READ METHODS
//	
//	@Override
//	public Set<E> getAllEdges(V sourceVertex, V targetVertex) {
//		await();
//		return graph.getAllEdges(sourceVertex, targetVertex);
//	}
//
//	@Override
//	public E getEdge(V sourceVertex, V targetVertex) {
//		await();
//		return graph.getEdge(sourceVertex, targetVertex);
//	}
//
//	@Override
//	public Supplier<V> getVertexSupplier() {
//		return graph.getVertexSupplier();
//	}
//
//	@Override
//	public Supplier<E> getEdgeSupplier() {
//		return graph.getEdgeSupplier();
//	}
//
//	@Override
//	public boolean containsEdge(V sourceVertex, V targetVertex) {
//		return graph.containsEdge(sourceVertex, targetVertex);
//	}
//
//	@Override
//	public boolean containsEdge(E e) {
//		return graph.containsEdge(e);
//	}
//
//	@Override
//	public boolean containsVertex(V v) {
//		return graph.containsVertex(v);
//	}
//
//	@Override
//	public Set<E> edgeSet() {
//		await();
//		return graph.edgeSet();
//	}
//
//	@Override
//	public int degreeOf(V vertex) {
//		await();
//		return graph.degreeOf(vertex);
//	}
//
//	@Override
//	public Set<E> edgesOf(V vertex) {
//		await();
//		return graph.edgesOf(vertex);
//	}
//
//	@Override
//	public int inDegreeOf(V vertex) {
//		await();
//		return graph.inDegreeOf(vertex);
//	}
//
//	@Override
//	public Set<E> incomingEdgesOf(V vertex) {
//		await();
//		return graph.incomingEdgesOf(vertex);
//	}
//
//	@Override
//	public int outDegreeOf(V vertex) {
//		await();
//		return graph.outDegreeOf(vertex);
//	}
//
//	@Override
//	public Set<E> outgoingEdgesOf(V vertex) {
//		await();
//		return graph.outgoingEdgesOf(vertex);
//	}
//
//	
//
//	@Override
//	public Set<V> vertexSet() {
//		await();
//		return graph.vertexSet(); 
//	}
//
//	@Override
//	public V getEdgeSource(E e) {
//		await();
//		return graph.getEdgeSource(e);
//	}
//
//	@Override
//	public V getEdgeTarget(E e) {
//		await();
//		return graph.getEdgeTarget(e);
//	}
//
//	@Override
//	public GraphType getType() {
//		return graph.getType();
//	}
//
//	@Override
//	public double getEdgeWeight(E e) {
//		await();
//		return graph.getEdgeWeight(e);
//	}
//	
//	public void enableWriting() {
//		if (latch().getCount() > 0) {
//			return;
//		} else {
//			this.graphWriter = new GraphWriter();
//			this.graphWriter.setDaemon(true);
//			this.latch = new CountDownLatch(1);
//			this.graphWriter.start();
//		}
//	}
//	
//	public void completeWriting() {
//		checkWriteable();
//		this.graphWriter.finishWriting();
//	}
//	
//	public void checkWriteable() {
//		if (latch().getCount() == 0) throw new RuntimeException("Not enabled for writing.");
//	}
//	
//	@Override
//	public E addEdge(V sourceVertex, V targetVertex) {
//		E tmp = this.getEdgeSupplier().get();
//		this.addEdge(sourceVertex, targetVertex, tmp);
//		return tmp;
//	}
//	
//	@Override
//	public V addVertex() {
//		V tmp = this.getVertexSupplier().get();
//		addVertex(tmp);
//		return tmp;
//	}
//
//	@Override
//	public boolean addEdge(V sourceVertex, V targetVertex, E e) {
//		checkWriteable();
//		graphWriter.queueEdge(sourceVertex, targetVertex, e);
//		return true;
//	}
//
//	@Override
//	public void setEdgeWeight(E e, double weight) {
//		checkWriteable();
//		graphWriter.queueEdgeWeight(e,weight);
//	}
//	
//	@Override
//	public boolean addVertex(V v) {
//		checkWriteable();
//		graphWriter.queueVertex(v);
//		return true;
//	}
//
//	@Override
//	public boolean removeAllEdges(Collection<? extends E> edges) {
//		throw new RuntimeException("unsupported operation");
//	}
//
//	@Override
//	public Set<E> removeAllEdges(V sourceVertex, V targetVertex) {
//		throw new RuntimeException("unsupported operation");
//	}
//
//	@Override
//	public boolean removeAllVertices(Collection<? extends V> vertices) {
//		throw new RuntimeException("unsupported operation");
//	}
//
//	@Override
//	public E removeEdge(V sourceVertex, V targetVertex) {
//		throw new RuntimeException("unsupported operation");
//	}
//
//	@Override
//	public boolean removeEdge(E e) {
//		throw new RuntimeException("unsupported operation");
//	}
//
//	@Override
//	public boolean removeVertex(V v) {
//		throw new RuntimeException("unsupported operation");
//	}
//	
//}
