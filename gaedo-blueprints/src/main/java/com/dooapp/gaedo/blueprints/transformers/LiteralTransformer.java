package com.dooapp.gaedo.blueprints.transformers;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;

/**
 * Allow transformation of a data type into a literal vertex
 * @author ndx
 *
 *@param Type type of transformed data (to avoid cast)
 */
public interface LiteralTransformer<Type> extends Transformer {
	/**
	 * Get expected vertex id for the given value
	 * @param value input value
	 * @return the id used for the vertex representing this value
	 */
	public String getVertexId(Graph database, Type value);
	/**
	 * Get vertex for given object. This vertex may be simple or the root of a sub-graph
	 * @param database
	 * @param value
	 * @return
	 */
	Vertex getVertexFor(IndexableGraph database, Type value);
	
	/**
	 * Load given vertex into an object
	 * @param key
	 * @return
	 */
	public Type loadObject(Vertex key);
	/**
	 * Load given key into an object, with added bonus of known type
	 * @param effectiveClass
	 * @param key
	 * @return
	 */
	public Type loadObject(Class effectiveClass, Vertex key);

}