package com.dooapp.gaedo.blueprints.transformers;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.CascadeType;

import com.dooapp.gaedo.blueprints.BluePrintsBackedFinderService;
import com.dooapp.gaedo.blueprints.BluePrintsPersister;
import com.dooapp.gaedo.blueprints.GraphUtils;
import com.dooapp.gaedo.blueprints.Kind;
import com.dooapp.gaedo.blueprints.Properties;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;

public abstract class AbstractTupleTransformer<TupleType> {

	protected final BluePrintsPersister persister = new BluePrintsPersister(Kind.tuple);

	public <DataType> Vertex getVertexFor(BluePrintsBackedFinderService<DataType, ?> service, TupleType cast, Map<String, Object> objectsBeingUpdated) {
		// First step is to build an id for given tuple by concatenating key and value id (which is hopefully done separately)
		String entryVertexId = getIdOfTuple(service.getDatabase(), service.getRepository(), cast);
		// No need to memorize updated version
		persister.performUpdate(service, entryVertexId, getContainedClass(), getContainedProperties(), cast, CascadeType.PERSIST, objectsBeingUpdated);
		return GraphUtils.locateVertex(service.getDatabase(), Properties.vertexId, entryVertexId);
	}

	/**
	 * As there can be a difference between effectively contained class and claimed contained class, this method don't use the TupleType generics, but rather let subclass
	 * decide how tuples are persisted
	 * @return
	 */
	protected abstract Class<?> getContainedClass();

	protected abstract Map<Property, Collection<CascadeType>> getContainedProperties();

	/**
	 * Create a long string id by concatenating all contained properties ones
	 */
	public String getIdOfTuple(IndexableGraph graph, ServiceRepository repository, TupleType value) {
		StringBuilder sOut = new StringBuilder();
		for(Property p : getContainedProperties().keySet()) {
			Object propertyValue = p.get(value);
			String id = GraphUtils.getIdOf(graph, repository, propertyValue);
			sOut.append(p.getName()).append(":").append(id).append("-");
		}
		return sOut.toString();
	}

	public TupleType loadObject(ClassLoader classLoader, Class effectiveClass, Vertex key, ServiceRepository repository, Map<String, Object> objectsBeingAccessed) {
		TupleType tuple = instanciateTupleFor(classLoader, key);
		persister.loadObjectProperties(classLoader, repository, key, tuple, getContainedProperties(), objectsBeingAccessed);
		return tuple;
	}

	protected abstract TupleType instanciateTupleFor(ClassLoader classLoader, Vertex key);

}
