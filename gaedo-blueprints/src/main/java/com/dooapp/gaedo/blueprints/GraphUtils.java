package com.dooapp.gaedo.blueprints;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.persistence.CascadeType;
import javax.persistence.Column;

import com.dooapp.gaedo.blueprints.annotations.GraphProperty;
import com.dooapp.gaedo.blueprints.indexable.IndexNames;
import com.dooapp.gaedo.blueprints.strategies.GraphMappingStrategy;
import com.dooapp.gaedo.blueprints.strategies.PropertyMappingStrategy;
import com.dooapp.gaedo.blueprints.strategies.UnableToGetVertexTypeException;
import com.dooapp.gaedo.blueprints.transformers.LiteralTransformer;
import com.dooapp.gaedo.blueprints.transformers.Literals;
import com.dooapp.gaedo.blueprints.transformers.TupleTransformer;
import com.dooapp.gaedo.blueprints.transformers.Tuples;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.Vertex;

public class GraphUtils {

	/**
	 * Log level used for "normal" removals. This is the best way to track some weird transaction bugs
	 */
	private static final Level REMOVAL_LOG_LEVEL = Level.FINE;

	private static final String GAEDO_PREFIX = "https://github.com/Riduidel/gaedo/";

	/**
	 * Ontologic context used by all gaedo graph elements.
	 */
	public static final String GAEDO_CONTEXT = GAEDO_PREFIX + "visible";

	/**
	 * Ontologic context used by gaedo graph elements that we want to keep
	 * hidden. Those elements should never be exported. To make sure this works
	 * well, this context is set to null. Crazy no ?
	 */
	public static final String GAEDO_HIDDEN_CONTEXT = GAEDO_PREFIX + "hidden";

	private static final Logger logger = Logger.getLogger(GraphUtils.class.getName());

	/**
	 * Generate edge name from property infos. Notice generated edge name will
	 * first be searched in property annotations, and only if none compatile
	 * found by generating a basic property name
	 *
	 * @param p
	 *            source property
	 * @return an edge name (by default property container class name + "." +
	 *         property name
	 */
	public static String getEdgeNameFor(Property p) {
		if (p.getAnnotation(GraphProperty.class) != null) {
			GraphProperty graph = p.getAnnotation(GraphProperty.class);
			// Test added to avoid default value (which defaults name to "")
			if (graph.name() != null && graph.name().trim().length() > 0)
				return graph.name();
		}
		if (p.getAnnotation(Column.class) != null) {
			Column column = p.getAnnotation(Column.class);
			if (column.name() != null && column.name().trim().length() > 0)
				return column.name();
		}
		return getDefaultEdgeNameFor(p);
	}

	public static String getDefaultEdgeNameFor(Property p) {
		return p.getDeclaringClass().getName() + ":" + p.getName();
	}

	/**
	 * Create a vertex out of a basic object. if object is of a simple type,
	 * we'll use value as id. Elsewhere, we will generate an id for object
	 *
	 * @param database
	 *            database in which vertex will be stored
	 * @param value
	 *            value to get
	 * @param cascade
	 *            cascade type. if nor PERSIST neither MERGE and vertex doesn't
	 *            exist, null may be returned
	 * @return vertex associated to literal or null if none found.
	 */
	public static Vertex getVertexForLiteral(GraphDatabaseDriver database, Object value, CascadeType cascade) {
		Vertex returned = null;
		// Now distinct behaviour between known objects and unknown ones
		Class<? extends Object> valueClass = value.getClass();
		if (Literals.containsKey(valueClass)) {
			LiteralTransformer transformer = Literals.get(valueClass);
			returned = transformer.getVertexFor(database, valueClass.cast(value), cascade);
		} else {
			throw new ObjectIsNotARealLiteralException(value, valueClass);
			// TODO do not forget to set id property
		}
		return returned;
	}

