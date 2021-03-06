package com.dooapp.gaedo.blueprints.queries.tests;

import java.util.Iterator;

import com.dooapp.gaedo.blueprints.GraphDatabaseDriver;
import com.dooapp.gaedo.blueprints.strategies.GraphMappingStrategy;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class Anything extends TargettedVertexTest implements VertexTest {

	public Anything(GraphMappingStrategy<?> strategy, GraphDatabaseDriver driver, Iterable<Property> p) {
		super(strategy, driver, p);
	}

	/**
	 * Simply ensure there is an edge going
	 * @param examined
	 * @return
	 * @see com.dooapp.gaedo.blueprints.queries.tests.VertexTest#matches(com.tinkerpop.blueprints.pgm.Vertex)
	 */
	@Override
	public boolean matches(Vertex examined) {
		// Navigates to the first target edge and perform test when reached
		Vertex currentVertex = examined;
		for(Property currentProperty : path) {
			Iterator<Edge> edges = strategy.getOutEdgesFor(currentVertex, currentProperty).iterator();
			if(edges.hasNext()) {
				currentVertex = edges.next().getVertex(Direction.IN);
			} else {
				return false;
			}
		}
		return (currentVertex!=null);
	}

	@Override
	public void accept(VertexTestVisitor visitor) {
		visitor.visit(this);
	}

}
