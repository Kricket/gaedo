package com.dooapp.gaedo.blueprints.indexable;


import java.util.Collection;
import java.util.SortedSet;
import java.util.UUID;

import com.dooapp.gaedo.blueprints.AbstractBluePrintsBackedFinderService;
import com.dooapp.gaedo.blueprints.GraphUtils;
import com.dooapp.gaedo.blueprints.Kind;
import com.dooapp.gaedo.blueprints.Properties;
import com.dooapp.gaedo.blueprints.strategies.GraphMappingStrategy;
import com.dooapp.gaedo.blueprints.strategies.StrategyType;
import com.dooapp.gaedo.blueprints.transformers.ClassLiteralTransformer;
import com.dooapp.gaedo.blueprints.transformers.Literals;
import com.dooapp.gaedo.blueprints.transformers.Tuples;
import com.dooapp.gaedo.blueprints.transformers.TypeUtils;
import com.dooapp.gaedo.extensions.views.InViewService;
import com.dooapp.gaedo.finders.Informer;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.finders.root.InformerFactory;
import com.dooapp.gaedo.properties.Property;
import com.dooapp.gaedo.properties.PropertyProvider;
import com.dooapp.gaedo.properties.TypeProperty;
import com.tinkerpop.blueprints.pgm.CloseableSequence;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.oupls.sail.GraphSail;

/**
 * Indexable graph backed version of finder service.
 * 
 * Notice we maintain {@link AbstractCooperantFinderService} infos about objects being accessed as String containing, in fact, vertex ids
 * @author ndx
 *
 * @param <DataType> type of data managed by this service
 * @param <InformerType> type of informer used to provide infos about managed data
 */