	/**
	 * Create an object instance from a literal vertex compatible with this
	 * service contained class
	 *
	 * @param driver
	 *            driver used to load data
	 * @param strategy
	 *            used graph mapping strategy
	 * @param classLoader
	 *            class loader used to find class
	 * @param key
	 *            vertex containing object id
	 * @param repository
	 *            service repository, used to disambiguate subclass of literal
	 *            and managed class
	 * @param objectsBeingAccessed
	 * @param property
	 *            property used to navigate to this value. it allows
	 *            disambiguation for literal values (which may be linked to more
	 *            than one type, the typical example being a saved float, say
	 *            "3.0", which may also be refered as the string "3.0").
	 * @return a fresh instance, with only id set
	 */
	public static Object createInstance(GraphDatabaseDriver driver, GraphMappingStrategy strategy, ClassLoader classLoader, Vertex key, Class<?> defaultType,
					ServiceRepository repository, ObjectCache objectsBeingAccessed) {
		String effectiveType = null;
		Kind kind = getKindOf(key);
		/*
		 * One literal node may be used according to different types. To
		 * disambiguate, we check if effective type matches default one. If not
		 * (typically type returns string and user wants number), prefer default
		 * type.
		 */
		try {
			effectiveType = driver.getEffectiveType(key);
		} catch (UnableToGetVertexTypeException untypedVertex) {
			try {
				// Don't remember the reason of that mess
				if (!Collection.class.isAssignableFrom(defaultType) && !defaultType.isAssignableFrom(Class.forName(effectiveType))) {
					effectiveType = defaultType.getName();
				}
			} catch (Exception unableToLoadClass) {
				// nothing to do : we use effective type - or try to
			}
			if (effectiveType == null) {
				// First alternative is here for untyped strings in uris nodes
				// (like uris themselves when treated as strings)
				// Second alternative is there for cases when we try to load a
				// collection of untyped thingies
				if (String.class.isAssignableFrom(defaultType) || Collection.class.isAssignableFrom(defaultType))
					effectiveType = GraphMappingStrategy.STRING_TYPE;

			}
		}
		if (classLoader == null) {
			throw new UnspecifiedClassLoader();
		}
		try {
			if (Literals.containsKey(classLoader, effectiveType) && !repository.containsKey(effectiveType)) {
				LiteralTransformer transformer = Literals.get(classLoader, effectiveType);
				return transformer.loadObject(driver, classLoader, effectiveType, key);
			} else {
				Class<?> type = loadClass(classLoader, effectiveType);
				if (Tuples.containsKey(type) && !repository.containsKey(type)) {
					// Tuples are handled the object way (easier, but more
					// dangerous
					TupleTransformer transformer = Tuples.get(type);
					return transformer.loadObject(driver, strategy, classLoader, type, key, repository, objectsBeingAccessed);
				} else {
					return type.newInstance();
				}
			}
		} catch (Exception e) {
			throw UnableToCreateException.dueTo(key, effectiveType, e);
		}
	}

	/**
	 * Load given class if possible.
	 * If not, a clear message should be output (or at least a special exception should be used)
	 * @param classLoader
	 * @param effectiveType the type we want to load
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<?> loadClass(ClassLoader classLoader, String effectiveType) throws ClassNotFoundException {
		try {
			Class<?> returned = classLoader.loadClass(effectiveType);
			if(returned==null) {
				throw new UnableToLoadClassException("unable to load class \""+effectiveType+"\"");
			}
			return returned;
		} catch(Exception e) {
			throw new UnableToLoadClassException("unable to load class \""+effectiveType+"\"", e);
		}
	}

	public static Kind getKindOf(Vertex key) {
		Object kindObject = key.getProperty(Properties.kind.name());
		if(kindObject==null) {
			throw new VertexHasNoPropertyException("vertex "+toString(key)+" has no "+Properties.kind+" property defined.");
		}
		String kindName = kindObject.toString();
		Kind kind = Kind.valueOf(kindName);
		return kind;
	}

	/**
	 * get an id value for the given object whatever the object is
	 *
	 * @param repository
	 * @param value
	 * @return
	 */
	public static <DataType> String getIdOf(ServiceRepository repository, DataType value) {
		Class<? extends Object> valueClass = value.getClass();
		if (repository.containsKey(valueClass)) {
			AbstractBluePrintsBackedFinderService<IndexableGraph, DataType, ?> service = (AbstractBluePrintsBackedFinderService<IndexableGraph, DataType, ?>) repository
							.get(valueClass);
			// All ids are string, don't worry about it
			return service.getIdOf(value).toString();
		} else if (Literals.containsKey(valueClass)) {
			return getIdOfLiteral(valueClass, null, value);
		} else if (Tuples.containsKey(valueClass)) {
			return getIdOfTuple(repository, valueClass, value);
		} else {
			throw new ImpossibleToGetIdOfUnknownType(valueClass);
		}
	}

