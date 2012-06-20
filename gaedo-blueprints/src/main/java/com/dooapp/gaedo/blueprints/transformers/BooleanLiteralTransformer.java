package com.dooapp.gaedo.blueprints.transformers;

public class BooleanLiteralTransformer extends AbstractLiteralTransformer<Boolean> implements LiteralTransformer<Boolean> {

	@Override
	protected Object getVertexValue(Boolean value) {
		return value==null ? Boolean.FALSE.toString() : value.toString();
	}

	@Override
	protected Class getValueClass(Boolean value) {
		return Boolean.class;
	}

}