public class IndexableGraphBackedFinderService <DataType, InformerType extends Informer<DataType>> 
	extends AbstractBluePrintsBackedFinderService<IndexableGraph, DataType, InformerType> {

	public static final String TYPE_EDGE_NAME = GraphUtils.getEdgeNameFor(TypeProperty.INSTANCE);

	public static ClassLiteralTransformer classTransformer = (ClassLiteralTransformer) Literals.get(Class.class);

	/**
	 * Construct a default service, for which the mapping strategy is the default one (that's to say {@link StrategyType#beanBased}
	 * @param containedClass contained data class
	 * @param informerClass informer calss associated to that data class
	 * @param factory informer factory used when performing queries
	 * @param repository service repository used to load other classes
	 * @param provider property provider
	 * @param graph graph used as storage
	 * @see IndexableGraphBackedFinderService#IndexableGraphBackedFinderService(IndexableGraph, Class, Class, InformerFactory, ServiceRepository, PropertyProvider, StrategyType)
	 * @deprecated replaced by {@link IndexableGraphBackedFinderService#IndexableGraphBackedFinderService(IndexableGraph, Class, Class, InformerFactory, ServiceRepository, PropertyProvider)}
	 */
	public IndexableGraphBackedFinderService(Class<DataType> containedClass, Class<InformerType> informerClass, InformerFactory factory, ServiceRepository repository,
					PropertyProvider provider, IndexableGraph graph) {
		this(graph, containedClass, informerClass, factory, repository, provider, StrategyType.beanBased);
	}
	
	

	public IndexableGraphBackedFinderService(IndexableGraph graph, Class<DataType> containedClass, Class<InformerType> informerClass, InformerFactory factory,
					ServiceRepository repository, PropertyProvider provider, GraphMappingStrategy<DataType> strategy) {
		super(graph, containedClass, informerClass, factory, repository, provider, strategy);
	}


	/**
	 * Construct a gaedo servcie allowing reading/writing to an indexable graph
	 * @param graph graph we want to write/read to/from
	 * @param containedClass class we want to map to that graph
	 * @param informerClass informer used to allow easy queries on that class
	 * @param factory informer factory
	 * @param repository service repository, to load other classes
	 * @param provider property provider
	 * @param strategy mapping strategy. If bean based, the bean fields will define which edges are read/written. If graph based, that's the edges that define how the object will be loaded.
	 */
	public IndexableGraphBackedFinderService(IndexableGraph graph, Class<DataType> containedClass, Class<InformerType> informerClass, InformerFactory factory,
					ServiceRepository repository, PropertyProvider provider, StrategyType strategy) {
		super(graph, containedClass, informerClass, factory, repository, provider, strategy);
	}



	public IndexableGraphBackedFinderService(IndexableGraph graph, Class<DataType> containedClass, Class<InformerType> informerClass, InformerFactory factory,
					ServiceRepository repository, PropertyProvider provider) {
		super(graph, containedClass, informerClass, factory, repository, provider);
	}



	@Override
	public Vertex loadVertexFor(String objectVertexId, String className) {
		CloseableSequence<Vertex> matching = database.getIndex(Index.VERTICES, Vertex.class).get(Properties.value.name(), objectVertexId);
		if(matching.hasNext()) {
			while(matching.hasNext()) {
				Vertex vertex = matching.next();
				String vertexTypeName = getEffectiveType(vertex);
				if(Kind.literal==GraphUtils.getKindOf(vertex)) {
					if(className.equals(vertexTypeName)) {
						return vertex;
					}
				} else {
					return vertex;
				}
			}
		}
		return null;
	}

	@Override
	public String getIdOfVertex(Vertex objectVertex) {
		return objectVertex.getProperty(Properties.value.name()).toString();
	}

	/**
	 * When creating an empty vertex, we immediatly link it to its associated type vertex : a long will as a consequence be linked to the Long class
	 * @param vertexId
	 * @param valueClass
	 * @return
	 * @see com.dooapp.gaedo.blueprints.AbstractBluePrintsBackedFinderService#createEmptyVertex(java.lang.String, java.lang.Class)
	 */
	@Override
	protected Vertex createEmptyVertex(String vertexId, Class<? extends Object> valueClass) {
		// technical vertex id is no more used by gaedo which only rley upon the getIdOfVertex method !
		Vertex returned = database.addVertex(valueClass.getName()+":"+vertexId);
		returned.setProperty(Properties.value.name(), vertexId);
		if(Literals.containsKey(valueClass)) {
			// some literals aren't so ... literal, as they can accept incoming connections (like classes)
			returned.setProperty(Properties.kind.name(), Literals.get(valueClass).getKind().name());
			returned.setProperty(Properties.type.name(), TypeUtils.getType(valueClass));
		} else {
			if(repository.containsKey(valueClass)){
				returned.setProperty(Properties.kind.name(), Kind.uri.name());
			} else if(Tuples.containsKey(valueClass)) {
				// some literals aren't so ... literal, as they can accept incoming connections (like classes)
				returned.setProperty(Properties.kind.name(), Tuples.get(valueClass).getKind().name());
			}
			// obtain vertex for type
			Vertex classVertex = classTransformer.getVertexFor(getDriver(), valueClass);
			Edge toType = getDriver().createEdgeFor(returned, classVertex, TypeProperty.INSTANCE);
			/*
			 * Make sure literals are literals by changing that particular edge context to a null value.
			 *  Notice we COULD have stored literal type as a property, instead of using
			 */
			toType.setProperty(GraphSail.CONTEXT_PROP, GraphUtils.asSailProperty(GraphUtils.GAEDO_CONTEXT));
		}
		// Yup, this if has no default else statement, and that's normal.
		
		return returned;
	}

	@Override
	protected String getEffectiveType(Vertex vertex) {
		return getStrategy().getEffectiveType(vertex);
	}

	@Override
	protected void setValue(Vertex vertex, Object value) {
		vertex.setProperty(Properties.value.name(), value);
	}

	@Override
	protected Object getValue(Vertex vertex) {
		return vertex.getProperty(Properties.value.name());
	}

	public Edge createEdgeFor(Vertex fromVertex, Vertex toVertex, Property property) {
		String edgeNameFor = GraphUtils.getEdgeNameFor(property);
		Edge edge = database.addEdge(getEdgeId(fromVertex, toVertex, property), fromVertex, toVertex, edgeNameFor);
		String predicateProperty = GraphUtils.asSailProperty(GraphUtils.getEdgeNameFor(property));
		edge.setProperty(GraphSail.PREDICATE_PROP, predicateProperty);
		Collection<String> contexts = getLens();
		StringBuilder contextPropertyBuilder = new StringBuilder();
		if(contexts.size()==0) {
			contextPropertyBuilder.append(GraphUtils.asSailProperty(GraphSail.NULL_CONTEXT_NATIVE));
		} else {
			for(String context : contexts) {
				if(contextPropertyBuilder.length()>0)
					contextPropertyBuilder.append(" " );
				contextPropertyBuilder.append(GraphUtils.asSailProperty(context));
				
			}
		}
		String contextProperty = contextPropertyBuilder.toString();
		edge.setProperty(GraphSail.CONTEXT_PROP, contextProperty);
		// Finally build the context-predicate property by concatenating both
		edge.setProperty(GraphSail.CONTEXT_PROP + GraphSail.PREDICATE_PROP, contextProperty+" "+predicateProperty);
		return edge;
	}



	public String getEdgeId(Vertex fromVertex, Vertex toVertex, Property property) {
		return fromVertex.getId().toString()+"_to_"+toVertex.getId().toString()+"___"+UUID.randomUUID().toString(); 
	}



	@Override
	public InViewService<DataType, InformerType, SortedSet<String>> focusOn(SortedSet<String> lens) {
		AbstractBluePrintsBackedFinderService<IndexableGraph, DataType, InformerType> returned = 
						new IndexableGraphBackedFinderService<DataType, InformerType>(
										database, 
										containedClass, 
										informerClass, 
										getInformerFactory(), 
										repository, 
										propertyProvider, 
										getStrategy());
		returned.setLens(lens);
		return returned;
	}

}