package com.dooapp.gaedo.blueprints.transformers;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.persistence.CascadeType;

import com.dooapp.gaedo.blueprints.BluePrintsBackedFinderService;
import com.dooapp.gaedo.blueprints.GraphUtils;
import com.dooapp.gaedo.blueprints.Kind;
import com.dooapp.gaedo.blueprints.Properties;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;

/**
 * Serializable handling is quite particular. Instead of saving a simple node type, this transformer checks the contained value and try to save it according to its inner type 
 * (using {@link LiteralTransformer}, others {@link TupleTransformer} and services). This provide compact DB with good performances, at the cost of a complex class ... this one.
 * @author ndx
 *
 */
public class SerializableTransformer implements TupleTransformer<Serializable> {
	private Serializable readSerializable(String valueString) {
		ByteArrayInputStream stream = new ByteArrayInputStream(valueString.getBytes());
		XMLDecoder decoder = new XMLDecoder(stream);
		return (Serializable) decoder.readObject();
	}

	private String writeSerializable(Serializable value) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		XMLEncoder encoder = new XMLEncoder(stream);
		encoder.writeObject(value);
		encoder.close();
		try {
			stream.close();
			return new String(stream.toByteArray());
		} catch (IOException e) {
			throw new UnableToStoreSerializableException("impossible to store serializable value "+value, e);
		}
	}

	/**
	 * Get vertex for the value. To get that vertex, we first check the effective value type. If a literal or a tuple (other than serializable), we use that vertex value.
	 * If a managed object, we use the service associated to that object. Finally, if an unknown serializable ... well, we serialize it
	 * @param service
	 * @param cast
	 * @param objectsBeingUpdated
	 * @return
	 * @see com.dooapp.gaedo.blueprints.transformers.TupleTransformer#getVertexFor(com.dooapp.gaedo.blueprints.BluePrintsBackedFinderService, java.lang.Object, java.util.Map)
	 */
	@Override
	public <DataType> Vertex getVertexFor(BluePrintsBackedFinderService<DataType, ?> service, Serializable cast, Map<String, Object> objectsBeingUpdated) {
		IndexableGraph database = service.getDatabase();
		ServiceRepository repository = service.getRepository();
		// some first-level check to see if someone else than this transformer has any knowledge of value (because, well, this id will be longer than hell)
		Class<? extends Serializable> valueClass = cast.getClass();
		if(Tuples.containsKey(valueClass)) {
			if(Tuples.get(valueClass).equals(this)) {
				return getVertextForUnknownSerializable(database, repository, cast);
			}
		}
		// Gently ask service for effective access to value
		return service.getVertexFor(cast, null, objectsBeingUpdated);
	}

	
	private Vertex getVertextForUnknownSerializable(IndexableGraph database, ServiceRepository repository, Serializable value) {
		String serialized = writeSerializable(value);
		Object vertexId = serialized;
		// First try direct vertexId access
		if(database.getVertex(vertexId)!=null) {
			return database.getVertex(vertexId);
		}
		// Then indexed vertex id (for neo4j, typically)
		Vertex returned = GraphUtils.locateVertex(database, Properties.vertexId.name(), vertexId);
		// Finally create vertex
		if(returned==null) {
			returned = database.addVertex(vertexId);
			returned.setProperty(Properties.value.name(), serialized);
			returned.setProperty(Properties.vertexId.name(), vertexId);
			returned.setProperty(Properties.kind.name(), Kind.tuple.name());
			returned.setProperty(Properties.type.name(), Serializable.class.getCanonicalName());
		}
		return returned;
	}

	@Override
	public String getIdOfTuple(IndexableGraph graph, ServiceRepository repository, Serializable value) {
		// some first-level check to see if someone else than this transformer has any knowledge of value (because, well, this id will be longer than hell)
		Class<? extends Serializable> valueClass = value.getClass();
		if(Tuples.containsKey(valueClass)) {
			if(Tuples.get(valueClass).equals(this)) {
				return writeSerializable(value);
			}
		}
		// Delegate to the rest of the world
		return GraphUtils.getIdOf(graph, repository, value);
	}

	/**
	 * For loading object, reverse job iof persisting is done, but way simpler, as using other persistences mechanisms allows us to load known serializable
	 * using their assocaited literal transformers/services, which is WAAAAYYYYYYY cooler.
	 * So, this method simple job is just to read value and deserialize it. Nice, no ?
	 * @param classLoader
	 * @param effectiveClass
	 * @param key
	 * @param repository
	 * @param objectsBeingAccessed
	 * @return
	 * @see com.dooapp.gaedo.blueprints.transformers.TupleTransformer#loadObject(java.lang.ClassLoader, java.lang.Class, com.tinkerpop.blueprints.pgm.Vertex, com.dooapp.gaedo.finders.repository.ServiceRepository, java.util.Map)
	 */
	@Override
	public Object loadObject(ClassLoader classLoader, Class effectiveClass, Vertex key, ServiceRepository repository, Map<String, Object> objectsBeingAccessed) {
		return readSerializable(key.getProperty(Properties.value.name()).toString());
	}
}