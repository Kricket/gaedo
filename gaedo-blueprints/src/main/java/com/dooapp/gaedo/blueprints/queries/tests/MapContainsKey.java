package com.dooapp.gaedo.blueprints.queries.tests;


import java.util.Map;

import com.dooapp.gaedo.blueprints.transformers.LiteralTransformer;
import com.dooapp.gaedo.blueprints.transformers.MapEntryTransformer;
import com.dooapp.gaedo.blueprints.transformers.Tuples;
import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.pgm.Vertex;

public class MapContainsKey extends MonovaluedValuedVertexTest<Object> implements VertexTest {

	public MapContainsKey(ServiceRepository repository, Iterable<Property> p, Object value) {
		super(repository, ((MapEntryTransformer) Tuples.get(Map.Entry.class)).constructMapEntryKeyIterable(p), value);
	}

	@Override
	protected boolean callMatchManaged(Vertex currentVertex, Property finalProperty) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method "+MonovaluedValuedVertexTest.class.getName()+"#callMatchManaged has not yet been implemented AT ALL");
	}

	@Override
	public void accept(VertexTestVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	protected boolean matchesLiteral(Vertex currentVertex, LiteralTransformer literalTransformer) {
		return literalTransformer.isVertexEqualsTo(currentVertex, getExpected());
	}

}
