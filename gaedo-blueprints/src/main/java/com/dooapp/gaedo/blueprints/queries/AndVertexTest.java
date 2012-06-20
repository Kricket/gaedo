package com.dooapp.gaedo.blueprints.queries;

import com.dooapp.gaedo.finders.repository.ServiceRepository;
import com.dooapp.gaedo.properties.Property;
import com.tinkerpop.blueprints.pgm.Vertex;

public class AndVertexTest extends AggregatedTargettedVertexTest implements CompoundVertexTest {
	public AndVertexTest(ServiceRepository repository, Iterable<Property> p) {
		super(repository, p);
	}

	@Override
	public boolean matches(Vertex examined) {
		boolean returned = true;
		for(VertexTest v : tests) {
			if(returned) {
				returned &= v.matches(examined);
			}
		}
		return returned;
	}

}