package com.dooapp.gaedo.blueprints.queries;

import java.util.Iterator;

import com.dooapp.gaedo.blueprints.BluePrintsBackedFinderService;
import com.dooapp.gaedo.blueprints.GraphUtils;
import com.dooapp.gaedo.blueprints.Properties;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Vertex;

/**
 * Base class for all simple tests (like contains, greater than, ...).
 * Notice {@link #expected} is used for both direct value comparison (for literals) and id check (for managed objects).
 * This is done by asking the {@link TargettedVertexTest#repository} if {@link #expected} is a managed value
 * 
 * @author ndx
 *
 * @param <ValueType> current value type
 */
public abstract class MonovaluedValuedVertexTest<ValueType extends Object> extends TargettedVertexTest implements VertexTest {

	/**
	 * Expected value
	 */
	protected final ValueType expected;

	public MonovaluedValuedVertexTest(ServiceRepository repository, Iterable<Property> p, ValueType value) {
		super(repository, p);
		if(value==null) {
			throw new NullExpectedValueException("impossible to build a "+getClass().getSimpleName()+" search condition on path "+p+" using null search value.");
		}
		this.expected = value;
	}

	/**
	 * To match node
	 * @param examined
	 * @return
	 * @see com.dooapp.gaedo.blueprints.queries.VertexTest#matches(com.tinkerpop.blueprints.pgm.Vertex)
	 */
	@Override
	public boolean matches(Vertex examined) {
		// Navigates to the first target edge and perform etest when reached
		Vertex currentVertex = examined;
		Property finalProperty = null;
		// Counting path length allows us to check if we expect a null value
		int currentPathLength = 0;
		for(Property currentProperty : path) {
			Iterator<Edge> edges = currentVertex.getOutEdges(GraphUtils.getEdgeNameFor(currentProperty)).iterator();
			if(edges.hasNext()) {
				currentVertex = edges.next().getInVertex();
			} else {
				return false;
			}
			finalProperty = currentProperty;
			currentPathLength++;
		}
		if(finalProperty==null) {
			return false;
		} else {
			return matchesVertex(currentVertex, finalProperty);
		}
	}

	/**
	 * Perform the final vertex match
	 * @param currentVertex
	 * @param finalProperty
	 * @return
	 */
	public boolean matchesVertex(Vertex currentVertex, Property finalProperty) {
		if(repository.containsKey(expected.getClass())) {
			return callMatchManaged(currentVertex, finalProperty);
		} else {
			return callMatchLiteral(currentVertex, finalProperty);
		}
	}

	/**
	 * Check vertex corresponding to given final property matches with a managed object (that's to say an object 
	 * for which exist a {@link BluePrintsBackedFinderService}
	 * @param currentVertex vertex corresponding to finalProperty in property path
	 * @param finalProperty
	 * @return true if managed value matches ... yup, really awesome
	 */
	protected abstract boolean callMatchManaged(Vertex currentVertex, Property finalProperty);

	/**
	 * Call the {@link #matchesLiteral(Object)} method after getting property value
	 * @param currentVertex
	 * @param finalProperty
	 * @return
	 */
	protected boolean callMatchLiteral(Vertex currentVertex, Property finalProperty) {
		Object value = currentVertex.getProperty(Properties.value.name());
		return matchesLiteral((ValueType) finalProperty.fromString(value.toString()));
	}


	/**
	 * Check if literal values match
	 * @param effective
	 * @return
	 */
	protected abstract boolean matchesLiteral(ValueType effective);

	/**
	 * Obtain service associated to expected value
	 * @return 
	 */
	protected BluePrintsBackedFinderService getService() {
		return (BluePrintsBackedFinderService) repository.get(expected.getClass());
	}

}