	/**
	 * Get the value of the vertex id for the given literal
	 *
	 * @param database
	 *            used graph
	 * @param declaredClass
	 *            declared object class
	 * @param idProperty
	 *            gives the declared type of id (which may differ from primitive
	 *            types, where user may give an integer instead of a long, as an
	 *            example). Notice that, contrary to most of gaedo code, this
	 *            field can be null
	 * @param objectId
	 *            object id value
	 * @return the value used by {@link Properties#vertexId} to identify the
	 *         vertex associated to that object
	 */
	public static String getIdOfLiteral(Class<?> declaredClass, Property idProperty, Object objectId) {
		PropertyMappingStrategy strategy = PropertyMappingStrategy.prefixed;
		if (idProperty != null && idProperty.getAnnotation(GraphProperty.class) != null) {
			strategy = idProperty.getAnnotation(GraphProperty.class).mapping();
		}
		return strategy.literalToId(declaredClass, idProperty, objectId);
	}

	/**
	 * Get the value of the vertex id for the given object
	 *
	 * @param database
	 *            used graph
	 * @param declaredClass
	 *            declared object class
	 * @param idProperty
	 *            gives the declared type of id (which may differ from primitive
	 *            types, where user may give an integer instead of a long, as an
	 *            example). Notice that, contrary to most of gaedo code, this
	 *            field can be null
	 * @param value
	 *            object id value
	 * @return the value used by {@link Properties#vertexId} to identify the
	 *         vertex associated to that object
	 */
	public static String getIdOfTuple(ServiceRepository repository, Class<?> declaredClass, Object value) {
		return Tuples.get(declaredClass).getIdOfTuple(repository, value);
	}

	/**
	 * Generates a vertex for the given tuple
	 *
	 * @param bluePrintsBackedFinderService
	 *            source service, some informations may be extracted from it
	 * @param repository
	 *            service repository for non literal values
	 * @param value
	 *            tuple to persist
	 * @param cascade
	 *            cascade type to be used for all operations
	 * @param objectsBeingUpdated
	 *            map of objects already being accessed. Links object id to
	 *            object
	 * @return the
	 */
	public static Vertex getVertexForTuple(AbstractBluePrintsBackedFinderService<? extends Graph, ?, ?> service, ServiceRepository repository, Object value,
					CascadeType cascade, ObjectCache objectsBeingUpdated) {
		Vertex returned = null;
		// Now distinct behaviour between known objects and unknown ones
		Class<? extends Object> valueClass = value.getClass();
		if (Tuples.containsKey(valueClass)) {
			TupleTransformer transformer = Tuples.get(valueClass);
			returned = transformer.getVertexFor(service, valueClass.cast(value), cascade, objectsBeingUpdated);
		} else {
			throw new ObjectIsNotARealTupleException(value, valueClass);
			// TODO do not forget to set id property
		}
		return returned;
	}

	public static Collection<CascadeType> extractCascadeOf(CascadeType[] cascade) {
		Set<CascadeType> returned = new HashSet<CascadeType>();
		returned.addAll(Arrays.asList(cascade));
		if (returned.contains(CascadeType.ALL)) {
			returned.remove(CascadeType.ALL);
			returned.add(CascadeType.MERGE);
			returned.add(CascadeType.PERSIST);
			returned.add(CascadeType.REFRESH);
			returned.add(CascadeType.REMOVE);
		}
		return returned;
	}

	/**
	 * Converts a vertex to a string by outputing all its properties values
	 *
	 * @param objectVertex
	 * @return
	 */
	public static String toString(Vertex objectVertex) {
		StringBuilder sOut = new StringBuilder("{");
		toString(objectVertex, sOut);
		return sOut.append("}").toString();
	}

	public static void toString(Element objectVertex, StringBuilder sOut) {
		sOut.append("graph id=").append(objectVertex.getId().toString());
		for (String s : objectVertex.getPropertyKeys()) {
			sOut.append("; ");
			sOut.append(s).append("=").append(objectVertex.getProperty(s));
		}
	}

