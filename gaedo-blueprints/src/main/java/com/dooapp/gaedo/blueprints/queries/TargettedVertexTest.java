package com.dooapp.gaedo.blueprints.queries;

import java.util.TreeMap;

import com.dooapp.gaedo.blueprints.BluePrintsBackedFinderService;
import com.dooapp.gaedo.blueprints.Properties;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.pgm.Vertex;

/**
 * Base class for all vertex tests
 * @author ndx
 *
 */
public class TargettedVertexTest {
	
	/**
	 * Check if vertex is the one associated to the given object
	 * @param currentVertex examined vertex
	 * @param service service used to get stored object value
	 * @param expected expected object value
	 * @param deepInspect should we perform deep object inspection to check equaality ? This requires object loading and is as a consequence by far slower
	 * @return true if object is equals to vertex associated object
	 */
	protected static boolean isVertexEqualsTo(Vertex currentVertex, BluePrintsBackedFinderService service, Object expected, boolean deepInspect) {
		Object expectedId = service.getIdOf(expected);
		if (expectedId.equals(currentVertex.getProperty(Properties.vertexId.name()))) {
			return true;
		} else if(deepInspect) {
			Object value = service.loadObject(currentVertex, new TreeMap<String, Object>());
			return ((expected == null && value == null) || (expected != null && expected.equals(value)));
		} else {
			return false;
		}
	}

	/**
	 * Path used to navigate from source node to inspected one
	 */
	protected final Iterable<Property> path;
	/**
	 * Current service repository
	 */
	protected final ServiceRepository repository;
	/**
	 * Used path length, useful for checking that a null value can be found
	 */
	protected final int pathLength;

	/**
	 * Builds the vertex test
	 * @param repository repository allowing re-creation of objects and id evaluation
	 * @param path
	 */
	public TargettedVertexTest(ServiceRepository repository, Iterable<Property> path) {
		this.repository = repository;
		this.path = path;
		this.pathLength = computeLengthOf(path);
	}

	private int computeLengthOf(Iterable<Property> path) {
		int returned = 0;
		if(path!=null) {
			for(Property p : path) {
				returned++;
			}
		}
		return returned;
	}
}