	public static String toString(Edge existing) {
		StringBuilder sOut = new StringBuilder();
		sOut.append("{{{");
		sOut.append("\n").append("fromVertex (aka outVertex) => ").append(toString(existing.getVertex(Direction.OUT)));
		sOut.append("\n\t").append(existing.getLabel());
		toString(existing, sOut);
		sOut.append("\n").append("toVertex (aka inVertex) => ").append(toString(existing.getVertex(Direction.IN)));
		sOut.append("\n}}}");
		return sOut.toString();
	}

	/**
	 * Remove an edge "safely". That's to say with prior existence check.
	 *
	 * @param database
	 *            database from which edge should be removed
	 * @param existing
	 *            edge to remove
	 */
	public static void removeSafely(Graph database, Edge existing) {
		if (logger.isLoggable(REMOVAL_LOG_LEVEL)) {
			logger.log(REMOVAL_LOG_LEVEL, "removing safely " + existing);
		}
		Edge toRemove = null;
		if ((toRemove = database.getEdge(existing.getId())) == null) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.log(Level.WARNING, "We tried to remove non existing edge " + toString(existing));
			}
		} else {
			removeFromIndex(database, existing, IndexNames.EDGES);
			database.removeEdge(toRemove);
			if (logger.isLoggable(REMOVAL_LOG_LEVEL)) {
				logger.log(REMOVAL_LOG_LEVEL, "REMOVED " + toRemove);
			}
		}
	}

	public static void removeSafely(Graph database, Vertex existing) {
		if (logger.isLoggable(REMOVAL_LOG_LEVEL)) {
			logger.log(REMOVAL_LOG_LEVEL, "removing safely " + existing);
		}
		Vertex toRemove = null;
		if ((toRemove = database.getVertex(existing.getId())) == null) {
			if (logger.isLoggable(Level.WARNING)) {
				logger.log(Level.WARNING, "We tried to remove non existing vertex " + toString(existing));
			}
		} else {
			removeFromIndex(database, existing, IndexNames.VERTICES);
			database.removeVertex(toRemove);
			if (logger.isLoggable(REMOVAL_LOG_LEVEL)) {
				logger.log(REMOVAL_LOG_LEVEL, "REMOVED " + toRemove);
			}
		}
	}

	/**
	 * Remove given element from index by removing all bindings from its properties names to its properties values
	 * @param database graph on which remove operation will be performed (must be indexable)
	 * @param existing element to remove index entries
	 * @param indexName name of index associated to element
	 */
	public static <Type extends Element> void removeFromIndex(Graph database, Type existing, IndexNames indexName) {
		if (database instanceof IndexableGraph) {
			IndexableGraph indexable = (IndexableGraph) database;
			if(indexName.isUsable()) {
				Index<Type> index = (Index<Type>) indexable.getIndex(indexName.getIndexName(), indexName.getIndexed());
				for(String propertyName : existing.getPropertyKeys()) {
					index.remove(propertyName, existing.getProperty(propertyName), existing);
				}
			}
		}
	}

	/**
	 * Define if a vertex can be created when using the given cascade type
	 *
	 * @param cascade
	 * @return true for PERSIST and MERGE, false otherwise
	 */
	public static boolean canCreateVertex(CascadeType cascade) {
		switch (cascade) {
		case PERSIST:
		case MERGE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Set an indexed property on any graph element, updating the given list of indices
	 * @param graph graph from which indices should be loaded
	 * @param graphElement graph element to update
	 * @param propertyName property to set
	 * @param propertyValue value to set
	 * @param indexName index to update. Notice removing value from index on mutation is not supported, as gaedo vertices and edges are not considered as mutable
	 */
	public static <ElementType extends Element> void setIndexedProperty(IndexableGraph graph, ElementType graphElement, String propertyName, Object propertyValue, IndexNames indexName) {
		graphElement.setProperty(propertyName, propertyValue);
		if(IndexNames.usableIndices().contains(indexName)) {
			Index<ElementType> index = graph.getIndex(indexName.getIndexName(), (Class<ElementType>) indexName.getIndexed());
			index.put(propertyName, propertyValue, graphElement);
		}
	}
}
